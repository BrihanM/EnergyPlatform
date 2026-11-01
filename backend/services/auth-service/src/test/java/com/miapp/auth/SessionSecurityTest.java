package com.miapp.auth;

import com.miapp.auth.infrastructure.adapter.in.web.RefreshTokenCookieManager;
import com.miapp.auth.infrastructure.adapter.out.security.SecureRefreshTokenGenerator;
import com.miapp.auth.infrastructure.adapter.out.security.Sha256RefreshTokenHashing;
import com.miapp.auth.infrastructure.config.RefreshTokenCookieProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionSecurityTest {

    @Test
    void generatesDifferentHighEntropyTokens() {
        SecureRefreshTokenGenerator generator = new SecureRefreshTokenGenerator();

        String first = generator.generate();
        String second = generator.generate();

        assertThat(first).hasSize(43).isNotEqualTo(second);
        assertThat(first).matches("[A-Za-z0-9_-]+");
    }

    @Test
    void hashesAndMatchesWithoutPersistingRawToken() {
        Sha256RefreshTokenHashing hashing = new Sha256RefreshTokenHashing();
        String token = "opaque-refresh-token";
        String hash = hashing.hash(token);

        assertThat(hash).hasSize(43).doesNotContain(token);
        assertThat(hashing.matches(token, hash)).isTrue();
        assertThat(hashing.matches("other-token", hash)).isFalse();
    }

    @Test
    void configuresHttpOnlyCookieAndClearsIt() {
        RefreshTokenCookieProperties properties = new RefreshTokenCookieProperties(
                "refresh_token", true, "Strict", "/api/v1/auth", "", "604800000");
        RefreshTokenCookieManager manager = new RefreshTokenCookieManager(properties);

        ResponseCookie cookie = manager.create("secret");
        ResponseCookie cleared = manager.clear();

        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Strict");
        assertThat(cookie.getPath()).isEqualTo("/api/v1/auth");
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofDays(7));
        assertThat(cleared.getMaxAge()).isZero();
        assertThat(cleared.getValue()).isEmpty();
    }

    @Test
    void rejectsInsecureSameSiteNone() {
        assertThatThrownBy(() -> new RefreshTokenCookieProperties(
                "refresh_token", false, "None", "/api/v1/auth", "", "604800000"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
