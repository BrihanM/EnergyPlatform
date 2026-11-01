package com.miapp.gateway.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miapp.gateway.correlation.CorrelationId;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Manejador reactivo central de excepciones no controladas del Gateway.
 *
 * <p>Traduce errores de comunicación y timeout a códigos HTTP conocidos y
 * evita exponer detalles internos mediante mensajes seguros.</p>
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewayExceptionHandler implements WebExceptionHandler {

    private final ObjectMapper objectMapper;

    /**
     * Crea el manejador con el serializador de respuestas de error.
     *
     * @param objectMapper serializador JSON de la aplicación
     */
    public GatewayExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Genera una respuesta JSON uniforme si la respuesta aún no fue enviada.
     *
     * @param exchange intercambio HTTP que produjo el error
     * @param exception excepción original del procesamiento
     * @return operación reactiva que escribe la respuesta o propaga el error
     */
    @Override
    @NonNull
    public Mono<Void> handle(ServerWebExchange exchange, @NonNull Throwable exception) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(exception);
        }

        String correlationId = exchange.getRequest().getHeaders().getFirst(CorrelationId.HEADER_NAME);
        int status = statusFor(exception);
        GatewayErrorResponse response = new GatewayErrorResponse(
                Instant.now(), status, HttpStatus.valueOf(status).getReasonPhrase(),
                safeMessage(status), exchange.getRequest().getURI().getPath(), correlationId);
        exchange.getResponse().setStatusCode(HttpStatus.valueOf(status));
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            byte[] body = objectMapper.writeValueAsString(response).getBytes(StandardCharsets.UTF_8);
            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
                    .bufferFactory().wrap(body)));
        } catch (Exception serializationError) {
            return Mono.error(serializationError);
        }
    }

    private int statusFor(Throwable exception) {
        if (exception instanceof java.util.concurrent.TimeoutException) {
            return HttpStatus.GATEWAY_TIMEOUT.value();
        }
        if (exception instanceof java.io.IOException) {
            return HttpStatus.BAD_GATEWAY.value();
        }
        return HttpStatus.INTERNAL_SERVER_ERROR.value();
    }

    private String safeMessage(int status) {
        return status == 504 ? "Upstream service timeout" :
                status == 502 ? "Upstream service unavailable" : "Gateway processing error";
    }
}
