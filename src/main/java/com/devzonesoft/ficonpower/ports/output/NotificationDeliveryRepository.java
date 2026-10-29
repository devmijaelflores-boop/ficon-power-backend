package com.devzonesoft.ficonpower.ports.output;

import com.devzonesoft.ficonpower.domain.NotificationDelivery;
import com.devzonesoft.ficonpower.domain.MobileNotification;
import java.util.List;
import java.util.UUID;

public interface NotificationDeliveryRepository {

    void save(NotificationDelivery delivery);

    List<MobileNotification> findByUser(UUID userId, int limit);

    long countUnread(UUID userId);

    boolean markRead(UUID userId, UUID notificationId);

    int markAllRead(UUID userId);

    List<UUID> findIncidentIdsByNotificationKey(UUID userId, UUID notificationId);

    List<UUID> findIncidentIdsByUser(UUID userId);
}
