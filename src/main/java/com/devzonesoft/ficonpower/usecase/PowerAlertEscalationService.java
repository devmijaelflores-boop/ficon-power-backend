package com.devzonesoft.ficonpower.usecase;

import com.devzonesoft.ficonpower.config.PowerAlertEscalationProperties;
import com.devzonesoft.ficonpower.domain.PowerAlertEscalation;
import com.devzonesoft.ficonpower.ports.output.NotificationService;
import com.devzonesoft.ficonpower.ports.output.PowerAlertEscalationRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PowerAlertEscalationService {

    private final PowerAlertEscalationRepository escalations;
    private final NotificationService notifications;
    private final PowerAlertEscalationProperties properties;
    private final Clock clock;

    public PowerAlertEscalationService(
            PowerAlertEscalationRepository escalations,
            NotificationService notifications,
            PowerAlertEscalationProperties properties,
            Clock clock
    ) {
        this.escalations = escalations;
        this.notifications = notifications;
        this.properties = properties;
        this.clock = clock;
    }

    public void start(UUID incidentId, Instant outageAt) {
        if (!properties.isEnabled() || incidentId == null) {
            return;
        }
        escalations.start(incidentId, outageAt.plus(delayForAttempt(2)));
    }

    public void markReviewed(UUID incidentId) {
        if (incidentId != null) {
            escalations.markReviewed(incidentId, Instant.now(clock));
        }
    }

    public void markCompleted(UUID incidentId) {
        if (incidentId != null) {
            escalations.markCompleted(incidentId, Instant.now(clock));
        }
    }

    @Scheduled(fixedDelayString = "${app.notifications.power-alerts.poll-delay:15000}")
    public void dispatchDueAlerts() {
        if (!properties.isEnabled()) {
            return;
        }
        Instant now = Instant.now(clock);
        List<PowerAlertEscalation> due = escalations.findDue(now, properties.getBatchSize());
        due.forEach(this::dispatch);
    }

    private void dispatch(PowerAlertEscalation escalation) {
        int nextAttempt = escalation.attemptCount() + 1;
        Instant sentAt = Instant.now(clock);
        notifications.notifyPowerOutagePushOnly(
                escalation.incidentId(),
                escalation.deviceName(),
                escalation.outageAt());
        escalations.markAttempt(
                escalation.incidentId(),
                nextAttempt,
                sentAt,
                sentAt.plus(delayForAttempt(nextAttempt + 1)));
    }

    private Duration delayForAttempt(int attemptNumber) {
        List<Duration> delays = properties.getRetryDelays();
        int index = Math.max(0, Math.min(attemptNumber - 2, delays.size() - 1));
        return delays.get(index);
    }
}
