package com.devzonesoft.ficonpower.adapters.persistence;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devzonesoft.ficonpower.domain.DeviceStatus;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class PostgresDeviceRepositoryTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final PostgresDeviceRepository repository = new PostgresDeviceRepository(jdbc);

    @Test
    void restoresDefaultSiteAtomicallyBeforeUpsertingDevice() {
        repository.upsert(
                "tuya-device",
                "Medidor",
                DeviceStatus.OFFLINE,
                "dlq",
                "product",
                "RPE1");

        verify(jdbc).queryForObject(
                argThat(sql -> sql != null
                        && sql.contains("WITH default_site AS")
                        && sql.contains("INSERT INTO sites (name)")
                        && sql.contains("ON CONFLICT ((lower(name))) DO UPDATE")
                        && sql.contains("FROM default_site")),
                any(org.springframework.jdbc.core.RowMapper.class),
                eq("tuya-device"),
                eq("Medidor"),
                eq("offline"),
                eq("dlq"),
                eq("product"),
                eq("RPE1"));
    }

    @Test
    void deactivatesAnUnboundDeviceWithoutDeletingIt() {
        when(jdbc.update(any(String.class), eq("tuya-device"))).thenReturn(1);

        boolean deactivated = repository.deactivateByDeviceId("tuya-device");

        org.junit.jupiter.api.Assertions.assertTrue(deactivated);
        verify(jdbc).update(
                argThat(sql -> sql != null
                        && sql.contains("UPDATE devices")
                        && sql.contains("is_active = false")
                        && !sql.contains("DELETE FROM")),
                eq("tuya-device"));
    }
}
