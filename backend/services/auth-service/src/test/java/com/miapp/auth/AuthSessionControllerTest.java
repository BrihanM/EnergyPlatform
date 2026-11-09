package com.miapp.auth;

import com.miapp.auth.application.port.in.LogoutUseCase;
import com.miapp.auth.application.port.in.RefreshSessionUseCase;
import com.miapp.auth.application.result.AuthenticationResult;
import com.miapp.auth.domain.model.Role;
import com.miapp.auth.infrastructure.adapter.in.web.AuthSessionController;
import com.miapp.auth.infrastructure.adapter.in.web.RefreshTokenCookieManager;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthSessionControllerTest {
    @Test
    void refreshReturnsCurrentRolesInResponse() {
        var refresh = mock(RefreshSessionUseCase.class);
        var cookies = mock(RefreshTokenCookieManager.class);
        var roles = Set.of(Role.NORMAL_USER, Role.SECURITY_AUDITOR);
        var user = new AuthenticationResult.UserSummary(UUID.randomUUID(), "Test", "User",
                "testuser", "test@example.test", Instant.now());
        when(cookies.name()).thenReturn("refresh_token");
        when(cookies.create("replacement")).thenReturn(ResponseCookie.from("refresh_token", "replacement").build());
        when(refresh.refresh("original")).thenReturn(new AuthenticationResult(
                "access", 300, "replacement", user, roles));
        var request = new MockHttpServletRequest();
        request.setCookies(new Cookie("refresh_token", "original"));

        var response = new AuthSessionController(refresh, mock(LogoutUseCase.class), cookies).refresh(request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().roles()).isEqualTo(roles);
    }
}
