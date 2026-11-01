package com.miapp.auth.infrastructure.adapter.in.web;

import java.time.Instant;

/**
 * Formato uniforme y seguro de errores HTTP.
 */
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        String correlationId) {
}
