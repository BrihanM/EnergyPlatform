package com.miapp.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Verifica la inicialización independiente del microservicio de autenticación.
 */
@SpringBootTest
@ActiveProfiles("testcontainers")
@Testcontainers(disabledWithoutDocker = true)
class AuthServiceApplicationTests extends PostgresIntegrationTest {

    /**
     * Prueba básica de carga del contexto de Spring Boot.
     */
    @Test
    void contextLoads() {
    }
}
