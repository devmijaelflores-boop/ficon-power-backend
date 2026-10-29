package com.devzonesoft.ficonpower.domain;

import java.time.Instant;
import tools.jackson.databind.JsonNode;

public record Event(
        Long id,
        String deviceId,
        String eventType,
        JsonNode payload,
        Instant occurredAt,
        Instant receivedAt
) {
}
