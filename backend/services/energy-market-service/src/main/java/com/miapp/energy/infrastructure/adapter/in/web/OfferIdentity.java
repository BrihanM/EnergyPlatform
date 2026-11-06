package com.miapp.energy.infrastructure.adapter.in.web;

import com.miapp.energy.domain.exception.EnergyOfferDomainException;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

final class OfferIdentity {

    static final String USER_ID = "X-Authenticated-User-Id";
    static final String ROLES = "X-Authenticated-Roles";

    private OfferIdentity() {
    }

    static UUID requireNormalUser(HttpServletRequest request) {
        UUID userId;
        try {
            userId = UUID.fromString(request.getHeader(USER_ID));
        } catch (Exception exception) {
            throw new EnergyOfferDomainException("Authenticated user is required");
        }
        String roles = request.getHeader(ROLES);
        Set<String> roleSet = roles == null ? Set.of()
                : Arrays.stream(roles.split(",")).map(String::trim).collect(java.util.stream.Collectors.toSet());
        if (!roleSet.contains("NORMAL_USER")) {
            throw new EnergyOfferDomainException("NORMAL_USER role is required");
        }
        return userId;
    }
}
