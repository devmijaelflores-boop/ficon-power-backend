package com.devzonesoft.ficonpower.ports.output;

import java.time.Instant;
import java.util.UUID;

public interface NotificationService {

    void notifyPowerOutage(String deviceName, Instant outageAt);

    default void notifyPowerOutage(UUID incidentId, String deviceName, Instant outageAt) {
        notifyPowerOutage(deviceName, outageAt);
    }

    default void notifyPowerOutagePushOnly(UUID incidentId, String deviceName, Instant outageAt) {
        notifyPowerOutage(incidentId, deviceName, outageAt);
    }

    void notifyPowerRestored(String deviceName, Instant outageAt, Instant restoredAt);

    default void notifyPowerRestored(
            UUID incidentId,
            String deviceName,
            Instant outageAt,
            Instant restoredAt
    ) {
        notifyPowerRestored(deviceName, outageAt, restoredAt);
    }
}
