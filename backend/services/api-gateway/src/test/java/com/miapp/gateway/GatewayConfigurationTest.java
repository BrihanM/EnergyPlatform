package com.miapp.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de las propiedades esenciales de routing expuestas al contexto.
 */
@SpringBootTest
class GatewayConfigurationTest {

    @Autowired
    private Environment environment;

    @Autowired
    private RouteDefinitionLocator routeDefinitionLocator;

    /**
     * Comprueba que la ruta inicial del servicio de autenticación esté registrada.
     */
    @Test
    void exposesConfiguredAuthRoute() {
        assertThat(environment.getProperty("spring.cloud.gateway.server.webflux.routes[0].id"))
                .isEqualTo("auth-service");
    }

    @Test
    void exposesReservedEnergyMarketRoute() {
        var routes = routeDefinitionLocator.getRouteDefinitions().collectList().block();
        assertThat(routes).extracting(RouteDefinition::getId)
                .contains("energy-market-service");
        assertThat(routes).filteredOn(route -> route.getId().equals("energy-market-service"))
                .singleElement()
                .satisfies(route -> assertThat(route.getPredicates()).anyMatch(predicate ->
                        predicate.getName().equals("Path")
                                && predicate.getArgs().containsValue("/api/v1/market/**")));
    }
}
