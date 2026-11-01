package com.miapp.auth.infrastructure.adapter.out.security;

import com.miapp.auth.application.port.out.RefreshTokenGeneratorPort;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Genera tokens opacos con 256 bits de entropía.
 */
@Component
public class SecureRefreshTokenGenerator implements RefreshTokenGeneratorPort {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generate() {
        byte[] value = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }
}
