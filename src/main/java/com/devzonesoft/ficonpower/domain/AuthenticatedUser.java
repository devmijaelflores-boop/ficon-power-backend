package com.devzonesoft.ficonpower.domain;

import java.util.UUID;

public record AuthenticatedUser(
        UUID id,
        String username,
        UserRole role,
        String displayName,
        String email,
        String whatsappPhone
) {
    public AuthenticatedUser(UUID id, String username, UserRole role, String displayName) {
        this(id, username, role, displayName, null, null);
    }
}
