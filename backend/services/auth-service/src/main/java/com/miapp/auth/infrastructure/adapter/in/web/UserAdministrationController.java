package com.miapp.auth.infrastructure.adapter.in.web;

import com.miapp.auth.application.service.UserAdministrationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
public class UserAdministrationController {
    private final UserAdministrationService service;
    public UserAdministrationController(UserAdministrationService service) { this.service = service; }

    @GetMapping
    public UserAdministrationService.Page users(@RequestParam int page, @RequestParam int size,
                                                 HttpServletRequest request) {
        return service.users(TrustedIdentity.require(request, "ADMIN"), page, size);
    }

    @PostMapping("/{userId}/suspension")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void suspend(@PathVariable UUID userId, HttpServletRequest request) {
        service.suspend(TrustedIdentity.require(request, "ADMIN"), userId);
    }

    @PostMapping("/{userId}/reactivation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reactivate(@PathVariable UUID userId, HttpServletRequest request) {
        service.reactivate(TrustedIdentity.require(request, "ADMIN"), userId);
    }
}
