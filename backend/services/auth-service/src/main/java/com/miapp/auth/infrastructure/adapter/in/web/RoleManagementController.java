package com.miapp.auth.infrastructure.adapter.in.web;

import com.miapp.auth.application.service.RoleManagementService;
import com.miapp.auth.domain.model.Role;
import com.miapp.auth.domain.exception.InvalidRoleOperationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.UUID;

/**
 * Administrative role-management adapter. Trusted identity headers are supplied
 * by the Gateway after JWT validation; direct public clients cannot set them.
 */
@RestController
@RequestMapping("/api/v1/admin")
public class RoleManagementController {

    private static final Logger AUDIT = LoggerFactory.getLogger("com.miapp.auth.audit");
    private static final String USER_ID = "X-Authenticated-User-Id";
    private static final String ROLES = "X-Authenticated-Roles";

    private final RoleManagementService service;

    public RoleManagementController(RoleManagementService service) {
        this.service = service;
    }

    @GetMapping("/roles")
    public Set<Role> availableRoles(HttpServletRequest request) {
        UUID actor = requireAdmin(request);
        audit("LIST_ROLES", actor, actor, "SUCCESS");
        return service.availableRoles();
    }

    @GetMapping("/users/{userId}/roles")
    public Set<Role> userRoles(@PathVariable UUID userId, HttpServletRequest request) {
        UUID actor = requireAdmin(request);
        Set<Role> result = service.rolesOf(actor, userId);
        audit("VIEW_USER_ROLES", actor, userId, "SUCCESS");
        return result;
    }

    @PostMapping("/users/{userId}/roles")
    @ResponseStatus(HttpStatus.OK)
    public Set<Role> assign(@PathVariable UUID userId,
                            @Valid @RequestBody RoleRequest request,
                            HttpServletRequest httpRequest) {
        UUID actor = requireAdmin(httpRequest);
        Set<Role> result = service.assign(actor, userId, request.role());
        audit("ASSIGN_ROLE", actor, userId, "SUCCESS");
        return result;
    }

    @DeleteMapping("/users/{userId}/roles/{role}")
    public Set<Role> revoke(@PathVariable UUID userId,
                            @PathVariable Role role,
                            HttpServletRequest request) {
        UUID actor = requireAdmin(request);
        Set<Role> result = service.revoke(actor, userId, role);
        audit("REVOKE_ROLE", actor, userId, "SUCCESS");
        return result;
    }

    private UUID requireAdmin(HttpServletRequest request) {
        UUID actor;
        try {
            actor = UUID.fromString(request.getHeader(USER_ID));
        } catch (Exception exception) {
            throw new InvalidRoleOperationException("Administrator role required");
        }
        String roles = request.getHeader(ROLES);
        if (roles == null || !Set.of(roles.split(",")).contains(Role.ADMIN.name())) {
            throw new InvalidRoleOperationException("Administrator role required");
        }
        return actor;
    }

    private void audit(String action, UUID actor, UUID target, String result) {
        AUDIT.info("role_change action={} actorUserId={} targetUserId={} result={}",
                action, actor, target, result);
    }

    public record RoleRequest(@NotNull Role role) {
    }
}
