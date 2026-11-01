package com.miapp.auth.infrastructure.adapter.in.web;

import com.miapp.auth.application.command.RegisterUserCommand;
import com.miapp.auth.application.port.in.RegisterUserUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Adaptador REST de entrada para el registro de cuentas.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthRegistrationController {

    private final RegisterUserUseCase registerUserUseCase;

    public AuthRegistrationController(RegisterUserUseCase registerUserUseCase) {
        this.registerUserUseCase = registerUserUseCase;
    }

    /**
     * Registra una identidad y su cuenta de acceso.
     *
     * @param request datos validados del registro
     * @return respuesta segura con estado {@code 201 Created}
     */
    @PostMapping("/register")
    public ResponseEntity<RegisterUserResponse> register(
            @Valid @RequestBody RegisterUserRequest request) {
        RegisterUserCommand command = new RegisterUserCommand(
                request.firstName(), request.lastName(), request.username(),
                request.email(), request.password());
        RegisterUserResponse response = RegisterUserResponse.from(registerUserUseCase.register(command));
        return ResponseEntity.created(URI.create("/api/v1/users/" + response.id())).body(response);
    }
}
