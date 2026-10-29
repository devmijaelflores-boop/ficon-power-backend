package com.devzonesoft.ficonpower.adapters.persistence;

import com.devzonesoft.ficonpower.domain.MonitoringViews.DeviceView;
import com.devzonesoft.ficonpower.domain.MonitoringViews.EventView;
import com.devzonesoft.ficonpower.domain.MonitoringViews.IncidentView;
import com.devzonesoft.ficonpower.domain.MonitoringViews.SiteView;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
class MonitoringViewRowMapper {

    private final ObjectMapper objectMapper;
    private final ZoneId timeZone;

    MonitoringViewRowMapper(ObjectMapper objectMapper, ZoneId timeZone) {
        this.objectMapper = objectMapper;
        this.timeZone = timeZone;
    }

    SiteView site(ResultSet rs, int rowNum) throws SQLException {
        return new SiteView(
                rs.getObject("id", UUID.class),
                rs.getString("name"),
                rs.getString("address"),
                rs.getString("district"),
                rs.getString("province"),
                rs.getString("department"),
                rs.getBigDecimal("latitude"),
                rs.getBigDecimal("longitude"),
                rs.getString("time_zone"),
                rs.getBoolean("is_active"),
                rs.getLong("device_count"),
                localTime(rs, "created_at"),
                localTime(rs, "updated_at"));
    }

    DeviceView device(ResultSet rs, int rowNum) throws SQLException {
        return new DeviceView(
                rs.getObject("id", UUID.class),
                rs.getString("external_id"),
                rs.getString("name"),
                rs.getString("provider"),
                rs.getString("category"),
                rs.getString("product_id"),
                rs.getString("model"),
                rs.getBoolean("is_active"),
                rs.getString("status"),
                localTime(rs, "last_status_change"),
                rs.getObject("site_id", UUID.class),
                rs.getString("site_name"),
                rs.getString("district"),
                rs.getString("province"),
                rs.getString("department"),
                rs.getBigDecimal("latitude"),
                rs.getBigDecimal("longitude"),
                localTime(rs, "current_incident_started_at"),
                rs.getLong("event_count"),
                rs.getLong("incident_count"),
                rs.getLong("total_downtime_seconds"),
                localTime(rs, "created_at"),
                localTime(rs, "updated_at"));
    }

    IncidentView incident(ResultSet rs, int rowNum) throws SQLException {
        return new IncidentView(
                rs.getObject("id", UUID.class),
                rs.getString("status"),
                rs.getObject("device_id", UUID.class),
                rs.getString("device_name"),
                rs.getString("site_name"),
                localTime(rs, "start_time"),
                localTime(rs, "end_time"),
                rs.getObject("duration_seconds", Long.class),
                rs.getString("acknowledged_by"),
                localTime(rs, "acknowledged_at"),
                rs.getString("notes"),
                localTime(rs, "created_at"),
                localTime(rs, "updated_at"));
    }

    EventView event(ResultSet rs, int rowNum) throws SQLException {
        return new EventView(
                rs.getString("id"),
                rs.getString("event_type"),
                rs.getObject("device_id", UUID.class),
                rs.getString("device_name"),
                rs.getString("site_name"),
                localTime(rs, "occurred_at"),
                localTime(rs, "received_at"),
                rs.getLong("delivery_latency_ms"),
                objectMapper.readTree(rs.getString("payload")));
    }

    private OffsetDateTime localTime(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null
                ? null
                : timestamp.toInstant().atZone(timeZone).toOffsetDateTime();
    }
}
