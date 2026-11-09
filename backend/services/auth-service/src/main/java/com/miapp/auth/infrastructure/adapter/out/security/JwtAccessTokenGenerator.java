package com.miapp.auth.infrastructure.adapter.out.security;

import com.miapp.auth.application.port.out.AccessTokenGeneratorPort;
import com.miapp.auth.application.service.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.Set;
import com.miapp.auth.domain.model.Role;

/**
 * Generador JWT HMAC-SHA256. El secreto se obtiene exclusivamente de
 * configuración externalizada y nunca se persiste.
 */
@Component
public class JwtAccessTokenGenerator implements AccessTokenGeneratorPort {

    private final JwtProperties properties;
    private final SecretKey key;
    private final Duration expiration;

    public JwtAccessTokenGenerator(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.expiration = Duration.ofMillis(Long.parseLong(properties.expiration()));
    }

    @Override
    public AccessToken generate(UUID subject, UUID sessionId, String username, Set<Role> roles, Instant issuedAt) {
        Instant expiresAt = issuedAt.plus(expiration);
        String value = Jwts.builder()
                .subject(subject.toString())
                .issuer(properties.issuer())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .claim("sid", sessionId.toString())
                .claim("username", username)
                .claim("roles", roles.stream().map(Role::name).sorted().toList())
                .signWith(key, Jwts.SIG.HS256)
                .compact();
        return new AccessToken(value, expiration.toSeconds());
    }
}
