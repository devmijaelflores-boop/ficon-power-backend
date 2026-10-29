package com.devzonesoft.ficonpower.adapters.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devzonesoft.ficonpower.domain.AppUser;
import com.devzonesoft.ficonpower.domain.UserRole;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class PostgresUserRepositoryTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final PostgresUserRepository repository = new PostgresUserRepository(jdbc);

    @Test
    void mapsAUserWithoutChangingStoredValues() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-08-13T18:00:00Z");
        when(rs.getObject("id", UUID.class)).thenReturn(id);
        when(rs.getString("username")).thenReturn("admin");
        when(rs.getString("password_hash")).thenReturn("{bcrypt}hash");
        when(rs.getString("role")).thenReturn("ADMIN");
        when(rs.getString("display_name")).thenReturn("Administrador");
        when(rs.getString("email")).thenReturn("admin@ficon.pe");
        when(rs.getBoolean("is_active")).thenReturn(false);
        when(rs.getTimestamp("created_at")).thenReturn(Timestamp.from(createdAt));
        when(rs.getTimestamp("updated_at")).thenReturn(null);

        AppUser user = repository.mapUser(rs, 0);

        assertEquals(id, user.id());
        assertEquals("admin", user.username());
        assertEquals("{bcrypt}hash", user.passwordHash());
        assertEquals(UserRole.ADMIN, user.role());
        assertEquals("Administrador", user.displayName());
        assertEquals("admin@ficon.pe", user.email());
        assertFalse(user.active());
        assertEquals(createdAt, user.createdAt());
        assertNull(user.updatedAt());
    }

    @Test
    void countsOnlyActiveUsersForTheRequestedRole() {
        when(jdbc.queryForObject(
                argThat(sql -> sql.contains("FROM app_users")
                        && sql.contains("is_active = true")),
                eq(Integer.class),
                eq("ADMIN"))).thenReturn(2);

        assertEquals(2, repository.countActiveByRole(UserRole.ADMIN));
    }

    @Test
    void treatsANullActiveUserCountAsZero() {
        when(jdbc.queryForObject(
                argThat(sql -> sql.contains("FROM app_users")),
                eq(Integer.class),
                eq("USER"))).thenReturn(null);

        assertEquals(0, repository.countActiveByRole(UserRole.USER));
    }

    @Test
    void updatesThePasswordHashWithoutReceivingPlainText() {
        UUID id = UUID.randomUUID();

        repository.updatePassword(id, "{bcrypt}new-hash");

        verify(jdbc).queryForObject(
                argThat(sql -> sql.contains("UPDATE app_users")
                        && sql.contains("SET password_hash = ?")),
                org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<AppUser>>any(),
                eq("{bcrypt}new-hash"),
                eq(id));
    }
}
