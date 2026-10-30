package com.miapp.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del microservicio de autenticación.
 *
 * <p>Actualmente sólo inicializa la aplicación Spring Boot. La lógica de
 * usuarios, autenticación y persistencia se incorporará posteriormente
 * respetando la arquitectura hexagonal.</p>
 */
@SpringBootApplication
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
