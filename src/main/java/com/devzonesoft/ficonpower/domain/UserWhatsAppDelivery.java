package com.devzonesoft.ficonpower.domain;

import java.time.Instant;

public record UserWhatsAppDelivery(
        long id,
        String eventType,
        String templateName,
        NotificationDeliveryStatus status,
        String errorMessage,
        Instant sentAt,
        Instant createdAt
) {
}
