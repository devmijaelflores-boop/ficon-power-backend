package com.devzonesoft.ficonpower.usecase;

import com.devzonesoft.ficonpower.domain.DeviceStatus;
import com.devzonesoft.ficonpower.ports.input.HandleDeviceConnectivity;
import java.time.Instant;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

@Service
public class HandleDeviceConnectivityService implements HandleDeviceConnectivity {

    private final DeviceConnectivityTransaction transaction;
    private final ConnectivityNotificationDispatcher notifications;

    public HandleDeviceConnectivityService(
            DeviceConnectivityTransaction transaction,
            ConnectivityNotificationDispatcher notifications
    ) {
        this.transaction = transaction;
        this.notifications = notifications;
    }

    @Override
    public void handle(String deviceId, DeviceStatus status, Instant occurredAt, JsonNode payload) {
        Instant normalizedTime = Instant.ofEpochMilli(occurredAt.toEpochMilli());
        transaction.apply(deviceId, status, normalizedTime, payload)
                .ifPresent(notifications::dispatch);
    }
}
