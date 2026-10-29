package com.devzonesoft.ficonpower.domain;

import java.time.Instant;
import java.util.UUID;

public record WhatsAppRecipient(
        UUID id,
        UUID userId,
        String label,
        String phone,
        UUID siteId,
        String siteName,
        boolean notifyOutage,
        boolean notifyRestored,
        boolean active,
        String lastTestStatus,
        String lastTestMessage,
        Instant lastTestAt,
        Instant createdAt,
        Instant updatedAt
) {
    public WhatsAppRecipient(
            UUID id,
            String label,
            String phone,
            UUID siteId,
            String siteName,
            boolean notifyOutage,
            boolean notifyRestored,
            boolean active,
            String lastTestStatus,
            String lastTestMessage,
            Instant lastTestAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(id, null, label, phone, siteId, siteName, notifyOutage, notifyRestored,
                active, lastTestStatus, lastTestMessage, lastTestAt, createdAt, updatedAt);
    }
}
