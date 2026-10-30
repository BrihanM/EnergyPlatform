package com.miapp.gateway.correlation;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Utilidad para validar y generar identificadores de correlación HTTP.
 *
 * <p>Centraliza el nombre del header y la regla de formato que evita aceptar
 * valores arbitrarios provenientes de clientes externos.</p>
 */
public final class CorrelationId {

    /**
     * Nombre estándar del header de correlación entre Gateway y servicios.
     */
    public static final String HEADER_NAME = "X-Correlation-ID";
    private static final Pattern VALID_PATTERN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{7,127}");

    private CorrelationId() {
    }

    /**
     * Comprueba que un identificador tenga un formato seguro y una longitud
     * comprendida entre 8 y 128 caracteres.
     *
     * @param value valor recibido en el header de correlación
     * @return {@code true} cuando el valor cumple la regla de validación
     */
    public static boolean isValid(String value) {
        return value != null && VALID_PATTERN.matcher(value).matches();
    }

    /**
     * Genera un identificador aleatorio para una solicitud que no aporta uno.
     *
     * @return UUID utilizado como identificador de correlación
     */
    public static String generate() {
        return UUID.randomUUID().toString();
    }
}
