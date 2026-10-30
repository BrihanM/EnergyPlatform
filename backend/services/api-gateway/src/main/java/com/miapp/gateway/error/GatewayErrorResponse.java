package com.miapp.gateway.error;

import java.time.Instant;

/**
 * Contrato JSON inmutable para errores producidos por el Gateway.
 *
 * @param timestamp instante en que se construye la respuesta
 * @param status código HTTP devuelto
 * @param error descripción estándar del estado HTTP
 * @param message mensaje seguro para el cliente
 * @param path ruta solicitada
 * @param correlationId identificador asociado a la solicitud
 */
public record GatewayErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        String correlationId
) {
}
