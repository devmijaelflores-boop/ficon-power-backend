package com.devzonesoft.ficonpower.usecase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.devzonesoft.ficonpower.domain.ConnectivityNotice;
import com.devzonesoft.ficonpower.ports.output.NotificationService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConnectivityNotificationDispatcherTest {

    private final NotificationService notifications = mock(NotificationService.class);
    private final PowerAlertEscalationService escalations = mock(PowerAlertEscalationService.class);
    private final ConnectivityNotificationDispatcher dispatcher =
            new ConnectivityNotificationDispatcher(notifications, escalations);

    @Test
    void dispatchesOutageNotices() {
        UUID incidentId = UUID.randomUUID();
        Instant outageAt = Instant.parse("2026-07-22T12:00:00Z");

        dispatcher.dispatch(new ConnectivityNotice.Outage(incidentId, "Medidor", outageAt));

        verify(notifications).notifyPowerOutage(incidentId, "Medidor", outageAt);
        verify(escalations).start(incidentId, outageAt);
    }

    @Test
    void dispatchesRestorationNotices() {
        UUID incidentId = UUID.randomUUID();
        Instant outageAt = Instant.parse("2026-07-22T12:00:00Z");
        Instant restoredAt = Instant.parse("2026-07-22T12:10:00Z");

        dispatcher.dispatch(new ConnectivityNotice.Restoration(
                incidentId, "Medidor", outageAt, restoredAt));

        verify(escalations).markCompleted(incidentId);
        verify(notifications).notifyPowerRestored(incidentId, "Medidor", outageAt, restoredAt);
    }
}
