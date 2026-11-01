package com.miapp.gateway.filter;

import com.miapp.gateway.correlation.CorrelationId;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Filtro global que establece el identificador de correlación de cada solicitud.
 *
 * <p>Se ejecuta con la máxima prioridad para validar, generar y propagar el
 * identificador antes de que actúen el routing y los demás filtros.</p>
 */
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    /**
     * Coloca el filtro al inicio de la cadena global del Gateway.
     *
     * @return máxima prioridad de Spring
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    /**
     * Valida el identificador recibido, o genera uno nuevo cuando no existe,
     * y lo añade tanto a la solicitud enriquecida como a la respuesta.
     *
     * @param exchange intercambio HTTP reactivo actual
     * @param chain cadena de filtros que continúa el procesamiento
     * @return operación reactiva que representa el procesamiento de la solicitud
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String suppliedId = exchange.getRequest().getHeaders().getFirst(CorrelationId.HEADER_NAME);
        if (suppliedId != null && !CorrelationId.isValid(suppliedId)) {
            return writeBadRequest(exchange, CorrelationId.generate());
        }

        String correlationId = suppliedId == null ? CorrelationId.generate() : suppliedId;
        ServerWebExchange enrichedExchange = exchange.mutate()
                .request(request -> request.headers(headers -> headers.set(CorrelationId.HEADER_NAME, correlationId)))
                .build();
        enrichedExchange.getResponse().getHeaders().set(CorrelationId.HEADER_NAME, correlationId);
        return chain.filter(enrichedExchange);
    }

    /**
     * Construye la respuesta de rechazo sin continuar hacia el servicio destino.
     *
     * @param exchange intercambio HTTP actual
     * @param correlationId identificador generado para poder rastrear el rechazo
     * @return operación reactiva que escribe la respuesta 400
     */
    private Mono<Void> writeBadRequest(ServerWebExchange exchange, String correlationId) {
        exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().set(CorrelationId.HEADER_NAME, correlationId);
        String bodyText = "{\"timestamp\":\"" + java.time.Instant.now()
                + "\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"Invalid X-Correlation-ID\""
                + ",\"path\":\"" + exchange.getRequest().getURI().getPath()
                + "\",\"correlationId\":\"" + correlationId + "\"}";
        byte[] body = bodyText.getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
                .bufferFactory().wrap(body)));
    }
}
