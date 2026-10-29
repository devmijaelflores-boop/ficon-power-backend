package com.devzonesoft.ficonpower.adapters.persistence;

import com.devzonesoft.ficonpower.domain.MonitoringViews.EventView;
import com.devzonesoft.ficonpower.domain.PagedResult;
import com.devzonesoft.ficonpower.ports.output.EventQueryRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresEventQueryRepository implements EventQueryRepository {

    private final JdbcTemplate jdbc;
    private final MonitoringViewRowMapper rows;

    public PostgresEventQueryRepository(JdbcTemplate jdbc, MonitoringViewRowMapper rows) {
        this.jdbc = jdbc;
        this.rows = rows;
    }

    @Override
    public List<EventView> findEvents(int limit) {
        return jdbc.query("""
                SELECT e.id, e.event_type, e.device_id, d.name AS device_name,
                       s.name AS site_name, e.occurred_at, e.received_at,
                       round(extract(epoch FROM (e.received_at - e.occurred_at)) * 1000)::bigint
                           AS delivery_latency_ms,
                       e.payload::text AS payload
                FROM events e
                JOIN devices d ON d.id = e.device_id
                JOIN sites s ON s.id = COALESCE(e.site_id, d.site_id)
                ORDER BY e.occurred_at DESC
                LIMIT ?
                """, rows::event, limit);
    }

    @Override
    public PagedResult<EventView> findEventsPage(
            int page,
            int size,
            String search,
            String eventType,
            String deviceName,
            String siteName,
            String occurredDate
    ) {
        EventFilters filters = new EventFilters();
        filters.likeAny(search, "e.event_type", "d.name", "s.name", "e.payload::text");
        filters.equalsIgnoreCase(eventType, "e.event_type");
        filters.equalsIgnoreCase(deviceName, "d.name");
        filters.equalsIgnoreCase(siteName, "s.name");
        filters.sameDate(occurredDate, "e.occurred_at");

        long total = count(filters);
        List<Object> params = new ArrayList<>(filters.params());
        params.add(size);
        params.add((long) page * size);
        List<EventView> items = jdbc.query("""
                SELECT e.id, e.event_type, e.device_id, d.name AS device_name,
                       s.name AS site_name, e.occurred_at, e.received_at,
                       round(extract(epoch FROM (e.received_at - e.occurred_at)) * 1000)::bigint
                           AS delivery_latency_ms,
                       e.payload::text AS payload
                FROM events e
                JOIN devices d ON d.id = e.device_id
                JOIN sites s ON s.id = COALESCE(e.site_id, d.site_id)
                """ + filters.where() + """
                ORDER BY e.occurred_at DESC
                LIMIT ? OFFSET ?
                """, rows::event, params.toArray());
        return PagedResult.of(items, page, size, total);
    }

    private long count(EventFilters filters) {
        Long total = jdbc.queryForObject("""
                SELECT count(*)
                FROM events e
                JOIN devices d ON d.id = e.device_id
                JOIN sites s ON s.id = COALESCE(e.site_id, d.site_id)
                """ + filters.where(), Long.class, filters.params().toArray());
        return total == null ? 0 : total;
    }

    private static final class EventFilters {
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
