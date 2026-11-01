package com.miapp.auth;

import com.miapp.auth.application.port.in.RegisterUserUseCase;
import com.miapp.auth.application.result.RegisterUserResult;
import com.miapp.auth.domain.model.AccountStatus;
import com.miapp.auth.infrastructure.adapter.in.web.AuthRegistrationController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verifica el contrato HTTP del adaptador de registro.
 */
@WebMvcTest(AuthRegistrationController.class)
class AuthRegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RegisterUserUseCase registerUserUseCase;

    @Test
    void returnsCreatedResourceWithoutCredentials() throws Exception {
        UUID id = UUID.randomUUID();
        when(registerUserUseCase.register(any())).thenReturn(new RegisterUserResult(
                id, "Brayan", "Marín", "brayan.marin", "brayan@example.com",
                AccountStatus.ACTIVE, Instant.parse("2026-10-31T00:00:00Z")));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/users/" + id))
                .andExpect(jsonPath("$.username").value("brayan.marin"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void rejectsInvalidRequestBeforeCallingUseCase() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .header("X-Correlation-ID", "corr-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"","lastName":"Marín","username":"x",
                                 "email":"invalid","password":"short"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.correlationId").value("corr-123"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    private String validRequest() throws Exception {
        return objectMapper.writeValueAsString(java.util.Map.of(
                "firstName", "Brayan",
                "lastName", "Marín",
                "username", "brayan.marin",
                "email", "brayan@example.com",
                "password", "ContraseñaSegura1"));
    }
}
