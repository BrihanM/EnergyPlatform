package com.miapp.auth.infrastructure.adapter.out.persistence;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Proporciona la clave de cifrado pgcrypto de datos personales desde la configuración externa.
 *
 * <p>La clave nunca se almacena en código ni en base de datos. Si falta o está vacía,
 * el contexto no arranca para evitar escribir datos en claro.</p>
 */
@Component
public class PiiEncryptionKeyProvider {

    private final String key;

    public PiiEncryptionKeyProvider(@Value("${auth.pii.encryption-key}") String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("AUTH_PII_ENCRYPTION_KEY must be configured");
        }
        this.key = key;
    }

    /**
     * @return clave simétrica para {@code pgp_sym_encrypt}/{@code pgp_sym_decrypt}
     */
    public String key() {
        return key;
    }

    @Override
    public String toString() {
        return "PiiEncryptionKeyProvider[redacted]";
    }
}
