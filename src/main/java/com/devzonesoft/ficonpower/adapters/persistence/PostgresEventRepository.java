package com.devzonesoft.ficonpower.adapters.persistence;

import com.devzonesoft.ficonpower.domain.Event;
import com.devzonesoft.ficonpower.ports.output.EventRepository;
import java.sql.Timestamp;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresEventRepository implements EventRepository {

    private final JdbcTemplate jdbcTemplate;

    public PostgresEventRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(Event event) {
        jdbcTemplate.update("""
                INSERT INTO events (
                    device_id, assignment_id, site_id, event_type,
                    payload, occurred_at, received_at
                )
                SELECT d.id, a.id, d.site_id, ?, CAST(? AS jsonb), ?, ?
                FROM devices d
                LEFT JOIN device_assignments a
                    ON a.device_id = d.id AND a.active = true
                WHERE d.external_id = ?
                """, event.eventType(), event.payload().toString(),
                Timestamp.from(event.occurredAt()), Timestamp.from(event.receivedAt()),
                event.deviceId());
    }
}
