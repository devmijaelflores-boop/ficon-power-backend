package com.devzonesoft.ficonpower.usecase;

import com.devzonesoft.ficonpower.domain.AuthenticatedUser;
import com.devzonesoft.ficonpower.domain.RefreshSession;
import com.devzonesoft.ficonpower.config.AuthSessionProperties;
import com.devzonesoft.ficonpower.ports.output.AuthSessionRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AuthSessionService {

    private final AuthSessionRepository authSessions;
    private final SessionTokenService sessionTokens;
    private final AuthSessionProperties properties;

    public AuthSessionService(
            AuthSessionRepository authSessions,
            SessionTokenService sessionTokens,
            AuthSessionProperties properties
    ) {
        this.authSessions = authSessions;
        this.sessionTokens = sessionTokens;
        this.properties = properties;
    }

    public Session create(UUID userId, String ip, String userAgent) {
        String token = sessionTokens.generate();
        String refreshToken = sessionTokens.generate();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.getAccessDuration());
        Instant refreshExpiresAt = now.plus(properties.getRefreshDuration());
        authSessions.create(
                userId,
                sessionTokens.hash(token),
                sessionTokens.hash(refreshToken),
                ip,
                userAgent,
                expiresAt,
                refreshExpiresAt);
        return new Session(token, expiresAt, refreshToken, refreshExpiresAt);
    }

    public RefreshedSession refresh(String refreshToken, String ip, String userAgent) {
        String previousHash = sessionTokens.hash(refreshToken);
        RefreshSession current = authSessions.findRefreshSession(previousHash)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token inválido o vencido"));
        String token = sessionTokens.generate();
        String nextRefreshToken = sessionTokens.generate();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.getAccessDuration());
        Instant refreshExpiresAt = now.plus(properties.getRefreshDuration());
        boolean rotated = authSessions.rotate(
                current.sessionId(),
                previousHash,
                sessionTokens.hash(token),
                sessionTokens.hash(nextRefreshToken),
                ip,
                userAgent,
                expiresAt,
                refreshExpiresAt);
        if (!rotated) {
            throw new IllegalArgumentException("Refresh token inválido o reutilizado");
        }
        return new RefreshedSession(
                token,
                expiresAt,
                nextRefreshToken,
                refreshExpiresAt,
                current.user());
    }

    public Optional<AuthenticatedUser> authenticate(String token) {
        return authSessions.findAuthenticatedUser(sessionTokens.hash(token));
    }

    public void revoke(String token) {
        authSessions.revoke(sessionTokens.hash(token));
    }

    public void revokeRefreshToken(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            authSessions.revokeByRefreshToken(sessionTokens.hash(refreshToken));
        }
    }

    public boolean revoke(UUID sessionId) {
        return authSessions.revokeById(sessionId);
    }

    public record Session(
            String token,
            Instant expiresAt,
            String refreshToken,
            Instant refreshExpiresAt
    ) {
    }

    public record RefreshedSession(
            String token,
            Instant expiresAt,
            String refreshToken,
            Instant refreshExpiresAt,
            AuthenticatedUser user
    ) {
    }
}
