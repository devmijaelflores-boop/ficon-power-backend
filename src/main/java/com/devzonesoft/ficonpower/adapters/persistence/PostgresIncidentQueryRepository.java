package com.devzonesoft.ficonpower.adapters.persistence;

import com.devzonesoft.ficonpower.domain.MonitoringViews.IncidentView;
import com.devzonesoft.ficonpower.domain.PagedResult;
import com.devzonesoft.ficonpower.ports.output.IncidentQueryRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresIncidentQueryRepository implements IncidentQueryRepository {

    private final JdbcTemplate jdbc;
    private final MonitoringViewRowMapper rows;

    public PostgresIncidentQueryRepository(JdbcTemplate jdbc, MonitoringViewRowMapper rows) {
        this.jdbc = jdbc;
        this.rows = rows;
    }

    @Override
    public List<IncidentView> findIncidents(int limit) {
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
                ORDER BY i.start_time DESC
                LIMIT ?
                """, rows::incident, limit);
    }

    @Override
    public PagedResult<IncidentView> findIncidentsPage(
            int page,
            int size,
            String search,
            String status,
            String deviceName,
            String siteName,
            String incidentId,
            String startDate,
            String endDate
    ) {
        IncidentFilters filters = new IncidentFilters();
        filters.likeAny(search, "d.name", "s.name", "i.status", "coalesce(i.notes, '')");
        filters.equalsIgnoreCase(status, "i.status");
        filters.equalsIgnoreCase(deviceName, "d.name");
        filters.equalsIgnoreCase(siteName, "s.name");
        filters.equalsUuid(incidentId, "i.id");
        filters.sameDate(startDate, "i.start_time");
        filters.sameDate(endDate, "i.end_time");

        long total = count(filters);
        List<Object> params = new ArrayList<>(filters.params());
        params.add(size);
        params.add((long) page * size);
        List<IncidentView> items = jdbc.query("""
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
                """ + filters.where() + """
                ORDER BY i.start_time DESC
                LIMIT ? OFFSET ?
                """, rows::incident, params.toArray());
        return PagedResult.of(items, page, size, total);
    }

    private long count(IncidentFilters filters) {
        Long total = jdbc.queryForObject("""
                SELECT count(*)
                FROM incidents i
                JOIN devices d ON d.id = i.device_id
                JOIN sites s ON s.id = COALESCE(i.site_id, d.site_id)
                LEFT JOIN technicians t ON t.id = i.acknowledged_by
                """ + filters.where(), Long.class, filters.params().toArray());
        return total == null ? 0 : total;
    }

    private static final class IncidentFilters {
        private final List<String> clauses = new ArrayList<>();
        private final List<Object> params = new ArrayList<>();

        void likeAny(String value, String... columns) {
            if (isBlank(value) || columns.length == 0) {
                return;
            }
            StringBuilder clause = new StringBuilder("(");
            for (int index = 0; index < columns.length; index++) {
                if (index > 0) {
                    clause.append(" OR ");
                }
                clause.append("lower(").append(columns[index]).append(") LIKE ?");
                params.add("%" + value.trim().toLowerCase() + "%");
            }
            clause.append(")");
            clauses.add(clause.toString());
        }

        void equalsIgnoreCase(String value, String column) {
            if (isBlank(value)) {
                return;
            }
            clauses.add("lower(" + column + ") = ?");
            params.add(value.trim().toLowerCase());
        }

        void sameDate(String value, String column) {
            if (isBlank(value)) {
                return;
            }
            clauses.add(column + "::date = ?::date");
            params.add(value.trim());
        }

        void equalsUuid(String value, String column) {
            if (isBlank(value)) {
                return;
            }
            clauses.add(column + " = ?::uuid");
            params.add(value.trim());
        }

        String where() {
            return clauses.isEmpty() ? "" : " WHERE " + String.join(" AND ", clauses) + "\n";
        }

        List<Object> params() {
            return params;
        }

        private static boolean isBlank(String value) {
            return value == null || value.trim().isEmpty();
        }
    }
}
