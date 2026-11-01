package com.miapp.auth.application.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración externalizada de access tokens.
 */
@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(String issuer, String secret, String expiration) {

    public JwtProperties {
        if (issuer == null || issuer.isBlank() || secret == null || secret.length() < 32
                || expiration == null || expiration.isBlank()) {
            throw new IllegalArgumentException("Invalid JWT configuration");
        }
    }
}
