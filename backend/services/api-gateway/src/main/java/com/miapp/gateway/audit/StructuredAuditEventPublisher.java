package com.miapp.gateway.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Adaptador de auditoría que serializa eventos como registros JSON estructurados.
 *
 * <p>El logger utiliza una categoría separada para facilitar el procesamiento
 * independiente de los logs técnicos.</p>
 */
@Component
public class StructuredAuditEventPublisher implements AuditEventPublisher {

    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("com.miapp.gateway.audit");
    private final ObjectMapper objectMapper;

    /**
     * Crea el publicador con el serializador configurado por la aplicación.
     *
     * @param objectMapper serializador JSON con soporte para fechas Java
     */
    public StructuredAuditEventPublisher(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Emite el evento como una línea identificable por {@code AUDIT_EVENT}.
     *
     * @param event evento que se serializará
     */
    @Override
    public void publish(AuditEvent event) {
        try {
            AUDIT_LOG.info("AUDIT_EVENT {}", objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException exception) {
            AUDIT_LOG.error("Unable to serialize audit event {}", event.eventId(), exception);
        }
    }
}
