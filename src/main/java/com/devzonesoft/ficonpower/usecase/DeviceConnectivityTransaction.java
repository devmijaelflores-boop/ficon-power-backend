package com.devzonesoft.ficonpower.usecase;

import com.devzonesoft.ficonpower.domain.ConnectivityNotice;
import com.devzonesoft.ficonpower.domain.Device;
import com.devzonesoft.ficonpower.domain.DeviceNotFoundException;
import com.devzonesoft.ficonpower.domain.DeviceStatus;
import com.devzonesoft.ficonpower.domain.Event;
import com.devzonesoft.ficonpower.domain.Incident;
import com.devzonesoft.ficonpower.ports.output.DeviceRepository;
import com.devzonesoft.ficonpower.ports.output.EventRepository;
import com.devzonesoft.ficonpower.ports.output.IncidentRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;
import tools.jackson.databind.JsonNode;

@Component
public class DeviceConnectivityTransaction {

    private final DeviceRepository devices;
    private final IncidentRepository incidents;
    private final EventRepository events;
    private final TransactionOperations transactions;
    private final Clock clock;

    public DeviceConnectivityTransaction(
            DeviceRepository devices,
            IncidentRepository incidents,
            EventRepository events,
            TransactionOperations transactions,
            Clock clock
    ) {
        this.devices = devices;
        this.incidents = incidents;
        this.events = events;
        this.transactions = transactions;
        this.clock = clock;
    }

    public Optional<ConnectivityNotice> apply(
            String deviceId,
            DeviceStatus status,
            Instant occurredAt,
            JsonNode payload
    ) {
        return transactions.execute(transaction -> {
            saveEvent(deviceId, status, occurredAt, payload);
            Device device = findForUpdate(deviceId);
            if (device.lastStatusChange() != null
                    && occurredAt.isBefore(device.lastStatusChange())) {
                return Optional.empty();
            }
            if (status == DeviceStatus.OFFLINE) {
                return applyOffline(device, occurredAt);
            }
            return applyOnline(device, occurredAt);
        });
    }

    private Optional<ConnectivityNotice> applyOffline(Device device, Instant occurredAt) {
        if (device.status() == DeviceStatus.OFFLINE) {
            return Optional.empty();
        }
        if (incidents.findActiveByDevice(device.id()).isPresent()) {
            devices.updateStatus(device.id(), DeviceStatus.OFFLINE, occurredAt);
            return Optional.empty();
        }

        Incident incident = incidents.create(Incident.activeFor(device.id(), occurredAt));
        devices.updateStatus(device.id(), DeviceStatus.OFFLINE, occurredAt);
        return Optional.of(new ConnectivityNotice.Outage(incident.id(), device.name(), occurredAt));
    }

    private Optional<ConnectivityNotice> applyOnline(Device device, Instant occurredAt) {
        devices.updateStatus(device.id(), DeviceStatus.ONLINE, occurredAt);
        return incidents.resolveOpenByDevice(device.id(), occurredAt)
                .map(incident -> new ConnectivityNotice.Restoration(
                        incident.id(), device.name(), incident.startTime(), occurredAt));
    }

    private void saveEvent(
            String deviceId,
            DeviceStatus status,
            Instant occurredAt,
            JsonNode payload
    ) {
        events.save(new Event(
                null,
                deviceId,
                status.databaseValue(),
                payload,
                occurredAt,
                Instant.now(clock)));
    }

    private Device findForUpdate(String deviceId) {
        return devices.findByDeviceIdForUpdate(deviceId)
                .orElseThrow(() -> new DeviceNotFoundException(deviceId));
    }
}
