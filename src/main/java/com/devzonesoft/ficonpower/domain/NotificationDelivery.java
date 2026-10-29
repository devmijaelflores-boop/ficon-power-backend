package com.devzonesoft.ficonpower.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record NotificationDelivery(
        UUID notificationKey,
        UUID incidentId,
        Long eventId,
        UUID userId,
        NotificationChannelType channel,
        String provider,
        String recipient,
        NotificationEventType eventType,
        String templateName,
        NotificationDeliveryStatus status,
        String providerMessageId,
        String errorCode,
        String errorMessage,
        int attemptCount,
        Instant sentAt,
        String title,
        String body,
        Map<String, String> data
) {
    public static NotificationDelivery push(
            UUID notificationKey,
            UUID userId,
            String recipient,
            NotificationEventType eventType,
            NotificationDeliveryStatus status,
            String providerMessageId,
            String errorCode,
            String errorMessage,
            String title,
            String body,
            Map<String, String> data
    ) {
        return push(null, notificationKey, userId, recipient, eventType, status,
                providerMessageId, errorCode, errorMessage, title, body, data);
    }

    public static NotificationDelivery push(
            UUID incidentId,
            UUID notificationKey,
            UUID userId,
            String recipient,
            NotificationEventType eventType,
            NotificationDeliveryStatus status,
            String providerMessageId,
            String errorCode,
            String errorMessage,
            String title,
            String body,
            Map<String, String> data
    ) {
        return new NotificationDelivery(
                notificationKey,
                incidentId,
                null,
                userId,
                NotificationChannelType.PUSH,
                "FCM",
                recipient,
                eventType,
                null,
                status,
                providerMessageId,
                errorCode,
                errorMessage,
                1,
                status == NotificationDeliveryStatus.SENT ? Instant.now() : null,
                title,
                body,
                Map.copyOf(data));
    }

    public static NotificationDelivery whatsapp(
            UUID incidentId,
            UUID userId,
            String recipient,
            NotificationEventType eventType,
            String templateName,
            NotificationDeliveryStatus status,
            String errorMessage
    ) {
        return new NotificationDelivery(
                UUID.randomUUID(),
                incidentId,
                null,
                userId,
                NotificationChannelType.WHATSAPP,
                "META",
                recipient,
                eventType,
                templateName,
                status,
                null,
                null,
                errorMessage,
                1,
                status == NotificationDeliveryStatus.SENT ? Instant.now() : null,
                null,
                null,
                Map.of());
    }
}
