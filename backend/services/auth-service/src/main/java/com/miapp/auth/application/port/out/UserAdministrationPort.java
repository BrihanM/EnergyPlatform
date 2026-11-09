package com.miapp.auth.application.port.out;

import com.miapp.auth.domain.model.AccountStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface UserAdministrationPort {
    List<UserRow> findUsers(int limit, long offset);
    record UserRow(UUID id, String firstName, String lastName, String username, String email,
                   AccountStatus status, Instant createdAt) {}
}
