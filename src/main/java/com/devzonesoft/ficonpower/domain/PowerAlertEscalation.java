package com.devzonesoft.ficonpower.domain;

import java.time.Instant;
import java.util.UUID;

public record PowerAlertEscalation(
        UUID incidentId,
        String deviceName,
        Instant outageAt,
        int attemptCount,
        Instant nextAlertAt
) {
}
