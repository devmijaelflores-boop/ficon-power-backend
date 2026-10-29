package com.devzonesoft.ficonpower.usecase;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devzonesoft.ficonpower.domain.ConnectivityNotice;
import com.devzonesoft.ficonpower.domain.Device;
import com.devzonesoft.ficonpower.domain.DeviceStatus;
import com.devzonesoft.ficonpower.domain.Event;
import com.devzonesoft.ficonpower.domain.Incident;
import com.devzonesoft.ficonpower.ports.output.DeviceRepository;
import com.devzonesoft.ficonpower.ports.output.EventRepository;
import com.devzonesoft.ficonpower.ports.output.IncidentRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;
import tools.jackson.databind.ObjectMapper;

class DeviceConnectivityTransactionTest {

    private static final Instant OUTAGE_AT = Instant.parse("2026-07-22T12:00:00Z");
    private static final Instant RESTORED_AT = Instant.parse("2026-07-22T12:10:00Z");
    private static final UUID INTERNAL_ID =
            UUID.fromString("6f37fa27-a9b9-4c08-b69a-5f668c664040");
    private static final UUID INCIDENT_ID =
            UUID.fromString("9003059c-87da-4130-b1b1-3424538f8442");
    private static final UUID SITE_ID =
            UUID.fromString("2a5907b1-42bd-4a02-b1ab-b60255ab3b93");
    private static final String TUYA_ID = "tuya-device-1";

    private DeviceRepository devices;
    private IncidentRepository incidents;
    private EventRepository events;
    private DeviceConnectivityTransaction transaction;

    @BeforeEach
    void setUp() {
        devices = mock(DeviceRepository.class);
        incidents = mock(IncidentRepository.class);
        events = mock(EventRepository.class);
        TransactionOperations transactions = mock(TransactionOperations.class);
        when(transactions.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
        transaction = new DeviceConnectivityTransaction(
                devices,
                incidents,
                events,
                transactions,
                Clock.fixed(OUTAGE_AT, ZoneOffset.UTC));
    }

    @Test
    void createsAnIncidentAndOutageNotice() throws Exception {
        when(devices.findByDeviceIdForUpdate(TUYA_ID))
                .thenReturn(Optional.of(device(DeviceStatus.ONLINE)));
        when(incidents.findActiveByDevice(INTERNAL_ID)).thenReturn(Optional.empty());
        when(incidents.create(any(Incident.class)))
                .thenReturn(Incident.activeFor(INTERNAL_ID, OUTAGE_AT).withId(INCIDENT_ID));
        var payload = new ObjectMapper().readTree("{}");

        Optional<ConnectivityNotice> notice = transaction.apply(
                TUYA_ID, DeviceStatus.OFFLINE, OUTAGE_AT, payload);

        ConnectivityNotice.Outage outage = assertInstanceOf(
                ConnectivityNotice.Outage.class, notice.orElseThrow());
        org.junit.jupiter.api.Assertions.assertEquals(INCIDENT_ID, outage.incidentId());
        verify(events).save(any(Event.class));
        verify(incidents).create(any(Incident.class));
        verify(devices).updateStatus(INTERNAL_ID, DeviceStatus.OFFLINE, OUTAGE_AT);
    }

    @Test
    void doesNotCreateAnotherIncidentForAnOfflineDevice() throws Exception {
        when(devices.findByDeviceIdForUpdate(TUYA_ID))
                .thenReturn(Optional.of(device(DeviceStatus.OFFLINE)));

        Optional<ConnectivityNotice> notice = transaction.apply(
                TUYA_ID,
                DeviceStatus.OFFLINE,
                OUTAGE_AT,
                new ObjectMapper().readTree("{}"));

        assertTrue(notice.isEmpty());
        verify(incidents, never()).create(any());
    }

    @Test
    void resolvesAnIncidentAndProducesARestorationNotice() throws Exception {
        Incident incident = Incident.activeFor(INTERNAL_ID, OUTAGE_AT).withId(INCIDENT_ID);
        when(devices.findByDeviceIdForUpdate(TUYA_ID))
                .thenReturn(Optional.of(device(DeviceStatus.OFFLINE)));
        when(incidents.resolveOpenByDevice(INTERNAL_ID, RESTORED_AT))
                .thenReturn(Optional.of(incident));

        Optional<ConnectivityNotice> notice = transaction.apply(
                TUYA_ID,
                DeviceStatus.ONLINE,
                RESTORED_AT,
                new ObjectMapper().readTree("{}"));

        ConnectivityNotice.Restoration restoration = assertInstanceOf(
                ConnectivityNotice.Restoration.class, notice.orElseThrow());
        org.junit.jupiter.api.Assertions.assertEquals(INCIDENT_ID, restoration.incidentId());
        org.junit.jupiter.api.Assertions.assertEquals(OUTAGE_AT, restoration.outageAt());
        verify(devices).updateStatus(INTERNAL_ID, DeviceStatus.ONLINE, RESTORED_AT);
    }

    @Test
    void recordsButDoesNotApplyAnOlderConnectivityEvent() throws Exception {
        when(devices.findByDeviceIdForUpdate(TUYA_ID))
                .thenReturn(Optional.of(device(
                        DeviceStatus.ONLINE,
                        RESTORED_AT)));

        Optional<ConnectivityNotice> notice = transaction.apply(
                TUYA_ID,
                DeviceStatus.OFFLINE,
                OUTAGE_AT,
                new ObjectMapper().readTree("{}"));

        assertTrue(notice.isEmpty());
        verify(events).save(any(Event.class));
        verify(devices, never()).updateStatus(any(), any(), any());
        verify(incidents, never()).create(any());
    }

    private Device device(DeviceStatus status) {
        return device(status, OUTAGE_AT.minusSeconds(60));
    }

    private Device device(DeviceStatus status, Instant lastStatusChange) {
        return new Device(
                INTERNAL_ID,
                TUYA_ID,
                "WIFI Energy Meter",
                SITE_ID,
                "Sede Lima",
                "tuya",
                "dlq",
                "product-1",
                "Energy Meter",
                true,
                status,
                lastStatusChange,
                OUTAGE_AT.minusSeconds(3600),
                OUTAGE_AT.minusSeconds(60));
    }
}
