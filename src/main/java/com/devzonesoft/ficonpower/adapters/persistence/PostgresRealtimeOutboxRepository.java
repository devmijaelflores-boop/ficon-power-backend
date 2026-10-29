package com.devzonesoft.ficonpower.adapters.persistence;

import com.devzonesoft.ficonpower.domain.RealtimeChange;
import com.devzonesoft.ficonpower.domain.RealtimeChange.Audience;
import com.devzonesoft.ficonpower.ports.output.RealtimeOutboxRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Repository
public class PostgresRealtimeOutboxRepository implements RealtimeOutboxRepository {

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public PostgresRealtimeOutboxRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public List<RealtimeChange> claimPending(int limit) {
        return jdbc.query("""
                UPDATE realtime_outbox outbox
                SET claimed_at = now(),
                    attempts = attempts + 1,
                    last_error = NULL
                WHERE outbox.id IN (
                    SELECT candidate.id
                    FROM realtime_outbox candidate
                    WHERE candidate.published_at IS NULL
                      AND (candidate.claimed_at IS NULL
                           OR candidate.claimed_at < now() - interval '1 minute')
                    ORDER BY candidate.id
                    FOR UPDATE SKIP LOCKED
                    LIMIT ?
                )
                RETURNING outbox.id, outbox.event_type, outbox.entity_type,
                          outbox.entity_id, outbox.audience, outbox.target_user_id,
                          outbox.payload::text AS payload, outbox.created_at
                """, this::mapChange, limit);
    }

    @Override
    public void markPublished(List<Long> ids) {
        if (ids.isEmpty()) {
            return;
        }
        jdbc.batchUpdate(
                "UPDATE realtime_outbox SET published_at = now() WHERE id = ?",
                ids,
                ids.size(),
                (statement, id) -> statement.setLong(1, id));
    }

    @Override
    public int deletePublishedOlderThanDays(int retentionDays) {
        return jdbc.update("""
                DELETE FROM realtime_outbox
                WHERE published_at < now() - (? * interval '1 day')
                """, retentionDays);
    }

    private RealtimeChange mapChange(ResultSet rs, int rowNum) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        return new RealtimeChange(
                rs.getLong("id"),
                rs.getString("event_type"),
                rs.getString("entity_type"),
                rs.getString("entity_id"),
                Audience.valueOf(rs.getString("audience")),
                rs.getObject("target_user_id", UUID.class),
                objectMapper.readTree(rs.getString("payload")),
                createdAt.toInstant());
    }
}
