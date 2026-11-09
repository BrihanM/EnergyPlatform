package com.miapp.energy.infrastructure.adapter.in.web;

import com.miapp.energy.application.service.MarketAuditService;
import com.miapp.energy.application.service.MeterException;
import com.miapp.energy.domain.model.AuditEvent;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/market/audit/events")
public class MarketAuditController {
    private final MarketAuditService audit;
    public MarketAuditController(MarketAuditService audit) { this.audit = audit; }

    @GetMapping
    public MarketAuditService.Page query(@RequestParam int page, @RequestParam int size,
                                         @RequestParam(required = false) Instant from,
                                         @RequestParam(required = false) Instant to,
                                         @RequestParam(required = false) UUID user,
                                         @RequestParam(required = false) AuditEvent.Type type,
                                         HttpServletRequest request) {
        try {
            UUID.fromString(request.getHeader(OfferIdentity.USER_ID));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new MeterException(MeterException.Kind.UNAUTHORIZED);
        }
        String roles = request.getHeader(OfferIdentity.ROLES);
        if (roles == null || Arrays.stream(roles.split(",")).map(String::trim)
                .noneMatch("SECURITY_AUDITOR"::equals)) {
            throw new MeterException(MeterException.Kind.FORBIDDEN);
        }
        return audit.query(from, to, user, type, page, size);
    }
}
