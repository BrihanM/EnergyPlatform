package com.miapp.energy.infrastructure.adapter.out.security;

import com.miapp.energy.application.port.out.MeterCredentials;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class SecureMeterCredentials implements MeterCredentials {
    private final SecureRandom random = new SecureRandom();

    @Override
    public IssuedCredential issue() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new IssuedCredential(value, hash(value));
    }

    @Override
    public boolean matches(String supplied, byte[] expected) {
        boolean validFormat = supplied != null && supplied.matches("[A-Za-z0-9_-]{43}");
        byte[] actual = hash(validFormat ? supplied : "");
        return MessageDigest.isEqual(actual, expected) && validFormat;
    }

    private byte[] hash(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.US_ASCII));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
