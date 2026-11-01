package com.miapp.auth.infrastructure.adapter.in.web;

import com.miapp.auth.application.command.LoginCommand;
import com.miapp.auth.application.port.in.LoginUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador REST de entrada para la validación de credenciales.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthLoginController {

    private final LoginUseCase loginUseCase;
    private final RefreshTokenCookieManager cookieManager;

    public AuthLoginController(LoginUseCase loginUseCase, RefreshTokenCookieManager cookieManager) {
        this.loginUseCase = loginUseCase;
        this.cookieManager = cookieManager;
    }

    /**
     * Valida username o email y contraseña sin emitir tokens.
     *
     * @param request credenciales validadas
     * @return datos seguros del usuario autenticado
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginCommand command = new LoginCommand(request.identifier(), request.password());
        var result = loginUseCase.login(command);
        var response = ResponseEntity.ok();
        if (result.refreshToken() != null) {
            response.header(HttpHeaders.SET_COOKIE,
                    cookieManager.create(result.refreshToken()).toString());
        }
        return response.body(LoginResponse.from(result));
    }
}
