package com.miapp.auth.infrastructure.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Recurso técnico mínimo para validar el forwarding autenticado del Gateway.
 *
 * <p>El servicio sólo es accesible desde la red interna en Compose. La
 * identidad devuelta debe proceder de los headers que el Gateway reconstruye
 * después de validar el JWT.</p>
 */
@RestController
public class IntegrationProbeController {

    /**
     * Devuelve la identidad confiable recibida desde el Gateway.
     *
     * @param userId identidad de usuario propagada por el Gateway
     * @param username nombre de usuario propagado por el Gateway
     * @param correlationId identificador de correlación propagado por el Gateway
     * @return respuesta técnica para pruebas de integración
     */
    @GetMapping("/api/v1/integration/probe")
    public ResponseEntity<Map<String, String>> probe(
            @RequestHeader(value = "X-Authenticated-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Authenticated-Username", required = false) String username,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        return ResponseEntity.ok(Map.of(
                "status", "forwarded",
                "userId", valueOrEmpty(userId),
                "username", valueOrEmpty(username),
                "correlationId", valueOrEmpty(correlationId)));
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
