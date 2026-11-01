package com.miapp.gateway.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración externalizada utilizada para validar los access tokens.
 */
@ConfigurationProperties(prefix = "security.jwt")
public record GatewayJwtProperties(String issuer, String secret) {

    public GatewayJwtProperties {
        if (issuer == null || issuer.isBlank() || secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("Invalid gateway JWT configuration");
        }
    }
}
