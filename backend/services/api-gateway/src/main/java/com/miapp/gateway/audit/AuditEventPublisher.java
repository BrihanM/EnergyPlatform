package com.miapp.gateway.audit;

/**
 * Puerto de salida para publicar eventos de auditoría del Gateway.
 *
 * <p>La interfaz evita acoplar los filtros HTTP a logging, mensajería o una
 * futura persistencia.</p>
 */
public interface AuditEventPublisher {

    /**
     * Publica un evento de auditoría ya construido.
     *
     * @param event evento técnico que debe emitirse
     */
    void publish(AuditEvent event);
}
