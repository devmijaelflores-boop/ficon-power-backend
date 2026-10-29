package com.devzonesoft.ficonpower.ports.output;

import com.devzonesoft.ficonpower.domain.NotificationEventType;
import com.devzonesoft.ficonpower.domain.WhatsAppRecipient;
import com.devzonesoft.ficonpower.domain.UserWhatsAppDelivery;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WhatsAppRecipientRepository {

    List<WhatsAppRecipient> findAll();

    boolean hasAny();

    Optional<WhatsAppRecipient> findById(UUID id);

    Optional<WhatsAppRecipient> findByUserId(UUID userId);

    List<WhatsAppRecipient> findActiveForIncident(UUID incidentId, NotificationEventType eventType);

    WhatsAppRecipient create(String label, String phone, UUID siteId,
            boolean notifyOutage, boolean notifyRestored, boolean active);

    Optional<WhatsAppRecipient> update(UUID id, String label, String phone, UUID siteId,
            boolean notifyOutage, boolean notifyRestored, boolean active);

    WhatsAppRecipient upsertForUser(UUID userId, String label, String phone, boolean active);

    List<UserWhatsAppDelivery> findDeliveriesByUserId(UUID userId, int limit);

    boolean delete(UUID id);

    void recordTest(UUID id, boolean accepted, String message);
}
