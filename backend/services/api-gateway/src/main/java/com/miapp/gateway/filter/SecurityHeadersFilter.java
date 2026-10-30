package com.miapp.gateway.filter;

import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Filtro perimetral que añade headers HTTP básicos de protección al cliente.
 */
public class SecurityHeadersFilter implements GlobalFilter, Ordered {

    /**
     * Ejecuta este filtro al final de la cadena para cubrir respuestas generadas
     * por el Gateway y por los servicios enrutados.
     *
     * @return prioridad baja dentro de los filtros globales
     */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 100;
    }

    /**
     * Añade headers de seguridad sin modificar el cuerpo de la respuesta.
     *
     * @param exchange intercambio HTTP actual
     * @param chain cadena de filtros del Gateway
     * @return operación reactiva del procesamiento
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        exchange.getResponse().getHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponse().getHeaders().set("X-Frame-Options", "DENY");
        exchange.getResponse().getHeaders().set("Referrer-Policy", "no-referrer");
        exchange.getResponse().getHeaders().set("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
        return chain.filter(exchange);
    }
}
