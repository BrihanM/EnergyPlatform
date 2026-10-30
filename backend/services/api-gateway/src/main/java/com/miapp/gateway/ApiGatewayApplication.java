package com.miapp.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de la aplicación reactiva API Gateway.
 *
 * <p>Inicializa el contexto de Spring Boot que reúne el enrutamiento y los
 * componentes transversales del perímetro HTTP.</p>
 */
@SpringBootApplication
public class ApiGatewayApplication {

    /**
     * Inicia el API Gateway con los argumentos recibidos del proceso Java.
     *
     * @param args argumentos de línea de comandos de Spring Boot
     */
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
