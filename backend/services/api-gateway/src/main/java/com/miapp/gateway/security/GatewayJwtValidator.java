package com.miapp.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.List;
import java.util.Set;
import com.miapp.gateway.security.GatewayJwtValidator.GatewayIdentity;

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
            Object rawRoles = claims.get("roles");
            if (!(rawRoles instanceof List<?> roleValues)) {
                throw new JwtValidationException();
            }
            Set<String> roles = roleValues.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
            if (roles.isEmpty() || roles.stream().anyMatch(value ->
                    !Set.of("NORMAL_USER", "ADMIN", "SECURITY_AUDITOR").contains(value))) {
                throw new JwtValidationException();
            }
            return new GatewayIdentity(userId.toString(), username, roles);
        } catch (Exception exception) {
            throw new JwtValidationException();
        }
    }

    public record GatewayIdentity(String userId, String username, Set<String> roles) {
    }

    public static final class JwtValidationException extends RuntimeException {
        public JwtValidationException() {
            super("Invalid access token");
        }
    }
}
