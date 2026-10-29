package com.devzonesoft.ficonpower.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devzonesoft.ficonpower.domain.AuthenticatedUser;
import com.devzonesoft.ficonpower.config.AuthSessionProperties;
import com.devzonesoft.ficonpower.domain.UserRole;
import com.devzonesoft.ficonpower.ports.output.AuthSessionRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuthSessionServiceTest {

    private final AuthSessionRepository authSessions = mock(AuthSessionRepository.class);
    private final SessionTokenService sessionTokens = mock(SessionTokenService.class);
    private final AuthSessionProperties properties = new AuthSessionProperties();
    private final AuthSessionService service =
            new AuthSessionService(authSessions, sessionTokens, properties);

    @Test
    void createsHashedAccessAndRefreshCredentials() {
        UUID userId = UUID.randomUUID();
        when(sessionTokens.generate()).thenReturn("raw-token", "raw-refresh-token");
        when(sessionTokens.hash("raw-token")).thenReturn("hashed-token");
        when(sessionTokens.hash("raw-refresh-token")).thenReturn("hashed-refresh-token");
        Instant minimumExpiry = Instant.now().plusSeconds(24 * 60 * 60 - 1);

        AuthSessionService.Session session = service.create(userId, "10.0.0.8", "JUnit");

        assertEquals("raw-token", session.token());
        assertEquals("raw-refresh-token", session.refreshToken());
        assertFalse(session.expiresAt().isBefore(minimumExpiry));
        verify(authSessions).create(
                eq(userId),
                eq("hashed-token"),
                eq("hashed-refresh-token"),
                eq("10.0.0.8"),
                eq("JUnit"),
                any(Instant.class),
                any(Instant.class));
    }

    @Test
    void authenticatesUsingTheTokenHash() {
        AuthenticatedUser user = new AuthenticatedUser(
                UUID.randomUUID(), "admin", UserRole.ADMIN, "Admin");
        when(sessionTokens.hash("raw-token")).thenReturn("hashed-token");
        when(authSessions.findAuthenticatedUser("hashed-token")).thenReturn(Optional.of(user));

        Optional<AuthenticatedUser> result = service.authenticate("raw-token");

        assertSame(user, result.orElseThrow());
    }

    @Test
    void revokesOnlyTheMatchingTokenHash() {
        when(sessionTokens.hash("raw-token")).thenReturn("hashed-token");

        service.revoke("raw-token");

        verify(authSessions).revoke("hashed-token");
    }

    @Test
    void revokesASessionByItsAdministrativeId() {
        UUID sessionId = UUID.randomUUID();
        when(authSessions.revokeById(sessionId)).thenReturn(true);

        assertTrue(service.revoke(sessionId));

        verify(authSessions).revokeById(sessionId);
    }
}
