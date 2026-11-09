package com.miapp.auth.infrastructure.adapter.in.web;

import com.miapp.auth.domain.exception.InvalidCredentialsException;
import com.miapp.auth.domain.exception.InvalidRoleOperationException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.UUID;

final class TrustedIdentity {
    private TrustedIdentity() {}

    static UUID require(HttpServletRequest request, String role) {
        UUID user;
        try {
            user = UUID.fromString(request.getHeader("X-Authenticated-User-Id"));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new InvalidCredentialsException();
        }
        String roles = request.getHeader("X-Authenticated-Roles");
        if (roles == null || Arrays.stream(roles.split(",")).map(String::trim).noneMatch(role::equals)) {
            throw new InvalidRoleOperationException("ADMIN".equals(role)
                    ? "Administrator role required" : "Auditor role required");
        }
        return user;
    }
}
