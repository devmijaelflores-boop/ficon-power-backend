package com.devzonesoft.ficonpower.adapters.persistence;

import com.devzonesoft.ficonpower.domain.DevicePlacement;
import com.devzonesoft.ficonpower.ports.output.DeviceAssignmentRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresDeviceAssignmentRepository implements DeviceAssignmentRepository {

    private static final Map<String, SiteDefaults> SITE_DEFAULTS = Map.of(
            "Puno", new SiteDefaults("Puno", "Puno", "-15.811540", "-70.032300"),
            "Juliaca", new SiteDefaults(
                    "Juliaca", "San Román", "-15.499687", "-70.129653"),
            "Putina", new SiteDefaults(
                    "Putina", "San Antonio de Putina", "-14.914100", "-69.868480"),
            "Lampa", new SiteDefaults("Lampa", "Lampa", "-15.364430", "-70.366620"));

    private final JdbcTemplate jdbc;

    public PostgresDeviceAssignmentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void assign(
            String externalDeviceId,
            DevicePlacement placement,
            String tuyaContainerId
    ) {
        UUID siteId = findOrCreateSite(placement.district());
        UUID deviceId = jdbc.queryForObject(
                "SELECT id FROM devices WHERE external_id = ? FOR UPDATE",
                UUID.class,
                externalDeviceId);

        List<ActiveAssignment> active = jdbc.query("""
                SELECT id, site_id, tuya_container_id
                FROM device_assignments
                WHERE device_id = ? AND active = true
                FOR UPDATE
                """, (rs, rowNum) -> new ActiveAssignment(
                    rs.getLong("id"),
                    rs.getObject("site_id", UUID.class),
                    rs.getString("tuya_container_id")), deviceId);

        boolean samePlacement = active.stream().anyMatch(current ->
                current.siteId().equals(siteId)
                        && java.util.Objects.equals(current.containerId(), tuyaContainerId));
        if (!samePlacement) {
            jdbc.update("""
                    UPDATE device_assignments
                    SET active = false, unassigned_at = now()
                    WHERE device_id = ? AND active = true
                    """, deviceId);
            jdbc.update("""
                    INSERT INTO device_assignments (
                        device_id, site_id, tuya_container_id, source_name,
                        canonical_name, assigned_at, active
                    ) VALUES (?, ?, ?, ?, ?, now(), true)
                    """, deviceId, siteId, tuyaContainerId, placement.sourceName(),
                    placement.deviceName());
        } else {
            jdbc.update("""
                    UPDATE device_assignments
                    SET source_name = ?, canonical_name = ?
                    WHERE device_id = ? AND active = true
                    """, placement.sourceName(), placement.deviceName(), deviceId);
        }

        jdbc.update("""
                UPDATE devices
                SET site_id = ?,
                    name = ?,
                    name_customized = true,
                    is_active = true,
                    updated_at = now()
                WHERE id = ?
                """, siteId, placement.deviceName(), deviceId);
    }

    @Override
    public boolean closeActive(String externalDeviceId, String tuyaContainerId) {
        List<UUID> devices = jdbc.query(
                "SELECT id FROM devices WHERE external_id = ? FOR UPDATE",
                (rs, rowNum) -> rs.getObject("id", UUID.class),
                externalDeviceId);
        if (devices.isEmpty()) {
            return true;
        }
        UUID deviceId = devices.getFirst();
        List<String> activeContainers = jdbc.query("""
                SELECT tuya_container_id
                FROM device_assignments
                WHERE device_id = ? AND active = true
                FOR UPDATE
                """, (rs, rowNum) -> rs.getString("tuya_container_id"), deviceId);
        boolean belongsToAnotherContainer = tuyaContainerId != null
                && !tuyaContainerId.isBlank()
                && activeContainers.stream().anyMatch(current -> current != null
                        && !current.equals(tuyaContainerId));
        if (belongsToAnotherContainer) {
            return false;
        }
        jdbc.update("""
                UPDATE device_assignments
                SET active = false, unassigned_at = now()
                WHERE device_id = ? AND active = true
                """, deviceId);
        return true;
    }

    private UUID findOrCreateSite(String district) {
        List<UUID> sites = jdbc.query("""
                SELECT id
                FROM sites
                WHERE lower(btrim(COALESCE(NULLIF(btrim(district), ''), name)))
                          = lower(btrim(?))
                   OR regexp_replace(lower(btrim(name)), '\\s*[_-]+\\s*', '-', 'g')
                          LIKE '%-' || lower(btrim(?))
                ORDER BY CASE
                             WHEN lower(btrim(district)) = lower(btrim(?)) THEN 0
                             ELSE 1
                         END,
                         is_active DESC,
                         name
                LIMIT 1
                """, (rs, rowNum) -> rs.getObject("id", UUID.class),
                district, district, district);
        if (!sites.isEmpty()) {
            UUID siteId = sites.getFirst();
            SiteDefaults defaults = defaultsFor(district);
            jdbc.update("""
                    UPDATE sites
                    SET district = ?, province = ?, department = 'Puno',
                        latitude = COALESCE(latitude, ?),
                        longitude = COALESCE(longitude, ?),
                        is_active = true, updated_at = now()
                    WHERE id = ?
                    """, district, defaults.province(), defaults.latitude(),
                    defaults.longitude(), siteId);
            return siteId;
        }

        SiteDefaults defaults = defaultsFor(district);
        return jdbc.queryForObject("""
                INSERT INTO sites (
                    name, district, province, department, latitude, longitude
                ) VALUES (?, ?, ?, 'Puno', ?, ?)
                ON CONFLICT ((lower(name))) DO UPDATE
                SET district = EXCLUDED.district,
                    province = EXCLUDED.province,
                    department = EXCLUDED.department,
                    latitude = COALESCE(sites.latitude, EXCLUDED.latitude),
                    longitude = COALESCE(sites.longitude, EXCLUDED.longitude),
                    is_active = true,
                    updated_at = now()
                RETURNING id
                """, UUID.class, defaults.name(), district, defaults.province(),
                defaults.latitude(), defaults.longitude());
    }

    private SiteDefaults defaultsFor(String district) {
        SiteDefaults defaults = SITE_DEFAULTS.get(district);
        if (defaults == null) {
            throw new IllegalArgumentException("Distrito Tuya no soportado: " + district);
        }
        return defaults;
    }

    private record ActiveAssignment(long id, UUID siteId, String containerId) {
    }

    private record SiteDefaults(
            String name,
            String province,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        private SiteDefaults(String name, String province, String latitude, String longitude) {
            this(name, province, new BigDecimal(latitude), new BigDecimal(longitude));
        }
    }
}
