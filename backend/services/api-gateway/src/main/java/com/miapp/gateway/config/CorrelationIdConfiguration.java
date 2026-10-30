package com.miapp.gateway.config;

import com.miapp.gateway.filter.CorrelationIdFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registra el filtro global que administra el identificador de correlación.
 */
@Configuration
public class CorrelationIdConfiguration {

    /**
     * Crea el filtro de correlación como bean del Gateway.
     *
     * @return filtro encargado de validar y propagar el identificador
     */
    @Bean
    CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }
}
