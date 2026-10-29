package com.devzonesoft.ficonpower.adapters.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devzonesoft.ficonpower.domain.AuthenticatedUser;
import com.devzonesoft.ficonpower.domain.UserRole;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class PostgresAuthSessionRepositoryTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final PostgresAuthSessionRepository repository =
            new PostgresAuthSessionRepository(jdbc);

    @Test
    void createsASessionWithTheProvidedHashAndExpiry() {
        UUID userId = UUID.randomUUID();
        Instant expiresAt = Instant.parse("2026-08-14T08:00:00Z");
        Instant refreshExpiresAt = Instant.parse("2026-11-12T08:00:00Z");

        repository.create(
                userId,
                "hashed-token",
                "hashed-refresh-token",
                "10.0.0.8",
                "JUnit",
                expiresAt,
                refreshExpiresAt);

        verify(jdbc).update(
                argThat(sql -> sql.contains("INSERT INTO auth_sessions")),
                eq(userId),
                eq("hashed-token"),
                eq("hashed-refresh-token"),
                eq("10.0.0.8"),
                eq("JUnit"),
                eq(Timestamp.from(expiresAt)),
                eq(Timestamp.from(refreshExpiresAt)));
    }

    @Test
    void mapsTheAuthenticatedUserWithoutChangingRoles() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        UUID userId = UUID.randomUUID();
        when(rs.getObject("id", UUID.class)).thenReturn(userId);
        when(rs.getString("username")).thenReturn("admin");
        when(rs.getString("role")).thenReturn("ADMIN");
        when(rs.getString("display_name")).thenReturn("Administrador");
        when(rs.getString("email")).thenReturn("admin@ficon.pe");
        when(rs.getString("whatsapp_phone")).thenReturn("51987654321");

        AuthenticatedUser user = repository.mapAuthenticatedUser(rs, 0);

        assertEquals(userId, user.id());
        assertEquals("admin", user.username());
        assertEquals(UserRole.ADMIN, user.role());
        assertEquals("Administrador", user.displayName());
        assertEquals("admin@ficon.pe", user.email());
        assertEquals("51987654321", user.whatsappPhone());
    }

    @Test
    void revokesOnlyTheMatchingActiveSession() {
        repository.revoke("hashed-token");

        verify(jdbc).update(
                argThat(sql -> sql.contains("UPDATE auth_sessions")
                        && sql.contains("revoked_at IS NULL")),
                eq("hashed-token"));
    }

    @Test
    void revokesAnActiveSessionByItsAdministrativeId() {
        UUID sessionId = UUID.randomUUID();
        when(jdbc.update(
                argThat(sql -> sql.contains("WHERE id = ?")
                        && sql.contains("revoked_at IS NULL")),
                eq(sessionId))).thenReturn(1);

        boolean revoked = repository.revokeById(sessionId);

        assertTrue(revoked);
    }
}
