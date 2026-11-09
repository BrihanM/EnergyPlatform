package com.miapp.gateway;

import com.miapp.gateway.security.GatewayJwtProperties;
import com.miapp.gateway.security.GatewayJwtValidator;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifica las garantías criptográficas y de claims exigidas por el Gateway.
 */
class GatewayJwtValidatorTest {

    private static final String SECRET = "test-gateway-secret-that-is-at-least-32-bytes";
    private static final String ISSUER = "mi-app-auth";
    private GatewayJwtValidator validator;
    private SecretKey key;

    @BeforeEach
    void setUp() {
        validator = new GatewayJwtValidator(new GatewayJwtProperties(ISSUER, SECRET));
        key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void acceptsValidTokenAndReturnsOnlyTrustedIdentity() {
        UUID userId = UUID.randomUUID();
        String token = token(userId, ISSUER, Instant.now().plusSeconds(900),
                "brayan.marin", key);

        GatewayJwtValidator.GatewayIdentity identity = validator.validate(token);

        assertThat(identity.userId()).isEqualTo(userId.toString());
        assertThat(identity.username()).isEqualTo("brayan.marin");
        assertThat(identity.roles()).containsExactly("NORMAL_USER");
    }

    @Test
    void rejectsExpiredToken() {
        String token = token(UUID.randomUUID(), ISSUER, Instant.now().minusSeconds(1),
                "brayan.marin", key);

        assertThatThrownBy(() -> validator.validate(token))
                .isInstanceOf(GatewayJwtValidator.JwtValidationException.class);
    }

    @Test
    void rejectsWrongIssuer() {
        String token = token(UUID.randomUUID(), "untrusted-issuer",
                Instant.now().plusSeconds(900), "brayan.marin", key);

        assertThatThrownBy(() -> validator.validate(token))
                .isInstanceOf(GatewayJwtValidator.JwtValidationException.class);
    }

    @Test
    void rejectsInvalidSignature() {
        SecretKey otherKey = Keys.hmacShaKeyFor(
                "another-gateway-secret-that-is-at-least-32-bytes".getBytes(StandardCharsets.UTF_8));
        String token = token(UUID.randomUUID(), ISSUER, Instant.now().plusSeconds(900),
                "brayan.marin", otherKey);

        assertThatThrownBy(() -> validator.validate(token))
                .isInstanceOf(GatewayJwtValidator.JwtValidationException.class);
    }

    @Test
    void rejectsMissingUsernameClaim() {
        String token = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .issuer(ISSUER)
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(900)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        assertThatThrownBy(() -> validator.validate(token))
                .isInstanceOf(GatewayJwtValidator.JwtValidationException.class);
    }

    @Test
    void rejectsMissingRolesClaim() {
        String token = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .issuer(ISSUER)
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(900)))
                .claim("username", "brayan.marin")
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        assertThatThrownBy(() -> validator.validate(token))
                .isInstanceOf(GatewayJwtValidator.JwtValidationException.class);
    }

    @Test
    void rejectsLegacyTokenWithoutSessionId() {
        String token = Jwts.builder().subject(UUID.randomUUID().toString()).issuer(ISSUER)
                .issuedAt(new Date()).expiration(Date.from(Instant.now().plusSeconds(60)))
                .claim("username", "user").claim("roles", java.util.List.of("NORMAL_USER"))
                .signWith(key, Jwts.SIG.HS256).compact();
        assertThatThrownBy(() -> validator.validate(token))
                .isInstanceOf(GatewayJwtValidator.JwtValidationException.class);
    }

    private String token(UUID userId, String issuer, Instant expiration, String username,
                         SecretKey signingKey) {
        return Jwts.builder()
                .subject(userId.toString())
                .issuer(issuer)
                .issuedAt(new Date())
                .expiration(Date.from(expiration))
                .claim("sid", UUID.randomUUID().toString())
                .claim("username", username)
                .claim("roles", java.util.List.of("NORMAL_USER"))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }
}
