package com.devzonesoft.ficonpower.adapters.persistence;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devzonesoft.ficonpower.domain.DevicePlacement;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class PostgresDeviceAssignmentRepositoryTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final PostgresDeviceAssignmentRepository repository =
            new PostgresDeviceAssignmentRepository(jdbc);

    @Test
    void createsAMissingKnownSiteAndStartsAPlacementPeriod() {
        UUID siteId = UUID.fromString("f536be13-e630-44cc-b8e8-a530a0f2ee08");
        UUID deviceId = UUID.fromString("06479d5f-cbc2-467c-b9de-a37df01db931");
        when(jdbc.query(
                argThat((String sql) -> sql != null && sql.contains("FROM sites")),
                any(RowMapper.class),
                eq("Lampa"), eq("Lampa"), eq("Lampa")))
                .thenReturn(List.of());
        when(jdbc.queryForObject(
                argThat((String sql) -> sql != null && sql.contains("INSERT INTO sites")),
                eq(UUID.class),
                eq("Lampa"), eq("Lampa"), eq("Lampa"),
                eq(new BigDecimal("-15.364430")),
                eq(new BigDecimal("-70.366620"))))
                .thenReturn(siteId);
        when(jdbc.queryForObject(
                argThat((String sql) -> sql != null && sql.contains("SELECT id FROM devices")),
                eq(UUID.class), eq("tuya-device")))
                .thenReturn(deviceId);
        when(jdbc.query(
                argThat((String sql) -> sql != null && sql.contains("FROM device_assignments")),
                any(RowMapper.class), eq(deviceId)))
                .thenReturn(List.of());

        repository.assign(
                "tuya-device",
                new DevicePlacement("TIMAX - Lampa", "Lampa", "timax_lampa"),
                "asset-1");

        verify(jdbc).update(
                argThat((String sql) -> sql != null
                        && sql.contains("INSERT INTO device_assignments")),
                eq(deviceId), eq(siteId), eq("asset-1"), eq("timax_lampa"),
                eq("TIMAX - Lampa"));
        verify(jdbc).update(
                argThat((String sql) -> sql != null && sql.contains("UPDATE devices")
                        && sql.contains("name = ?")
                        && sql.contains("name_customized = true")
                        && !sql.contains("CASE WHEN name_customized")),
                eq(siteId), eq("TIMAX - Lampa"), eq(deviceId));
    }

    @Test
    void doesNotCloseTheNewPlacementForAStaleUnbind() {
        UUID deviceId = UUID.fromString("06479d5f-cbc2-467c-b9de-a37df01db931");
        when(jdbc.query(
                argThat((String sql) -> sql != null && sql.contains("SELECT id FROM devices")),
                any(RowMapper.class), eq("tuya-device")))
                .thenReturn(List.of(deviceId));
        when(jdbc.query(
                argThat((String sql) -> sql != null
                        && sql.contains("SELECT tuya_container_id")),
                any(RowMapper.class), eq(deviceId)))
                .thenReturn(List.of("new-asset"));

        boolean closed = repository.closeActive("tuya-device", "old-asset");

        assertFalse(closed);
        verify(jdbc, never()).update(
                argThat((String sql) -> sql != null
                        && sql.contains("UPDATE device_assignments")),
                any(Object[].class));
    }
}
