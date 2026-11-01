package com.miapp.auth.infrastructure.adapter.out.security;

import com.miapp.auth.application.port.out.RefreshTokenHashingPort;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Objects;

/**
 * Hashea tokens de alta entropía con SHA-256 y codificación Base64 URL-safe.
 */
@Component
public class Sha256RefreshTokenHashing implements RefreshTokenHashingPort {

    @Override
    public String hash(String refreshToken) {
        Objects.requireNonNull(refreshToken, "refreshToken must not be null");
        return encode(digest(refreshToken));
    }

    @Override
    public boolean matches(String refreshToken, String refreshTokenHash) {
        Objects.requireNonNull(refreshToken, "refreshToken must not be null");
        Objects.requireNonNull(refreshTokenHash, "refreshTokenHash must not be null");
        return MessageDigest.isEqual(digest(refreshToken),
                decode(refreshTokenHash));
    }

    private static byte[] digest(String value) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the JRE", exception);
        }
    }

    private static String encode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private static byte[] decode(String value) {
        try {
            return Base64.getUrlDecoder().decode(value);
        } catch (IllegalArgumentException exception) {
            return new byte[0];
        }
    }
}
