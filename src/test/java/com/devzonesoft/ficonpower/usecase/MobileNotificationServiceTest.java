package com.devzonesoft.ficonpower.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devzonesoft.ficonpower.domain.AuthenticatedUser;
import com.devzonesoft.ficonpower.domain.MobileNotification;
import com.devzonesoft.ficonpower.domain.NotificationDeliveryStatus;
import com.devzonesoft.ficonpower.domain.UserRole;
import com.devzonesoft.ficonpower.ports.output.NotificationDeliveryRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MobileNotificationServiceTest {

    private final NotificationDeliveryRepository deliveries =
            mock(NotificationDeliveryRepository.class);
    private final PowerAlertEscalationService escalations =
            mock(PowerAlertEscalationService.class);
    private final MobileNotificationService service =
            new MobileNotificationService(deliveries, escalations);
    private final AuthenticatedUser user = new AuthenticatedUser(
            UUID.randomUUID(), "technician", UserRole.USER, "Técnico");

    @Test
    void scopesHistoryAndUnreadCountToTheAuthenticatedUser() {
        MobileNotification notification = new MobileNotification(
                UUID.randomUUID(),
                "Corte de energía",
                "Medidor sin conexión",
                Map.of("deviceName", "Medidor"),
                "power_outage",
                NotificationDeliveryStatus.SENT,
                Instant.now(),
                null);
        when(deliveries.findByUser(user.id(), 50)).thenReturn(List.of(notification));
        when(deliveries.countUnread(user.id())).thenReturn(1L);

        assertEquals(List.of(notification), service.findHistory(user, 50));
        assertEquals(1L, service.countUnread(user));
    }

    @Test
    void marksIndividualAndAllNotificationsForTheAuthenticatedUser() {
        UUID notificationId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        when(deliveries.markRead(user.id(), notificationId)).thenReturn(true);
        when(deliveries.findIncidentIdsByNotificationKey(user.id(), notificationId))
                .thenReturn(List.of(incidentId));
        when(deliveries.markAllRead(user.id())).thenReturn(3);
        when(deliveries.findIncidentIdsByUser(user.id())).thenReturn(List.of(incidentId));

        assertTrue(service.markRead(user, notificationId));
        assertEquals(3, service.markAllRead(user));

        verify(deliveries).markRead(user.id(), notificationId);
        verify(deliveries).markAllRead(user.id());
        verify(escalations, org.mockito.Mockito.times(2)).markReviewed(incidentId);
    }
}
