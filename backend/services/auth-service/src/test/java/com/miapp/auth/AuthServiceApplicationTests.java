package com.miapp.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifica la inicialización independiente del microservicio de autenticación.
 */
@SpringBootTest
@ActiveProfiles("test")
class AuthServiceApplicationTests {

    /**
     * Prueba básica de carga del contexto de Spring Boot.
     */
    @Test
    void contextLoads() {
    }
}
