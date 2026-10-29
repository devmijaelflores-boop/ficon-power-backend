package com.devzonesoft.ficonpower.adapters.persistence;

import com.devzonesoft.ficonpower.domain.MonitoringViews.SiteView;
import com.devzonesoft.ficonpower.ports.output.SiteRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresSiteRepository implements SiteRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ZoneId timeZone;

    public PostgresSiteRepository(
            JdbcTemplate jdbcTemplate,
            @Value("${app.time-zone:America/Lima}") String timeZone
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.timeZone = ZoneId.of(timeZone);
    }

    @Override
    public SiteView create(String name, String district, String province) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO sites (name, district, province)
                VALUES (?, ?, ?)
                RETURNING id, name, address, district, province, department,
                          latitude, longitude, time_zone, is_active,
                          0 AS device_count, created_at, updated_at
                """, this::mapSite, name, nullable(district), nullable(province));
    }

    private SiteView mapSite(ResultSet rs, int rowNum) throws SQLException {
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
                peruTime(rs, "created_at"),
                peruTime(rs, "updated_at")
        );
    }

    private OffsetDateTime peruTime(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null
                ? null
                : timestamp.toInstant().atZone(timeZone).toOffsetDateTime();
    }

    private String nullable(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
