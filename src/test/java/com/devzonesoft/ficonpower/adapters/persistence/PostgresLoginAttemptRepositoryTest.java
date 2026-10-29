package com.devzonesoft.ficonpower.adapters.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class PostgresLoginAttemptRepositoryTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final PostgresLoginAttemptRepository repository =
            new PostgresLoginAttemptRepository(jdbc);

    @Test
    void detectsActiveBlocksAndTreatsNullCountsAsInactive() {
        when(jdbc.queryForObject(
                argThat(sql -> sql != null && sql.contains("FROM blocked_login_users")),
                eq(Integer.class),
                eq("admin"),
                eq("10.0.0.8"))).thenReturn(1);
        when(jdbc.queryForObject(
                argThat(sql -> sql != null && sql.contains("FROM blocked_login_users")),
                eq(Integer.class),
                eq("other"),
                eq("10.0.0.9"))).thenReturn(null);

        assertTrue(repository.hasActiveBlock("admin", "10.0.0.8"));
        assertFalse(repository.hasActiveBlock("other", "10.0.0.9"));
    }

    @Test
    void countsFailuresUsingTheSameWindowForBothConditions() {
        Instant windowStart = Instant.parse("2026-08-13T20:00:00Z");
        Timestamp timestamp = Timestamp.from(windowStart);
        when(jdbc.queryForObject(
                argThat(sql -> sql != null && sql.contains("FROM login_attempts")),
                eq(Integer.class),
                eq("admin"),
                eq("10.0.0.8"),
                eq(timestamp),
                eq("admin"),
                eq("10.0.0.8"),
                eq(timestamp))).thenReturn(5);

        assertEquals(5, repository.countFailedAttemptsSinceLastSuccess(
                "admin", "10.0.0.8", windowStart));
    }

    @Test
    void treatsANullFailedCountAsZero() {
        when(jdbc.queryForObject(
                argThat(sql -> sql != null && sql.contains("FROM login_attempts")),
                eq(Integer.class),
                eq("admin"),
                eq("10.0.0.8"),
                any(Timestamp.class),
                eq("admin"),
                eq("10.0.0.8"),
                any(Timestamp.class))).thenReturn(null);

        assertEquals(0, repository.countFailedAttemptsSinceLastSuccess(
                "admin", "10.0.0.8", Instant.now()));
    }

    @Test
    void upsertsTheBlockWithTheCalculatedExpiry() {
        UUID userId = UUID.randomUUID();
        Instant expiresAt = Instant.parse("2026-08-13T20:10:00Z");

        repository.upsertBlock("admin", userId, "10.0.0.8", "JUnit", 5, expiresAt);

        verify(jdbc).update(
                argThat(sql -> sql != null && sql.contains("INSERT INTO blocked_login_users")
                        && sql.contains("ON CONFLICT (username, ip_address)")),
                eq("admin"),
                eq(userId),
                eq("10.0.0.8"),
                eq("JUnit"),
                eq(5),
                eq(Timestamp.from(expiresAt)));
    }
}
