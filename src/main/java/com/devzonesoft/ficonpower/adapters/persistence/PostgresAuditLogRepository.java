package com.devzonesoft.ficonpower.adapters.persistence;

import com.devzonesoft.ficonpower.domain.AuditLogEntry;
import com.devzonesoft.ficonpower.ports.output.AuditLogRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresAuditLogRepository implements AuditLogRepository {

    private final JdbcTemplate jdbc;

    public PostgresAuditLogRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(AuditLogEntry entry) {
        jdbc.update("""
                        INSERT INTO audit_logs (
                            actor_user_id, actor_username, actor_role, action, entity_type,
                            entity_id, ip_address, user_agent, metadata
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                entry.actorUserId(),
                entry.actorUsername(),
                entry.actorRole(),
                entry.action(),
                entry.entityType(),
                entry.entityId(),
                entry.ipAddress(),
                entry.userAgent(),
                entry.metadata());
    }
}
