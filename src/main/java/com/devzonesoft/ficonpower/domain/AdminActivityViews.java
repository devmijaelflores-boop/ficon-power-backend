package com.devzonesoft.ficonpower.domain;

import java.time.Instant;

public final class AdminActivityViews {

    private AdminActivityViews() {
    }

    public record AuditLogView(
            String id,
            String actorUserId,
            String actorUsername,
            String actorRole,
            String action,
            String entityType,
            String entityId,
            String ipAddress,
            String userAgent,
            String metadata,
            Instant createdAt
    ) {
    }

    public record LoginAttemptView(
            String id,
            String username,
            String userId,
            String ipAddress,
            String userAgent,
            boolean success,
            String reason,
            Instant createdAt
    ) {
    }

    public record AuthSessionView(
            String id,
            String userId,
            String username,
            String role,
            String ipAddress,
            String userAgent,
            Instant createdAt,
            Instant expiresAt,
            Instant revokedAt
    ) {
    }

    public record BlockedLoginUserView(
            String id,
            String username,
            String userId,
            String displayName,
            String ipAddress,
            String userAgent,
            int failedAttempts,
            String reason,
            Instant blockedAt,
            Instant expiresAt,
            Instant unblockedAt,
            String unblockedBy,
            String unblockedByUsername,
            String unblockReason,
            boolean active
    ) {
    }
}
