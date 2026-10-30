package com.miapp.gateway.config;

import org.springframework.context.annotation.Configuration;

/**
 * Marcador de configuración para la política CORS externalizada.
 *
 * <p>La política efectiva se define mediante propiedades de Spring Cloud
 * Gateway y variables de entorno por ambiente; esta clase no contiene reglas
 * duplicadas en Java.</p>
 */
@Configuration
public class CorsConfiguration {
}
