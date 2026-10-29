package com.devzonesoft.ficonpower.domain;

import java.time.Instant;
import java.util.UUID;

public record Incident(
        UUID id,
        UUID deviceId,
        Instant startTime,
        Instant endTime,
        IncidentStatus status,
        UUID acknowledgedBy,
        String notes,
        Instant createdAt
) {

    public static Incident activeFor(UUID deviceId, Instant startTime) {
        return new Incident(null, deviceId, startTime, null, IncidentStatus.ACTIVE, null, null, null);
    }

    public Incident withId(UUID generatedId) {
        return new Incident(generatedId, deviceId, startTime, endTime, status,
                acknowledgedBy, notes, createdAt);
    }
}
