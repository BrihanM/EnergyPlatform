package com.miapp.auth.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Provee dependencias técnicas compartidas y controlables en pruebas.
 */
@Configuration
public class InfrastructureConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
