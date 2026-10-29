package com.devzonesoft.ficonpower.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record MobileNotification(
        UUID id,
        String title,
        String body,
        Map<String, String> data,
        String eventType,
        NotificationDeliveryStatus status,
        Instant createdAt,
        Instant readAt
) {
    public boolean unread() {
        return readAt == null;
    }
}
