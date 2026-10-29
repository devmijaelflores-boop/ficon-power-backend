package com.devzonesoft.ficonpower.domain;

import java.time.Instant;
import java.util.UUID;

public sealed interface ConnectivityNotice
        permits ConnectivityNotice.Outage, ConnectivityNotice.Restoration {

    record Outage(UUID incidentId, String deviceName, Instant outageAt) implements ConnectivityNotice {
    }

    record Restoration(
            UUID incidentId,
            String deviceName,
            Instant outageAt,
            Instant restoredAt
    ) implements ConnectivityNotice {
    }
}
