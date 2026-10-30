package com.miapp.gateway.audit;

/**
 * Clasificaciones posibles para el resultado técnico de una solicitud.
 */
public enum AuditResult {
    /** Solicitud procesada correctamente. */
    SUCCESS,
    /** Error atribuible a la solicitud del cliente. */
    CLIENT_ERROR,
    /** Error HTTP producido por el servicio destino. */
    UPSTREAM_ERROR,
    /** Error de comunicación con el servicio destino. */
    COMMUNICATION_ERROR,
    /** Tiempo de espera agotado durante la comunicación. */
    TIMEOUT,
    /** Flujo cancelado antes de finalizar. */
    CANCELLED,
    /** Error interno del propio Gateway. */
    GATEWAY_ERROR
}
