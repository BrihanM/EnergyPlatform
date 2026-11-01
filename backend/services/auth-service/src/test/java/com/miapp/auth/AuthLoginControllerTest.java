package com.miapp.auth;

import com.miapp.auth.application.port.in.LoginUseCase;
import com.miapp.auth.application.result.LoginResult;
import com.miapp.auth.domain.model.AccountStatus;
import com.miapp.auth.infrastructure.adapter.in.web.AuthLoginController;
import com.miapp.auth.infrastructure.adapter.in.web.RefreshTokenCookieManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verifica el contrato REST del login y su respuesta segura.
 */
@WebMvcTest(AuthLoginController.class)
class AuthLoginControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoginUseCase loginUseCase;

    @MockitoBean
    private RefreshTokenCookieManager cookieManager;

    @Test
    void returnsAuthenticatedUserWithoutCredentialsOrTokens() throws Exception {
        UUID id = UUID.randomUUID();
        when(loginUseCase.login(any())).thenReturn(new LoginResult(true,
                new LoginResult.UserSummary(id, "Brayan", "Marín", "brayan.marin",
                        "brayan@example.com", Instant.parse("2026-10-31T00:00:00Z"))));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"identifier":"brayan.marin","password":"ContraseñaSegura1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.user.username").value("brayan.marin"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void returnsUniformUnauthorizedErrorWithCorrelationId() throws Exception {
        when(loginUseCase.login(any()))
                .thenThrow(new com.miapp.auth.domain.exception.InvalidCredentialsException());

        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Correlation-ID", "corr-login")
                        .contentType("application/json")
                        .content("""
                                {"identifier":"unknown","password":"wrong"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciales inválidas."))
                .andExpect(jsonPath("$.correlationId").value("corr-login"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void rejectsBlankCredentialsAsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"identifier":"","password":""}
                                """))
                .andExpect(status().isBadRequest());
    }
}
