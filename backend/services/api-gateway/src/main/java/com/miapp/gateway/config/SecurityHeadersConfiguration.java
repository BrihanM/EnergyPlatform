package com.miapp.gateway.config;

import com.miapp.gateway.filter.SecurityHeadersFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registra el filtro global de headers de seguridad HTTP.
 */
@Configuration
public class SecurityHeadersConfiguration {

    /**
     * Crea el filtro que añade las protecciones perimetrales configuradas.
     *
     * @return filtro global de headers de seguridad
     */
    @Bean
    SecurityHeadersFilter securityHeadersFilter() {
        return new SecurityHeadersFilter();
    }
}
