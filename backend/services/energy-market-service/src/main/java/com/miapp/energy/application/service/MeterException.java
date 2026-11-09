package com.miapp.energy.application.service;

public class MeterException extends RuntimeException {
    public enum Kind { UNAUTHORIZED, FORBIDDEN, NOT_FOUND, CONFLICT }
    private final Kind kind;

    public MeterException(Kind kind) {
        super(kind.name());
        this.kind = kind;
    }

    public Kind kind() { return kind; }
}
