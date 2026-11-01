package com.miapp.auth.infrastructure.adapter.out.security;

import com.miapp.auth.application.port.out.PasswordHashingPort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Adaptador de hashing basado en BCrypt, sin exponer Spring Security al dominio.
 */
@Component
public class PasswordHashingAdapter implements PasswordHashingPort {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public String hash(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
}
