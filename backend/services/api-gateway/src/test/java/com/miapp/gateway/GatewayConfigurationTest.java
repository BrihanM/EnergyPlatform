package com.miapp.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de las propiedades esenciales de routing expuestas al contexto.
 */
@SpringBootTest
class GatewayConfigurationTest {

    @Autowired
    private Environment environment;

    /**
     * Comprueba que la ruta inicial del servicio de autenticación esté registrada.
     */
    @Test
    void exposesConfiguredAuthRoute() {
        assertThat(environment.getProperty("spring.cloud.gateway.server.webflux.routes[0].id"))
                .isEqualTo("auth-service");
    }
}
