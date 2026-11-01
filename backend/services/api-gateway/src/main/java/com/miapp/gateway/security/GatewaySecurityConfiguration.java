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
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Configura autenticación JWT y autorización mínima del perímetro WebFlux.
 */
@Configuration
@EnableWebFluxSecurity
@EnableConfigurationProperties(GatewayJwtProperties.class)
public class GatewaySecurityConfiguration {

    private static final String USER_ID = "X-Authenticated-User-Id";
    private static final String ACCOUNT_ID = "X-Authenticated-Account-Id";
    private static final String USERNAME = "X-Authenticated-Username";

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
    public WebFilter gatewayAuthenticationFilter(GatewayJwtValidator validator, ObjectMapper objectMapper) {
        return (exchange, chain) -> {
            ServerWebExchange sanitized = exchange.mutate()
                    .request(request -> request.headers(headers -> {
                        headers.remove(USER_ID);
                        headers.remove(ACCOUNT_ID);
                        headers.remove(USERNAME);
                    }))
                    .build();

            if (isPublic(sanitized)) {
                return chain.filter(sanitized);
            }

            String authorization = sanitized.getRequest().getHeaders().getFirst("Authorization");
            if (authorization == null || !authorization.startsWith("Bearer ")
                    || authorization.substring(7).isBlank()) {
                return unauthorized(sanitized, objectMapper);
            }

            try {
                GatewayJwtValidator.GatewayIdentity identity =
                        validator.validate(authorization.substring(7).trim());
                ServerWebExchange trusted = sanitized.mutate()
                        .request(request -> request.headers(headers -> {
                            headers.set(USER_ID, identity.userId());
                            headers.set(USERNAME, identity.username());
                        }))
                        .build();
                var authentication = new UsernamePasswordAuthenticationToken(
                        identity.userId(), null, List.of());
                return chain.filter(trusted)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
            } catch (GatewayJwtValidator.JwtValidationException exception) {
                return unauthorized(sanitized, objectMapper);
            }
        };
    }

    private boolean isPublic(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();
        return "OPTIONS".equals(method)
                || "GET".equals(method) && (path.equals("/actuator/health")
                || path.startsWith("/actuator/health/"))
                || "POST".equals(method) && (path.equals("/api/v1/auth/register")
                || path.equals("/api/v1/auth/login")
                || path.equals("/api/v1/auth/refresh")
                || path.equals("/api/v1/auth/logout"));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, ObjectMapper objectMapper) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().set("WWW-Authenticate", "Bearer");
        String correlationId = exchange.getRequest().getHeaders().getFirst(CorrelationId.HEADER_NAME);
        try {
            String body = objectMapper.writeValueAsString(new GatewayErrorResponse(
                    java.time.Instant.now(), 401, "Unauthorized", "Unauthorized",
                    exchange.getRequest().getURI().getPath(), correlationId));
            DataBuffer buffer = exchange.getResponse().bufferFactory()
                    .wrap(body.getBytes(StandardCharsets.UTF_8));
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (Exception exception) {
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
