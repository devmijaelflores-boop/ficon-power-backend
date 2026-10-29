package com.devzonesoft.ficonpower.adapters.persistence;

import com.devzonesoft.ficonpower.domain.Device;
import com.devzonesoft.ficonpower.domain.DeviceStatus;
import com.devzonesoft.ficonpower.ports.output.DeviceRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresDeviceRepository implements DeviceRepository {

    private final JdbcTemplate jdbcTemplate;

    public PostgresDeviceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<Device> findByDeviceId(String deviceId) {
        List<Device> devices = jdbcTemplate.query("""
                SELECT d.id, d.external_id, d.name, d.site_id, s.name AS site,
                       d.provider, d.category, d.product_id, d.model, d.is_active,
                       d.status, d.last_status_change, d.created_at, d.updated_at
                FROM devices d
                JOIN sites s ON s.id = d.site_id
                WHERE d.external_id = ?
                """, this::mapDevice, deviceId);
        return devices.stream().findFirst();
    }

    @Override
    public Optional<Device> findByDeviceIdForUpdate(String deviceId) {
        List<Device> devices = jdbcTemplate.query("""
                SELECT d.id, d.external_id, d.name, d.site_id, s.name AS site,
                       d.provider, d.category, d.product_id, d.model, d.is_active,
                       d.status, d.last_status_change, d.created_at, d.updated_at
                FROM devices d
                JOIN sites s ON s.id = d.site_id
                WHERE d.external_id = ?
                FOR UPDATE OF d
                """, this::mapDevice, deviceId);
        return devices.stream().findFirst();
    }

    @Override
    public Device upsert(
            String deviceId,
            String name,
            DeviceStatus status,
            String category,
            String productId,
            String model
    ) {
        return jdbcTemplate.queryForObject("""
                WITH default_site AS (
                    INSERT INTO sites (name)
                    VALUES ('Sin sede asignada')
                    ON CONFLICT ((lower(name))) DO UPDATE
                    SET name = EXCLUDED.name,
                        is_active = true,
                        updated_at = now()
                    RETURNING id
                )
                INSERT INTO devices (
                    external_id, name, site_id, status, last_status_change,
                    category, product_id, model
                )
                SELECT ?, ?, id, ?, now(), ?, ?, ?
                FROM default_site
                ON CONFLICT (external_id) DO UPDATE
                SET name = CASE
                        WHEN devices.name_customized THEN devices.name
                        ELSE EXCLUDED.name
                    END,
                    category = COALESCE(EXCLUDED.category, devices.category),
                    product_id = COALESCE(EXCLUDED.product_id, devices.product_id),
                    model = COALESCE(EXCLUDED.model, devices.model),
                    status = EXCLUDED.status,
                    last_status_change = CASE
                        WHEN devices.status IS DISTINCT FROM EXCLUDED.status THEN now()
                        ELSE devices.last_status_change
                    END,
                    is_active = true
                RETURNING id, external_id, name, site_id,
                          (SELECT name FROM sites WHERE id = devices.site_id) AS site,
                          provider, category, product_id, model, is_active,
                          status, last_status_change, created_at, updated_at
                """, this::mapDevice, deviceId, name, status.databaseValue(),
                nullable(category), nullable(productId), nullable(model));
    }

    @Override
    public Optional<Device> updateDetails(UUID id, String name, UUID siteId) {
        List<Device> devices = jdbcTemplate.query("""
                UPDATE devices
                SET name = ?, site_id = ?, name_customized = true, updated_at = now()
                WHERE id = ?
                RETURNING id, external_id, name, site_id,
                          (SELECT name FROM sites WHERE id = devices.site_id) AS site,
                          provider, category, product_id, model, is_active,
                          status, last_status_change, created_at, updated_at
                """, this::mapDevice, name, siteId, id);
        return devices.stream().findFirst();
    }

    @Override
    public void updateStatus(
            UUID id,
            DeviceStatus status,
            java.time.Instant occurredAt
    ) {
        jdbcTemplate.update("""
                UPDATE devices
                SET status = ?, last_status_change = ?
                WHERE id = ?
                """, status.databaseValue(), Timestamp.from(occurredAt), id);
    }

    @Override
    public boolean deactivateByDeviceId(String deviceId) {
        return jdbcTemplate.update("""
                UPDATE devices
                SET is_active = false,
                    status = 'offline',
                    last_status_change = CASE
                        WHEN status IS DISTINCT FROM 'offline' THEN now()
                        ELSE last_status_change
                    END,
                    updated_at = now()
                WHERE external_id = ?
                  AND is_active = true
                """, deviceId) > 0;
    }

    private Device mapDevice(ResultSet rs, int rowNum) throws SQLException {
        return new Device(
                rs.getObject("id", UUID.class),
                rs.getString("external_id"),
                rs.getString("name"),
                rs.getObject("site_id", UUID.class),
                rs.getString("site"),
                rs.getString("provider"),
                rs.getString("category"),
                rs.getString("product_id"),
                rs.getString("model"),
                rs.getBoolean("is_active"),
                DeviceStatus.fromDatabase(rs.getString("status")),
                rs.getTimestamp("last_status_change") == null
                        ? null : rs.getTimestamp("last_status_change").toInstant(),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
        );
    }

    private String nullable(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
