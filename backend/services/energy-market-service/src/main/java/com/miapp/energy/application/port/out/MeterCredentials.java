package com.miapp.energy.application.port.out;

public interface MeterCredentials {
    record IssuedCredential(String value, byte[] hash) {
        @Override public String toString() { return "IssuedCredential[REDACTED]"; }
    }

    IssuedCredential issue();
    boolean matches(String supplied, byte[] hash);
}
