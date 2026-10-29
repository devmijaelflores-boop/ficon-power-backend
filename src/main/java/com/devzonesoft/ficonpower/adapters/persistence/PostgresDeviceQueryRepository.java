package com.devzonesoft.ficonpower.adapters.persistence;

import com.devzonesoft.ficonpower.domain.MonitoringViews.DeviceDetailMetrics;
import com.devzonesoft.ficonpower.domain.MonitoringViews.DeviceDetailView;
import com.devzonesoft.ficonpower.domain.MonitoringViews.DeviceView;
import com.devzonesoft.ficonpower.domain.MonitoringViews.EventView;
import com.devzonesoft.ficonpower.domain.MonitoringViews.FrequencyPointView;
import com.devzonesoft.ficonpower.domain.MonitoringViews.IncidentView;
import com.devzonesoft.ficonpower.ports.output.DeviceQueryRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresDeviceQueryRepository implements DeviceQueryRepository {

    private static final String DEVICE_SELECT = """
            SELECT d.id, d.external_id, d.name, d.provider, d.category,
                   d.product_id, d.model, d.is_active, d.status,
                   d.last_status_change, d.created_at, d.updated_at,
                   s.id AS site_id, s.name AS site_name, s.district,
                   s.province, s.department, s.latitude, s.longitude,
                   incident_stats.current_incident_started_at,
                   event_stats.event_count,
                   incident_stats.incident_count,
                   incident_stats.total_downtime_seconds
            FROM devices d
            JOIN sites s ON s.id = d.site_id
            LEFT JOIN LATERAL (
                SELECT count(*) AS event_count
                FROM events e
                WHERE e.device_id = d.id
            ) event_stats ON true
            LEFT JOIN LATERAL (
                SELECT count(*) AS incident_count,
                       COALESCE(sum(extract(epoch FROM
                           (COALESCE(i.end_time, now()) - i.start_time))), 0)::bigint
                           AS total_downtime_seconds,
                       max(i.start_time) FILTER (
                           WHERE i.status IN ('active', 'acknowledged')
                       ) AS current_incident_started_at
                FROM incidents i
                WHERE i.device_id = d.id
            ) incident_stats ON true
            """;

    private final JdbcTemplate jdbc;
    private final MonitoringViewRowMapper rows;

    public PostgresDeviceQueryRepository(JdbcTemplate jdbc, MonitoringViewRowMapper rows) {
        this.jdbc = jdbc;
        this.rows = rows;
    }

    @Override
    public List<DeviceView> findDevices() {
        return jdbc.query(
                DEVICE_SELECT + " WHERE d.is_active = true ORDER BY d.name",
                rows::device);
    }

    @Override
    public Optional<DeviceDetailView> findDeviceDetail(UUID id) {
        List<DeviceView> devices = jdbc.query(
                DEVICE_SELECT + " WHERE d.id = ? AND d.is_active = true",
                rows::device,
                id);
        if (devices.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new DeviceDetailView(
                devices.getFirst(),
                metrics(id),
                frequency(id, """
                        SELECT to_char(day_bucket, 'DD/MM') AS label, count(i.id) AS count
                        FROM generate_series(
                            current_date - 6, current_date, interval '1 day'
                        ) day_bucket
                        LEFT JOIN incidents i ON i.device_id = ?
                            AND i.start_time >= day_bucket
                            AND i.start_time < day_bucket + interval '1 day'
                        GROUP BY day_bucket
                        ORDER BY day_bucket
                        """),
                frequency(id, """
                        SELECT 'S ' || to_char(week_bucket, 'DD/MM') AS label,
                               count(i.id) AS count
                        FROM generate_series(
                            date_trunc('week', current_date) - interval '3 weeks',
                            date_trunc('week', current_date), interval '1 week'
                        ) week_bucket
                        LEFT JOIN incidents i ON i.device_id = ?
                            AND i.start_time >= week_bucket
                            AND i.start_time < week_bucket + interval '1 week'
                        GROUP BY week_bucket
                        ORDER BY week_bucket
                        """),
                frequency(id, """
                        SELECT to_char(month_bucket, 'MM/YYYY') AS label,
                               count(i.id) AS count
                        FROM generate_series(
                            date_trunc('month', current_date) - interval '5 months',
                            date_trunc('month', current_date), interval '1 month'
                        ) month_bucket
                        LEFT JOIN incidents i ON i.device_id = ?
                            AND i.start_time >= month_bucket
                            AND i.start_time < month_bucket + interval '1 month'
                        GROUP BY month_bucket
                        ORDER BY month_bucket
                        """),
                frequency(id, """
                        SELECT bucket.label, count(i.id) AS count
                        FROM (VALUES
                            (1, 'Lun'), (2, 'Mar'), (3, 'Mié'), (4, 'Jue'),
                            (5, 'Vie'), (6, 'Sáb'), (7, 'Dom')
                        ) bucket(day_number, label)
                        LEFT JOIN incidents i ON i.device_id = ?
                            AND extract(isodow FROM i.start_time) = bucket.day_number
                        GROUP BY bucket.day_number, bucket.label
                        ORDER BY bucket.day_number
                        """),
                frequency(id, """
                        SELECT bucket.label, count(i.id) AS count
                        FROM (VALUES
                            (0, '00-06', 0, 6),
                            (1, '06-12', 6, 12),
                            (2, '12-18', 12, 18),
                            (3, '18-24', 18, 24)
                        ) bucket(sort_order, label, start_hour, end_hour)
                        LEFT JOIN incidents i ON i.device_id = ?
                            AND extract(hour FROM i.start_time) >= bucket.start_hour
                            AND extract(hour FROM i.start_time) < bucket.end_hour
                        GROUP BY bucket.sort_order, bucket.label
                        ORDER BY bucket.sort_order
                        """),
                recentIncidents(id),
                recentEvents(id)));
    }

    private DeviceDetailMetrics metrics(UUID id) {
        return jdbc.queryForObject("""
                SELECT
                    count(i.id) FILTER (
                        WHERE i.start_time >= now() - interval '7 days'
                    ) AS incidents_last_7_days,
                    count(i.id) FILTER (
                        WHERE i.start_time >= now() - interval '30 days'
                    ) AS incidents_last_30_days,
                    count(i.id) FILTER (
                        WHERE i.start_time >= now() - interval '90 days'
                    ) AS incidents_last_90_days,
                    COALESCE(avg(extract(epoch FROM
                        (COALESCE(i.end_time, now()) - i.start_time))), 0)::bigint
                        AS average_downtime_seconds,
                    COALESCE(max(extract(epoch FROM
                        (COALESCE(i.end_time, now()) - i.start_time))), 0)::bigint
                        AS longest_downtime_seconds,
                    round(
                        greatest(0, 100 - (
                            COALESCE(sum(greatest(0, extract(epoch FROM (
                                least(COALESCE(i.end_time, now()), now())
                                - greatest(i.start_time, now() - interval '30 days')
                            )))) FILTER (
                                WHERE i.start_time < now()
                                  AND COALESCE(i.end_time, now()) > now() - interval '30 days'
                            ), 0) / 2592000.0 * 100
                        ))::numeric,
                        2
                    ) AS availability_last_30_days
                FROM devices d
                LEFT JOIN incidents i ON i.device_id = d.id
                WHERE d.id = ?
                GROUP BY d.id
                """, (rs, rowNum) -> new DeviceDetailMetrics(
                    rs.getLong("incidents_last_7_days"),
                    rs.getLong("incidents_last_30_days"),
                    rs.getLong("incidents_last_90_days"),
                    rs.getLong("average_downtime_seconds"),
                    rs.getLong("longest_downtime_seconds"),
                    defaultAvailability(rs.getBigDecimal("availability_last_30_days"))), id);
    }

    private List<FrequencyPointView> frequency(UUID id, String sql) {
        return jdbc.query(sql, (rs, rowNum) -> new FrequencyPointView(
                rs.getString("label"), rs.getLong("count")), id);
    }

    private List<IncidentView> recentIncidents(UUID id) {
        return jdbc.query("""
                SELECT i.id, i.status, i.device_id, d.name AS device_name,
                       s.name AS site_name, i.start_time, i.end_time,
                       extract(epoch FROM (COALESCE(i.end_time, now()) - i.start_time))::bigint
                           AS duration_seconds,
                       t.name AS acknowledged_by, i.acknowledged_at, i.notes,
                       i.created_at, i.updated_at
                FROM incidents i
                JOIN devices d ON d.id = i.device_id
                JOIN sites s ON s.id = COALESCE(i.site_id, d.site_id)
                LEFT JOIN technicians t ON t.id = i.acknowledged_by
                WHERE i.device_id = ?
                ORDER BY i.start_time DESC
                LIMIT 10
                """, rows::incident, id);
    }

    private List<EventView> recentEvents(UUID id) {
        return jdbc.query("""
                SELECT e.id, e.event_type, e.device_id, d.name AS device_name,
                       s.name AS site_name, e.occurred_at, e.received_at,
                       round(extract(epoch FROM (e.received_at - e.occurred_at)) * 1000)::bigint
                           AS delivery_latency_ms,
                       e.payload::text AS payload
                FROM events e
                JOIN devices d ON d.id = e.device_id
                JOIN sites s ON s.id = COALESCE(e.site_id, d.site_id)
                WHERE e.device_id = ?
                ORDER BY e.occurred_at DESC
                LIMIT 10
                """, rows::event, id);
    }

    private BigDecimal defaultAvailability(BigDecimal value) {
        return value == null ? BigDecimal.valueOf(100) : value;
    }
}
