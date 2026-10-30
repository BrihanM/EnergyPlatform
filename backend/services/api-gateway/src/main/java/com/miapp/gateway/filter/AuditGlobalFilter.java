package com.miapp.gateway.filter;

import com.miapp.gateway.audit.AuditEvent;
import com.miapp.gateway.audit.AuditEventPublisher;
import com.miapp.gateway.audit.AuditResult;
import com.miapp.gateway.correlation.CorrelationId;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Filtro global responsable de emitir la auditoría técnica de una solicitud.
 *
 * <p>Captura únicamente metadatos HTTP y publica el evento al finalizar la
 * cadena reactiva. No consume cuerpos ni registra headers sensibles.</p>
 */
@Component
public class AuditGlobalFilter implements GlobalFilter, Ordered {

    private final AuditEventPublisher publisher;

    /**
     * Crea el filtro con el puerto de publicación desacoplado de la salida
     * concreta, actualmente logging estructurado.
     *
     * @param publisher publicador de eventos de auditoría
     */
    public AuditGlobalFilter(AuditEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    /**
     * Ejecuta la auditoría alrededor de la cadena de filtros.
     *
     * <p>{@code doFinally} permite capturar finalización normal y cancelación,
     * mientras que {@code doOnError} conserva la excepción para clasificarla.</p>
     *
     * @param exchange intercambio HTTP actual
     * @param chain cadena de filtros del Gateway
     * @return operación reactiva del procesamiento
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Instant receivedAt = Instant.now();
        String correlationId = exchange.getRequest().getHeaders().getFirst(CorrelationId.HEADER_NAME);
        String sourceIp = sourceIp(exchange);
        String userAgent = exchange.getRequest().getHeaders().getFirst("User-Agent");
        return chain.filter(exchange)
                .doOnError(error -> publish(exchange, receivedAt, correlationId, sourceIp, userAgent, error))
                .doFinally(signal -> {
                    // La auditoría se emite una sola vez para finalización normal o cancelación.
                    if (signal == reactor.core.publisher.SignalType.ON_COMPLETE
                            || signal == reactor.core.publisher.SignalType.CANCEL) {
                        publish(exchange, receivedAt, correlationId, sourceIp, userAgent, null,
                                signal == reactor.core.publisher.SignalType.CANCEL);
                    }
                });
    }

    private void publish(ServerWebExchange exchange, Instant receivedAt, String correlationId,
                         String sourceIp, String userAgent, Throwable error) {
        publish(exchange, receivedAt, correlationId, sourceIp, userAgent, error, false);
    }

    private void publish(ServerWebExchange exchange, Instant receivedAt, String correlationId,
                         String sourceIp, String userAgent, Throwable error, boolean cancelled) {
        HttpStatusCode statusCode = exchange.getResponse().getStatusCode();
        int status = statusCode == null ? 500 : statusCode.value();
        AuditResult result = cancelled ? AuditResult.CANCELLED : classify(status, error);
        publisher.publish(new AuditEvent(
                UUID.randomUUID().toString(), receivedAt, Instant.now(), correlationId,
                exchange.getRequest().getMethod().name(), exchange.getRequest().getURI().getPath(),
                destination(exchange), sourceIp, userAgent, status,
                Duration.between(receivedAt, Instant.now()).toMillis(), result,
                error == null ? null : error.getClass().getSimpleName()));
    }

    /**
     * Publica la clasificación técnica a partir del estado HTTP y la excepción.
     *
     * @param status código HTTP observado
     * @param error excepción que terminó el flujo, si existe
     * @return categoría utilizada por el evento de auditoría
     */
    private AuditResult classify(int status, Throwable error) {
        if (error instanceof java.util.concurrent.TimeoutException) {
            return AuditResult.TIMEOUT;
        }
        if (error != null) {
            return AuditResult.COMMUNICATION_ERROR;
        }
        if (status >= 500) {
            return AuditResult.UPSTREAM_ERROR;
        }
        if (status >= 400) {
            return AuditResult.CLIENT_ERROR;
        }
        return AuditResult.SUCCESS;
    }

    private String sourceIp(ServerWebExchange exchange) {
        InetSocketAddress address = exchange.getRequest().getRemoteAddress();
        return address == null || address.getAddress() == null ? null : address.getAddress().getHostAddress();
    }

    private String destination(ServerWebExchange exchange) {
        java.net.URI routedUri = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
        return routedUri == null ? null : routedUri.getHost();
    }
}
