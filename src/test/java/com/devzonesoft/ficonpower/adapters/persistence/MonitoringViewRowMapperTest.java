package com.devzonesoft.ficonpower.adapters.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.devzonesoft.ficonpower.domain.MonitoringViews.EventView;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class MonitoringViewRowMapperTest {

    @Test
    void mapsEventPayloadAndApplicationTimeZone() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        String id = "42";
        UUID deviceId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-08-13T15:00:00Z");
        Instant receivedAt = occurredAt.plusMillis(250);
        when(rs.getString("id")).thenReturn(id);
        when(rs.getString("event_type")).thenReturn("deviceOffline");
        when(rs.getObject("device_id", UUID.class)).thenReturn(deviceId);
        when(rs.getString("device_name")).thenReturn("Medidor principal");
        when(rs.getString("site_name")).thenReturn("Sede Lima");
        when(rs.getTimestamp("occurred_at")).thenReturn(Timestamp.from(occurredAt));
        when(rs.getTimestamp("received_at")).thenReturn(Timestamp.from(receivedAt));
        when(rs.getLong("delivery_latency_ms")).thenReturn(250L);
        when(rs.getString("payload")).thenReturn("{\"online\":false}");
        MonitoringViewRowMapper mapper = new MonitoringViewRowMapper(
                new ObjectMapper(), ZoneId.of("America/Lima"));

        EventView event = mapper.event(rs, 0);

        assertEquals(id, event.id());
        assertEquals(deviceId, event.deviceId());
        assertEquals("2026-08-13T10:00-05:00", event.occurredAt().toString());
        assertEquals(250L, event.deliveryLatencyMillis());
        assertEquals(false, event.payload().get("online").asBoolean());
    }

    @Test
    void mapsDeviceSiteCoordinates() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        UUID id = UUID.randomUUID();
        UUID siteId = UUID.randomUUID();
        when(rs.getObject("id", UUID.class)).thenReturn(id);
        when(rs.getObject("site_id", UUID.class)).thenReturn(siteId);
        when(rs.getString("name")).thenReturn("Medidor Puno");
        when(rs.getString("status")).thenReturn("online");
        when(rs.getBigDecimal("latitude")).thenReturn(new java.math.BigDecimal("-15.840222"));
        when(rs.getBigDecimal("longitude")).thenReturn(new java.math.BigDecimal("-70.021881"));
        MonitoringViewRowMapper mapper = new MonitoringViewRowMapper(
                new ObjectMapper(), ZoneId.of("America/Lima"));

        var device = mapper.device(rs, 0);

        assertEquals(new java.math.BigDecimal("-15.840222"), device.latitude());
        assertEquals(new java.math.BigDecimal("-70.021881"), device.longitude());
    }
}
