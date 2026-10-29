package com.devzonesoft.ficonpower.usecase;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.devzonesoft.ficonpower.domain.AuthenticatedUser;
import com.devzonesoft.ficonpower.domain.RealtimeChange;
import com.devzonesoft.ficonpower.domain.RealtimeChange.Audience;
import com.devzonesoft.ficonpower.domain.UserRole;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class RealtimeSseHubTest {

    private final RealtimeSseHub hub = new RealtimeSseHub();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void limitsAdministrativeAndUserEventsToTheirAudience() {
        UUID userId = UUID.randomUUID();
        AuthenticatedUser user = user(userId, UserRole.USER);
        AuthenticatedUser admin = user(UUID.randomUUID(), UserRole.ADMIN);

        assertTrue(hub.canReceive(user, change(Audience.AUTHENTICATED, null)));
        assertFalse(hub.canReceive(user, change(Audience.ADMIN, null)));
        assertTrue(hub.canReceive(admin, change(Audience.ADMIN, null)));
        assertTrue(hub.canReceive(user, change(Audience.USER, userId)));
        assertFalse(hub.canReceive(admin, change(Audience.USER, userId)));
    }

    private AuthenticatedUser user(UUID id, UserRole role) {
        return new AuthenticatedUser(id, "user", role, "User");
    }

    private RealtimeChange change(Audience audience, UUID targetUserId) {
        return new RealtimeChange(
                1L,
                "device.updated",
                "device",
                "entity-1",
                audience,
                targetUserId,
                objectMapper.createObjectNode(),
                Instant.parse("2026-09-13T12:00:00Z"));
    }
}
