package com.devzonesoft.ficonpower.usecase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devzonesoft.ficonpower.config.PowerAlertEscalationProperties;
import com.devzonesoft.ficonpower.domain.PowerAlertEscalation;
import com.devzonesoft.ficonpower.ports.output.NotificationService;
import com.devzonesoft.ficonpower.ports.output.PowerAlertEscalationRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PowerAlertEscalationServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-26T15:00:00Z");
    private static final UUID INCIDENT_ID =
            UUID.fromString("e1c0e9a2-d9f9-41de-9ad3-e91562ab9fb3");

    private final PowerAlertEscalationRepository escalations =
            mock(PowerAlertEscalationRepository.class);
    private final NotificationService notifications = mock(NotificationService.class);
    private final PowerAlertEscalationProperties properties = new PowerAlertEscalationProperties();
    private final PowerAlertEscalationService service = new PowerAlertEscalationService(
            escalations,
            notifications,
            properties,
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void schedulesFirstReminderAfterInitialOutageAlert() {
        service.start(INCIDENT_ID, NOW);

        verify(escalations).start(INCIDENT_ID, NOW.plusSeconds(30));
    }

    @Test
    void dispatchesDueAlertsAndSchedulesNextDelay() {
        properties.setRetryDelays(List.of(Duration.ofSeconds(30), Duration.ofMinutes(2)));
        PowerAlertEscalation due = new PowerAlertEscalation(
                INCIDENT_ID,
                "WIFI Energy Meter",
                NOW.minusSeconds(30),
                1,
                NOW);
        when(escalations.findDue(NOW, properties.getBatchSize())).thenReturn(List.of(due));

        service.dispatchDueAlerts();

        verify(notifications).notifyPowerOutagePushOnly(
                INCIDENT_ID,
                "WIFI Energy Meter",
                NOW.minusSeconds(30));
        verify(escalations).markAttempt(
                INCIDENT_ID,
                2,
                NOW,
                NOW.plus(Duration.ofMinutes(2)));
    }
}
