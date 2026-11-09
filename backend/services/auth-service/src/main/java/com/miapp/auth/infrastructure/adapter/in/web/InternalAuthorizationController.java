package com.miapp.auth.infrastructure.adapter.in.web;

import com.miapp.auth.application.service.JwtProperties;
import com.miapp.auth.application.service.SessionAuthorizationService;
import com.miapp.auth.domain.exception.InvalidSessionException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

/** Internal authorization endpoint; both the caller credential and user JWT are verified. */
@RestController
public class InternalAuthorizationController {
    private final byte[] callerSecret;
    private final SecretKey jwtKey;
    private final String issuer;
    private final SessionAuthorizationService authorization;

    public InternalAuthorizationController(@Value("${security.introspection.secret}") String callerSecret,
                                           JwtProperties jwt, SessionAuthorizationService authorization) {
        if (callerSecret == null || callerSecret.length() < 32) {
            throw new IllegalArgumentException("Introspection secret must contain at least 32 characters");
        }
        this.callerSecret = callerSecret.getBytes(StandardCharsets.UTF_8);
        this.jwtKey = Keys.hmacShaKeyFor(jwt.secret().getBytes(StandardCharsets.UTF_8));
        this.issuer = jwt.issuer();
        this.authorization = authorization;
    }

    @PostMapping("/internal/authorization/introspect")
    public ResponseEntity<SessionAuthorizationService.Authorization> introspect(
            @RequestHeader(value = "X-Internal-Auth-Secret", required = false) String secret,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String bearer) {
        if (secret == null || !MessageDigest.isEqual(callerSecret, secret.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (bearer == null || !bearer.startsWith("Bearer ") || bearer.substring(7).isBlank()) {
            throw new InvalidSessionException();
        }
        UUID userId;
        UUID sessionId;
        try {
            var claims = Jwts.parser().verifyWith(jwtKey).requireIssuer(issuer).build()
                    .parseSignedClaims(bearer.substring(7).trim()).getPayload();
            if (claims.getExpiration() == null) throw new InvalidSessionException();
            userId = UUID.fromString(claims.getSubject());
            sessionId = UUID.fromString(claims.get("sid", String.class));
        } catch (JwtException | IllegalArgumentException | NullPointerException exception) {
            throw new InvalidSessionException();
        }
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(authorization.validate(userId, sessionId));
    }
}
