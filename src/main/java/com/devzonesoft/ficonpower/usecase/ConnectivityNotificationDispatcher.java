package com.devzonesoft.ficonpower.usecase;

import com.devzonesoft.ficonpower.domain.ConnectivityNotice;
import com.devzonesoft.ficonpower.ports.output.NotificationService;
import org.springframework.stereotype.Component;

@Component
public class ConnectivityNotificationDispatcher {

    private final NotificationService notifications;
    private final PowerAlertEscalationService escalations;

    public ConnectivityNotificationDispatcher(
            NotificationService notifications,
            PowerAlertEscalationService escalations
    ) {
        this.notifications = notifications;
        this.escalations = escalations;
    }

    public void dispatch(ConnectivityNotice notice) {
        if (notice instanceof ConnectivityNotice.Outage outage) {
            notifications.notifyPowerOutage(outage.incidentId(), outage.deviceName(), outage.outageAt());
            escalations.start(outage.incidentId(), outage.outageAt());
            return;
        }
        ConnectivityNotice.Restoration restoration =
                (ConnectivityNotice.Restoration) notice;
        escalations.markCompleted(restoration.incidentId());
        notifications.notifyPowerRestored(
                restoration.incidentId(),
                restoration.deviceName(),
                restoration.outageAt(),
                restoration.restoredAt());
    }
}
