package com.devzonesoft.ficonpower.adapters.persistence;

import com.devzonesoft.ficonpower.domain.Incident;
import com.devzonesoft.ficonpower.domain.IncidentStatus;
import com.devzonesoft.ficonpower.ports.output.IncidentRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresIncidentRepository implements IncidentRepository {

    private final JdbcTemplate jdbcTemplate;

    public PostgresIncidentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Incident create(Incident incident) {
        UUID id = jdbcTemplate.queryForObject("""
                INSERT INTO incidents (
                    device_id, assignment_id, site_id, start_time, status
                )
                SELECT d.id, a.id, d.site_id, ?, ?
                FROM devices d
                LEFT JOIN device_assignments a
                    ON a.device_id = d.id AND a.active = true
                WHERE d.id = ?
                RETURNING incidents.id
                """, UUID.class, Timestamp.from(incident.startTime()),
                incident.status().databaseValue(), incident.deviceId());
        return incident.withId(id);
    }

    @Override
    public Optional<Incident> findActiveByDevice(UUID deviceId) {
        List<Incident> incidents = jdbcTemplate.query("""
                SELECT id, device_id, start_time, end_time, status,
                       acknowledged_by, notes, created_at
                FROM incidents
                WHERE device_id = ? AND status IN ('active', 'acknowledged')
                LIMIT 1
                """, this::mapIncident, deviceId);
        return incidents.stream().findFirst();
    }

    @Override
    public Optional<Incident> acknowledge(UUID id, java.time.Instant acknowledgedAt) {
        List<Incident> incidents = jdbcTemplate.query("""
                UPDATE incidents
                SET status = 'acknowledged',
                    acknowledged_at = ?
                WHERE id = ? AND status = 'active'
                RETURNING id, device_id, start_time, end_time, status,
                          acknowledged_by, notes, created_at
                """, this::mapIncident, Timestamp.from(acknowledgedAt), id);
        return incidents.stream().findFirst();
    }

    @Override
    public Optional<Incident> resolveOpenByDevice(UUID deviceId, java.time.Instant endTime) {
        List<Incident> incidents = jdbcTemplate.query("""
                UPDATE incidents
                SET status = 'resolved', end_time = ?
                WHERE id = (
                    SELECT id FROM incidents
                    WHERE device_id = ? AND status IN ('active', 'acknowledged')
                    ORDER BY start_time DESC
                    LIMIT 1
                    FOR UPDATE
                )
                RETURNING id, device_id, start_time, end_time, status,
                          acknowledged_by, notes, created_at
                """, this::mapIncident, Timestamp.from(endTime), deviceId);
        return incidents.stream().findFirst();
    }

    private Incident mapIncident(ResultSet rs, int rowNum) throws SQLException {
        return new Incident(
                rs.getObject("id", UUID.class),
                rs.getObject("device_id", UUID.class),
                rs.getTimestamp("start_time").toInstant(),
                rs.getTimestamp("end_time") == null ? null : rs.getTimestamp("end_time").toInstant(),
                IncidentStatus.fromDatabase(rs.getString("status")),
                rs.getObject("acknowledged_by", UUID.class),
                rs.getString("notes"),
                rs.getTimestamp("created_at").toInstant()
        );
    }
}
