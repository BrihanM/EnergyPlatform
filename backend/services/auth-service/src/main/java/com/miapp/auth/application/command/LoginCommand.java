package com.miapp.auth.application.command;

/**
 * Entrada del caso de uso de login, independiente del transporte HTTP.
 */
public record LoginCommand(String identifier, String password) {

    @Override
    public String toString() {
        return "LoginCommand[identifier=[REDACTED], password=[REDACTED]]";
    }
}
