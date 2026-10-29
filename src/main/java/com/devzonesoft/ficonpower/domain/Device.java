package com.devzonesoft.ficonpower.domain;

import java.time.Instant;
import java.util.UUID;

public record Device(
        UUID id,
        String deviceId,
        String name,
        UUID siteId,
        String site,
        String provider,
        String category,
        String productId,
        String model,
        boolean active,
        DeviceStatus status,
        Instant lastStatusChange,
        Instant createdAt,
        Instant updatedAt
) {
}
