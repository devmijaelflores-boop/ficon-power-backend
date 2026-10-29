package com.devzonesoft.ficonpower.usecase;

import com.devzonesoft.ficonpower.domain.AuthenticatedUser;
import com.devzonesoft.ficonpower.domain.MobileNotification;
import com.devzonesoft.ficonpower.ports.output.NotificationDeliveryRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class MobileNotificationService {

    private final NotificationDeliveryRepository deliveries;
    private final PowerAlertEscalationService escalations;

    public MobileNotificationService(
            NotificationDeliveryRepository deliveries,
            PowerAlertEscalationService escalations
    ) {
        this.deliveries = deliveries;
        this.escalations = escalations;
    }

    public List<MobileNotification> findHistory(AuthenticatedUser user, int limit) {
        return deliveries.findByUser(user.id(), limit);
    }

    public long countUnread(AuthenticatedUser user) {
        return deliveries.countUnread(user.id());
    }

    public boolean markRead(AuthenticatedUser user, UUID notificationId) {
        boolean updated = deliveries.markRead(user.id(), notificationId);
        if (updated) {
            deliveries.findIncidentIdsByNotificationKey(user.id(), notificationId)
                    .forEach(escalations::markReviewed);
        }
        return updated;
    }

    public int markAllRead(AuthenticatedUser user) {
        return deliveries.markAllRead(user.id());
    }
}
