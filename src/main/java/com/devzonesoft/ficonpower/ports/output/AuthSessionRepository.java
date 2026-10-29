package com.devzonesoft.ficonpower.ports.output;

import com.devzonesoft.ficonpower.domain.AuthenticatedUser;
import com.devzonesoft.ficonpower.domain.RefreshSession;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AuthSessionRepository {

    void create(
            UUID userId,
            String tokenHash,
            String refreshTokenHash,
            String ip,
            String userAgent,
            Instant expiresAt,
            Instant refreshExpiresAt
    );

    Optional<AuthenticatedUser> findAuthenticatedUser(String tokenHash);

    Optional<RefreshSession> findRefreshSession(String refreshTokenHash);

    boolean rotate(
            UUID sessionId,
            String previousRefreshTokenHash,
            String tokenHash,
            String refreshTokenHash,
            String ip,
            String userAgent,
            Instant expiresAt,
            Instant refreshExpiresAt
    );

    void revoke(String tokenHash);

    void revokeByRefreshToken(String refreshTokenHash);

    boolean revokeById(UUID sessionId);
}
