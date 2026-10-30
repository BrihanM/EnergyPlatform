package com.miapp.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.miapp.gateway.error.GatewayExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configura el serializador JSON y el manejador central de excepciones.
 */
@Configuration
public class ErrorHandlingConfiguration {

    /**
     * Crea el serializador utilizado por las respuestas de error y la auditoría.
     *
     * @return {@link ObjectMapper} con soporte para tipos de fecha Java
     */
    @Bean
    ObjectMapper objectMapper() {
        return JsonMapper.builder().addModule(new JavaTimeModule()).build();
    }

    /**
     * Registra el manejador reactivo de excepciones del Gateway.
     *
     * @param objectMapper serializador usado para generar la respuesta JSON
     * @return manejador central de excepciones
     */
    @Bean
    GatewayExceptionHandler gatewayExceptionHandler(ObjectMapper objectMapper) {
        return new GatewayExceptionHandler(objectMapper);
    }
}
