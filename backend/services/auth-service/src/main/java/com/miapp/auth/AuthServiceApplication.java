package com.miapp.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.miapp.auth.application.service.SessionProperties;
import com.miapp.auth.application.service.JwtProperties;
import com.miapp.auth.infrastructure.config.RefreshTokenCookieProperties;

/**
 * Punto de entrada del microservicio de autenticación.
 *
 * <p>Inicializa el servicio de autenticación y registra las propiedades
 * tipadas de sesiones, JWT y cookies de refresh.</p>
 */
@SpringBootApplication
@EnableConfigurationProperties({SessionProperties.class, RefreshTokenCookieProperties.class,
        JwtProperties.class})
public class AuthServiceApplication {

    /**
     * Inicia el contexto independiente del servicio de autenticación.
     *
     * @param args argumentos de línea de comandos de Spring Boot
     */
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
