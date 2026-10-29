package com.devzonesoft.ficonpower.adapters.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devzonesoft.ficonpower.domain.AdminActivityViews.AuditLogView;
import com.devzonesoft.ficonpower.domain.AdminActivityViews.BlockedLoginUserView;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class PostgresAdminActivityRepositoryTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final PostgresAdminActivityRepository repository =
            new PostgresAdminActivityRepository(jdbc);

    @Test
    void queriesAuditLogsUsingTheRequestedLimit() {
        when(jdbc.query(
                anyString(),
                org.mockito.ArgumentMatchers.<RowMapper<AuditLogView>>any(),
                eq(25))).thenReturn(List.of());

        assertEquals(List.of(), repository.findAuditLogs(25));

        verify(jdbc).query(
                org.mockito.ArgumentMatchers.argThat(sql -> sql.contains("FROM audit_logs")),
                org.mockito.ArgumentMatchers.<RowMapper<AuditLogView>>any(),
                eq(25));
    }

    @Test
    void unblocksTheRequestedRecordWithActorAndReason() {
        UUID blockedId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        when(jdbc.update(anyString(), eq(actorId), eq("reviewed"), eq(blockedId)))
                .thenReturn(1);

        assertEquals(1, repository.unblock(blockedId, actorId, "reviewed"));

        verify(jdbc).update(
                org.mockito.ArgumentMatchers.argThat(sql -> sql.contains("UPDATE blocked_login_users")),
                eq(actorId),
                eq("reviewed"),
                eq(blockedId));
    }

    @Test
    void mapsNullableBlockedUserFieldsWithoutChangingTheApiShape() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        UUID id = UUID.randomUUID();
        Instant blockedAt = Instant.parse("2026-08-13T20:00:00Z");
        when(rs.getObject("id", UUID.class)).thenReturn(id);
        when(rs.getString("username")).thenReturn("admin");
        when(rs.getObject("user_id", UUID.class)).thenReturn(null);
        when(rs.getString("display_name")).thenReturn(null);
        when(rs.getString("ip_address")).thenReturn("10.0.0.8");
        when(rs.getString("user_agent")).thenReturn("JUnit");
        when(rs.getInt("failed_attempts")).thenReturn(5);
        when(rs.getString("reason")).thenReturn("too_many_failed_attempts");
        when(rs.getTimestamp("blocked_at")).thenReturn(Timestamp.from(blockedAt));
        when(rs.getTimestamp("expires_at")).thenReturn(null);
        when(rs.getTimestamp("unblocked_at")).thenReturn(null);
        when(rs.getObject("unblocked_by", UUID.class)).thenReturn(null);
        when(rs.getString("unblocked_by_username")).thenReturn(null);
        when(rs.getString("unblock_reason")).thenReturn(null);
        when(rs.getBoolean("active")).thenReturn(true);

        BlockedLoginUserView view = repository.mapBlockedUser(rs, 0);

        assertEquals(id.toString(), view.id());
        assertEquals("admin", view.username());
        assertEquals(blockedAt, view.blockedAt());
        assertEquals(5, view.failedAttempts());
        assertTrue(view.active());
        assertNull(view.userId());
        assertNull(view.expiresAt());
        assertNull(view.unblockedAt());
        assertNull(view.unblockedBy());
    }
}
