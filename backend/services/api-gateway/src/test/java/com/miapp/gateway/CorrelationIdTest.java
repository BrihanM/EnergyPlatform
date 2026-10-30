package com.miapp.gateway;

import com.miapp.gateway.correlation.CorrelationId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas unitarias de las reglas de validación del identificador de correlación.
 */
class CorrelationIdTest {

    /**
     * Verifica que se acepten caracteres permitidos y longitud válida.
     */
    @Test
    void acceptsSafeIdentifiers() {
        assertThat(CorrelationId.isValid("request-123_abc")).isTrue();
    }

    /**
     * Verifica el rechazo de valores cortos, con espacios o demasiado largos.
     */
    @Test
    void rejectsUntrustedIdentifiers() {
        assertThat(CorrelationId.isValid("short")).isFalse();
        assertThat(CorrelationId.isValid("invalid value")).isFalse();
        assertThat(CorrelationId.isValid("a".repeat(129))).isFalse();
    }
}
