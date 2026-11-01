package com.miapp.auth.infrastructure.adapter.in.web;

import com.miapp.auth.infrastructure.config.RefreshTokenCookieProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Construye y elimina la cookie HttpOnly del refresh token en un único punto.
 */
@Component
public class RefreshTokenCookieManager {

    private final RefreshTokenCookieProperties properties;

    public RefreshTokenCookieManager(RefreshTokenCookieProperties properties) {
        this.properties = properties;
    }

    public ResponseCookie create(String refreshToken) {
        return baseCookie(refreshToken).maxAge(properties.duration()).build();
    }

    public ResponseCookie clear() {
        return baseCookie("").maxAge(0).build();
    }

    public String name() {
        return properties.name();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(properties.name(), value)
                .httpOnly(true)
                .secure(properties.secure())
                .sameSite(properties.sameSite())
                .path(properties.path());
        if (properties.domain() != null && !properties.domain().isBlank()) {
            builder.domain(properties.domain());
        }
        return builder;
    }
}
