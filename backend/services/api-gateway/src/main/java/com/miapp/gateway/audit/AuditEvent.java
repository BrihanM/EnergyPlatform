package com.miapp.gateway.audit;

import java.time.Instant;

/**
 * Evento inmutable con los metadatos técnicos de una solicitud auditada.
 *
 * <p>No contiene cuerpos HTTP ni credenciales; su forma permite sustituir el
 * publicador de logging por una persistencia desacoplada en el futuro.</p>
 *
 * @param eventId identificador único del evento
 * @param receivedAt instante de recepción
 * @param completedAt instante de finalización
 * @param correlationId identificador de correlación
 * @param method método HTTP
 * @param path ruta solicitada sin query parameters
 * @param destination host del destino enrutado, cuando está disponible
 * @param sourceIp dirección de origen determinada por el servidor
 * @param userAgent agente de usuario recibido
 * @param status código HTTP observado
 * @param durationMs duración total en milisegundos
 * @param result clasificación técnica del resultado
 * @param error tipo de error, cuando existe
 */
public record AuditEvent(
        String eventId,
        Instant receivedAt,
        Instant completedAt,
        String correlationId,
        String method,
        String path,
        String destination,
        String sourceIp,
        String userAgent,
        Integer status,
        long durationMs,
        AuditResult result,
        String error
) {
}
