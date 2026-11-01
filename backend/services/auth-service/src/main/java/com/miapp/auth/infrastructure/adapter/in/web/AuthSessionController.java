package com.miapp.auth.infrastructure.adapter.in.web;

import com.miapp.auth.application.port.in.LogoutUseCase;
import com.miapp.auth.application.port.in.RefreshSessionUseCase;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador REST para rotación de sesión y logout.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthSessionController {

    private final RefreshSessionUseCase refreshUseCase;
    private final LogoutUseCase logoutUseCase;
    private final RefreshTokenCookieManager cookies;

    public AuthSessionController(RefreshSessionUseCase refreshUseCase,
                                 LogoutUseCase logoutUseCase,
                                 RefreshTokenCookieManager cookies) {
        this.refreshUseCase = refreshUseCase;
        this.logoutUseCase = logoutUseCase;
        this.cookies = cookies;
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(HttpServletRequest request) {
        var result = refreshUseCase.refresh(readToken(request));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookies.create(result.refreshToken()).toString())
                .body(LoginResponse.from(new com.miapp.auth.application.result.LoginResult(
                        true, new com.miapp.auth.application.result.LoginResult.UserSummary(
                        result.user().id(), result.user().firstName(), result.user().lastName(),
                        result.user().username(), result.user().email(), result.user().createdAt()),
                        result.accessToken(), result.expiresIn(), result.refreshToken())));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        logoutUseCase.logout(readToken(request));
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookies.clear().toString())
                .build();
    }

    private String readToken(HttpServletRequest request) {
        Cookie[] requestCookies = request.getCookies();
        if (requestCookies == null) {
            return null;
        }
        for (Cookie cookie : requestCookies) {
            if (cookies.name().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
