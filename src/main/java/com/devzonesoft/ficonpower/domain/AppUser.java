package com.devzonesoft.ficonpower.domain;

import java.time.Instant;
import java.util.UUID;

public record AppUser(
        UUID id,
        String username,
        String passwordHash,
        UserRole role,
        String displayName,
        String email,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public AppUser(
            UUID id,
            String username,
            String passwordHash,
            UserRole role,
            String displayName,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(id, username, passwordHash, role, displayName, null, active, createdAt, updatedAt);
    }
}
