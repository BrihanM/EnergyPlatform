package com.miapp.gateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miapp.gateway.correlation.CorrelationId;
import com.miapp.gateway.error.GatewayErrorResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Configura autenticación JWT y autorización mínima del perímetro WebFlux.
 */
@Configuration
@EnableWebFluxSecurity
@EnableConfigurationProperties(GatewayJwtProperties.class)
public class GatewaySecurityConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(GatewaySecurityConfiguration.class);
    private static final String USER_ID = "X-Authenticated-User-Id";
    private static final String ACCOUNT_ID = "X-Authenticated-Account-Id";
    private static final String USERNAME = "X-Authenticated-Username";
    private static final String ROLES = "X-Authenticated-Roles";
    private static final Pattern PUBLIC_OFFER_DETAIL = Pattern.compile(
            "^/api/v1/(?:market/)?offers/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private static final Pattern DEVICE_READING = Pattern.compile(
            "^/api/v1/market/devices/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}/readings$");

    /**
     * Exige autenticación en toda ruta no declarada pública.
     *
     * @param http configuración reactiva de Spring Security
     * @return cadena de filtros de seguridad
     */
    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http.csrf(csrf -> csrf.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .exceptionHandling(errors -> errors.accessDeniedHandler(
                        (exchange, denied) -> forbidden(exchange)))
                .authorizeExchange(exchange -> exchange
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers(HttpMethod.POST,
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/logout").permitAll()
                        .pathMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                        .matchers(request -> isPublicOfferRead(request) || isDeviceReading(request)
                                ? ServerWebExchangeMatcher.MatchResult.match()
                                : ServerWebExchangeMatcher.MatchResult.notMatch()).permitAll()
                        .anyExchange().authenticated())
                .build();
    }

    /**
     * Valida Bearer tokens y agrega headers internos sólo después de validarlos.
     *
     * @param validator validador configurado con el issuer y secreto del sistema
     * @param objectMapper serializador del error uniforme
     * @return filtro WebFlux de autenticación
     */
    @Bean
    @Order(-200)
    public WebFilter gatewayAuthenticationFilter(GatewayJwtValidator validator, ObjectMapper objectMapper,
                                                 OnlineAuthorizationClient authorizationClient) {
        return (exchange, chain) -> {
            ServerWebExchange sanitized = exchange.mutate()
                    .request(request -> request.headers(headers -> {
                        headers.remove(USER_ID);
                        headers.remove(ACCOUNT_ID);
                        headers.remove(USERNAME);
                        headers.remove(ROLES);
                        headers.remove("X-Internal-Auth-Secret");
                    }))
                    .build();

            if (sanitized.getRequest().getPath().value().startsWith("/internal/authorization/")) {
                sanitized.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
                return sanitized.getResponse().setComplete();
            }
            if (isPublic(sanitized)) {
                return chain.filter(sanitized);
            }

            String authorization = sanitized.getRequest().getHeaders().getFirst("Authorization");
            if (authorization == null || !authorization.startsWith("Bearer ")
                    || authorization.substring(7).isBlank()) {
                return unauthorized(sanitized, objectMapper);
            }

            try {
                String token = authorization.substring(7).trim();
                GatewayJwtValidator.GatewayIdentity jwtIdentity = validator.validate(token);
                return authorizationClient.validate(token, jwtIdentity.userId())
                        .flatMap(identity -> {
                            ServerWebExchange trusted = sanitized.mutate()
                                    .request(request -> request.headers(headers -> {
                                        headers.set(USER_ID, identity.userId());
                                        headers.set(USERNAME, identity.username());
                                        headers.set(ROLES, String.join(",", identity.roles().stream().sorted().toList()));
                                    })).build();
                            var authentication = new UsernamePasswordAuthenticationToken(identity.userId(), null, List.of());
                            return chain.filter(trusted)
                                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
                        })
                        .onErrorResume(OnlineAuthorizationClient.InvalidAuthorizationException.class,
                                exception -> unauthorized(sanitized, objectMapper))
                        .onErrorResume(OnlineAuthorizationClient.AuthorizationUnavailableException.class, exception -> {
                            Throwable cause = exception.getCause();
                            LOG.warn("Online authorization unavailable: {}",
                                    cause == null ? "invalid response or service error" : cause.getClass().getSimpleName());
                            return error(sanitized, objectMapper, HttpStatus.SERVICE_UNAVAILABLE);
                        });
            } catch (GatewayJwtValidator.JwtValidationException exception) {
                return unauthorized(sanitized, objectMapper);
            }
        };
    }

    private boolean isPublic(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();
        return "OPTIONS".equals(method)
                || isPublicOfferRead(exchange)
                || isDeviceReading(exchange)
                || "GET".equals(method) && (path.equals("/actuator/health")
                || path.startsWith("/actuator/health/"))
                || "POST".equals(method) && (path.equals("/api/v1/auth/register")
                || path.equals("/api/v1/auth/login")
                || path.equals("/api/v1/auth/refresh")
                || path.equals("/api/v1/auth/logout"));
    }

    private boolean isPublicOfferRead(ServerWebExchange exchange) {
        if (exchange.getRequest().getMethod() != HttpMethod.GET) return false;
        String path = exchange.getRequest().getPath().value();
        return path.equals("/api/v1/offers") || path.equals("/api/v1/market/offers")
                || PUBLIC_OFFER_DETAIL.matcher(path).matches();
    }

    private boolean isDeviceReading(ServerWebExchange exchange) {
        return exchange.getRequest().getMethod() == HttpMethod.POST
                && DEVICE_READING.matcher(exchange.getRequest().getPath().value()).matches();
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, ObjectMapper objectMapper) {
        exchange.getResponse().getHeaders().set("WWW-Authenticate", "Bearer");
        return error(exchange, objectMapper, HttpStatus.UNAUTHORIZED);
    }

    private Mono<Void> error(ServerWebExchange exchange, ObjectMapper objectMapper, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String correlationId = exchange.getRequest().getHeaders().getFirst(CorrelationId.HEADER_NAME);
        try {
            String body = objectMapper.writeValueAsString(new GatewayErrorResponse(
                    java.time.Instant.now(), status.value(), status.getReasonPhrase(), status.getReasonPhrase(),
                    exchange.getRequest().getURI().getPath(), correlationId));
            DataBuffer buffer = exchange.getResponse().bufferFactory()
                    .wrap(body.getBytes(StandardCharsets.UTF_8));
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            return Mono.error(exception);
        }
    }

    private Mono<Void> forbidden(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String correlationId = exchange.getRequest().getHeaders().getFirst(CorrelationId.HEADER_NAME);
        String body = "{\"timestamp\":\"" + java.time.Instant.now()
                + "\",\"status\":403,\"error\":\"Forbidden\",\"message\":\"Forbidden\","
                + "\"path\":\"" + exchange.getRequest().getURI().getPath()
                + "\",\"correlationId\":\"" + (correlationId == null ? "" : correlationId) + "\"}";
        DataBuffer buffer = exchange.getResponse().bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
