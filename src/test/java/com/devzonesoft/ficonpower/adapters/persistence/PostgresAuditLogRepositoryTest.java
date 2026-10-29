package com.devzonesoft.ficonpower.adapters.persistence;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.devzonesoft.ficonpower.domain.AuditLogEntry;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class PostgresAuditLogRepositoryTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final PostgresAuditLogRepository repository = new PostgresAuditLogRepository(jdbc);

    @Test
    void savesEveryAuditFieldWithoutTransformingIt() {
        UUID actorId = UUID.randomUUID();
        AuditLogEntry entry = new AuditLogEntry(
                actorId,
                "admin",
                "ADMIN",
                "device.update",
                "device",
                "device-1",
                "10.0.0.8",
                "JUnit",
                "{\"name\":\"Meter\"}");

        repository.save(entry);

        verify(jdbc).update(
                argThat(sql -> sql.contains("INSERT INTO audit_logs")),
                eq(actorId),
                eq("admin"),
                eq("ADMIN"),
                eq("device.update"),
                eq("device"),
                eq("device-1"),
                eq("10.0.0.8"),
                eq("JUnit"),
                eq("{\"name\":\"Meter\"}"));
    }
}
