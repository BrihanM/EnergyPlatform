package com.miapp.auth.infrastructure.adapter.in.web;

import com.miapp.auth.application.port.out.RolePersistencePort;
import com.miapp.auth.application.service.AuthAuditService;
import com.miapp.auth.domain.exception.InvalidRoleOperationException;
import com.miapp.auth.domain.model.AuditEvent;
import com.miapp.auth.domain.model.Role;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth/audit/events")
public class AuthAuditController {
    private final AuthAuditService audit;
    private final RolePersistencePort roles;
    public AuthAuditController(AuthAuditService audit, RolePersistencePort roles) {
        this.audit = audit;
        this.roles = roles;
    }

    @GetMapping
    public AuthAuditService.Page query(@RequestParam int page, @RequestParam int size,
                                       @RequestParam(required = false) Instant from,
                                       @RequestParam(required = false) Instant to,
                                       @RequestParam(required = false) UUID user,
                                       @RequestParam(required = false) AuditEvent.Type type,
                                       HttpServletRequest request) {
        UUID actor = TrustedIdentity.require(request, "SECURITY_AUDITOR");
        if (!roles.findRolesByUserId(actor).contains(Role.SECURITY_AUDITOR)) {
            throw new InvalidRoleOperationException("Auditor role required");
        }
        return audit.query(from, to, user, type, page, size);
    }
}
