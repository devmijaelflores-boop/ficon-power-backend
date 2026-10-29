package com.devzonesoft.ficonpower.domain;

import java.util.UUID;

public record AuditLogEntry(
        UUID actorUserId,
        String actorUsername,
        String actorRole,
        String action,
        String entityType,
        String entityId,
        String ipAddress,
        String userAgent,
        String metadata
) {
}
