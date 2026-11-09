package com.miapp.auth;

import org.testcontainers.containers.PostgreSQLContainer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Configuración compartida para las pruebas de persistencia sobre PostgreSQL.
 */
abstract class PostgresIntegrationTest {

    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:18.6-alpine3.24");

    // Singleton container: Spring caches contexts across test classes, so the database must outlive each class.
    static {
        POSTGRES.start();
    }

    /**
     * Sustituye la configuración local por las credenciales efímeras del contenedor.
     *
     * @param registry registro dinámico de propiedades de Spring
     */
    @DynamicPropertySource
    static void registerDatabaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }
}
