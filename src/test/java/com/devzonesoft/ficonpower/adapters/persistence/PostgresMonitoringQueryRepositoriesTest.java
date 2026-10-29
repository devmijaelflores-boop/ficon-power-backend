package com.devzonesoft.ficonpower.adapters.persistence;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class PostgresMonitoringQueryRepositoriesTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final MonitoringViewRowMapper rows = mock(MonitoringViewRowMapper.class);

    @Test
    void siteQueriesRemainScopedToSites() {
        new PostgresSiteQueryRepository(jdbc, rows).findSites();

        verify(jdbc).query(
                argThat((String sql) -> sql.contains("FROM sites s")
                        && sql.contains("LEFT JOIN devices")
                        && sql.contains("d.is_active = true")),
                any(RowMapper.class));
    }

    @Test
    void deviceQueriesKeepEventAndIncidentStatistics() {
        new PostgresDeviceQueryRepository(jdbc, rows).findDevices();

        verify(jdbc).query(
                argThat((String sql) -> sql.contains("FROM devices d")
                        && sql.contains("event_stats")
                        && sql.contains("incident_stats")
                        && sql.contains("WHERE d.is_active = true")),
                any(RowMapper.class));
    }

    @Test
    void deviceDetailLooksUpTheExactDeviceBeforeCalculatingStatistics() {
        UUID deviceId = UUID.randomUUID();
        when(jdbc.query(anyString(), any(RowMapper.class), eq(deviceId)))
                .thenReturn(List.of());

        new PostgresDeviceQueryRepository(jdbc, rows).findDeviceDetail(deviceId);

        verify(jdbc).query(
                argThat((String sql) -> sql.contains("WHERE d.id = ?")
                        && sql.contains("d.is_active = true")
                        && sql.contains("incident_stats")),
                any(RowMapper.class),
                eq(deviceId));
    }

    @Test
    void deviceDetailContractIncludesRecentEvents() throws Exception {
        var components = com.devzonesoft.ficonpower.domain.MonitoringViews
                .DeviceDetailView.class.getRecordComponents();

        org.junit.jupiter.api.Assertions.assertEquals(
                "recentEvents", components[components.length - 1].getName());
    }

    @Test
    void incidentQueriesKeepTheRequestedLimit() {
        new PostgresIncidentQueryRepository(jdbc, rows).findIncidents(25);

        verify(jdbc).query(
                argThat((String sql) -> sql.contains("FROM incidents i")
                        && sql.contains("LIMIT ?")),
                any(RowMapper.class),
                eq(25));
    }

    @Test
    void incidentPagesCanFilterTheExactNotificationIncident() {
        String incidentId = "ff0e5db2-e7b1-4fda-95a8-f040e2c07301";
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of());

        new PostgresIncidentQueryRepository(jdbc, rows).findIncidentsPage(
                0, 10, "", "", "", "", incidentId, "", "");

        verify(jdbc).queryForObject(
                argThat((String sql) -> sql.contains("i.id = ?::uuid")),
                eq(Long.class),
                eq(incidentId));
    }

    @Test
    void eventQueriesKeepTheRequestedLimit() {
        new PostgresEventQueryRepository(jdbc, rows).findEvents(40);

        verify(jdbc).query(
                argThat((String sql) -> sql.contains("FROM events e")
                        && sql.contains("delivery_latency_ms")
                        && sql.contains("LIMIT ?")),
                any(RowMapper.class),
                eq(40));
    }
}
