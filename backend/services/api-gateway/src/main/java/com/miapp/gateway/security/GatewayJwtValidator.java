package com.miapp.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Valida firma, issuer, expiración y claims mínimos del JWT emitido por auth-service.
 */
@Component
public class GatewayJwtValidator {

    private final GatewayJwtProperties properties;
    private final SecretKey key;

    public GatewayJwtValidator(GatewayJwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Valida un token y devuelve sólo la identidad mínima confiable.
     *
     * @param token JWT sin el esquema Bearer
     * @return identidad extraída de claims validados
     * @throws JwtValidationException cuando el token no es confiable
     */
    public GatewayIdentity validate(String token) {
        try {
            Jws<Claims> parsed = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(properties.issuer())
                    .build()
                    .parseSignedClaims(token);
            Claims claims = parsed.getPayload();
            UUID userId = UUID.fromString(claims.getSubject());
            String username = claims.get("username", String.class);
            if (username == null || username.isBlank()) {
                throw new JwtValidationException();
            }
            return new GatewayIdentity(userId.toString(), username);
        } catch (Exception exception) {
            throw new JwtValidationException();
        }
    }

    public record GatewayIdentity(String userId, String username) {
    }

    public static final class JwtValidationException extends RuntimeException {
        public JwtValidationException() {
            super("Invalid access token");
        }
    }
}
