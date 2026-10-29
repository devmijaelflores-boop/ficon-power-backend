package com.devzonesoft.ficonpower.adapters.persistence;

import com.devzonesoft.ficonpower.domain.PowerAlertEscalation;
import com.devzonesoft.ficonpower.ports.output.PowerAlertEscalationRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresPowerAlertEscalationRepository implements PowerAlertEscalationRepository {

    private final JdbcTemplate jdbc;

    public PostgresPowerAlertEscalationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void start(UUID incidentId, Instant firstReminderAt) {
        jdbc.update("""
                INSERT INTO power_alert_escalations (incident_id, next_alert_at)
                VALUES (?, ?)
                ON CONFLICT (incident_id) DO UPDATE
                SET next_alert_at = EXCLUDED.next_alert_at,
                    reviewed_at = NULL,
                    completed_at = NULL
                WHERE power_alert_escalations.reviewed_at IS NULL
                  AND power_alert_escalations.completed_at IS NULL
                """, incidentId, Timestamp.from(firstReminderAt));
    }

    @Override
    public List<PowerAlertEscalation> findDue(Instant now, int limit) {
        return jdbc.query("""
                SELECT pae.incident_id, d.name AS device_name, i.start_time,
                       pae.attempt_count, pae.next_alert_at
                FROM power_alert_escalations pae
                JOIN incidents i ON i.id = pae.incident_id
                JOIN devices d ON d.id = i.device_id
                WHERE i.status = 'active'
                  AND pae.reviewed_at IS NULL
                  AND pae.completed_at IS NULL
                  AND pae.next_alert_at <= ?
                ORDER BY pae.next_alert_at ASC
                LIMIT ?
                """, this::map, Timestamp.from(now), limit);
    }

    @Override
    public void markAttempt(UUID incidentId, int attemptCount, Instant sentAt, Instant nextAlertAt) {
        jdbc.update("""
                UPDATE power_alert_escalations
                SET attempt_count = ?,
                    last_alert_at = ?,
                    next_alert_at = ?
                WHERE incident_id = ?
                  AND reviewed_at IS NULL
                  AND completed_at IS NULL
                """, attemptCount, Timestamp.from(sentAt), Timestamp.from(nextAlertAt), incidentId);
    }

    @Override
    public void markReviewed(UUID incidentId, Instant reviewedAt) {
        jdbc.update("""
                UPDATE power_alert_escalations
                SET reviewed_at = COALESCE(reviewed_at, ?)
                WHERE incident_id = ? AND reviewed_at IS NULL
                """, Timestamp.from(reviewedAt), incidentId);
    }

    @Override
    public void markCompleted(UUID incidentId, Instant completedAt) {
        jdbc.update("""
                UPDATE power_alert_escalations
                SET completed_at = COALESCE(completed_at, ?)
                WHERE incident_id = ? AND completed_at IS NULL
                """, Timestamp.from(completedAt), incidentId);
    }

    private PowerAlertEscalation map(ResultSet rs, int rowNum) throws SQLException {
        return new PowerAlertEscalation(
                rs.getObject("incident_id", UUID.class),
                rs.getString("device_name"),
                rs.getTimestamp("start_time").toInstant(),
                rs.getInt("attempt_count"),
                rs.getTimestamp("next_alert_at").toInstant());
    }
}
