package com.devzonesoft.ficonpower.domain;

import java.time.Instant;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

public record RealtimeChange(
        long id,
        String eventType,
        String entityType,
        String entityId,
        Audience audience,
        UUID targetUserId,
        JsonNode payload,
        Instant createdAt
) {
    public enum Audience {
        AUTHENTICATED,
        ADMIN,
        USER
    }
}
