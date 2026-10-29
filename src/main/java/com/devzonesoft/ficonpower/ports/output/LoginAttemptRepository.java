package com.devzonesoft.ficonpower.ports.output;

import java.time.Instant;
import java.util.UUID;

public interface LoginAttemptRepository {

    boolean hasActiveBlock(String username, String ip);

    int countFailedAttemptsSinceLastSuccess(String username, String ip, Instant windowStart);

    void upsertBlock(
            String username,
            UUID userId,
            String ip,
            String userAgent,
            int failedAttempts,
            Instant expiresAt
    );

    void clearExpiredOrManualBlock(String username, String ip);

    void record(
            String username,
            UUID userId,
            String ip,
            String userAgent,
            boolean success,
            String reason
    );
}
