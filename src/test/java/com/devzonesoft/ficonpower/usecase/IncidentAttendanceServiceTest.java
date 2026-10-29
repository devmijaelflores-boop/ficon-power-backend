package com.devzonesoft.ficonpower.usecase;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devzonesoft.ficonpower.domain.Incident;
import com.devzonesoft.ficonpower.domain.IncidentStatus;
import com.devzonesoft.ficonpower.ports.output.IncidentRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class IncidentAttendanceServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-04T15:00:00Z");
    private static final UUID INCIDENT_ID =
            UUID.fromString("e1c0e9a2-d9f9-41de-9ad3-e91562ab9fb3");
    private static final UUID DEVICE_ID =
            UUID.fromString("b3324286-596c-42d4-9ebf-b3135a416f18");

    private final IncidentRepository incidents = Mockito.mock(IncidentRepository.class);
    private final PowerAlertEscalationService escalations =
            Mockito.mock(PowerAlertEscalationService.class);
    private final IncidentAttendanceService service = new IncidentAttendanceService(
            incidents,
            escalations,
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void marksAlertReviewedWhenActiveIncidentIsAcknowledged() {
        Incident acknowledged = new Incident(
                INCIDENT_ID,
                DEVICE_ID,
                NOW.minusSeconds(90),
                null,
                IncidentStatus.ACKNOWLEDGED,
                null,
                null,
                NOW.minusSeconds(90));
        when(incidents.acknowledge(INCIDENT_ID, NOW)).thenReturn(Optional.of(acknowledged));

        assertTrue(service.acknowledge(INCIDENT_ID));

        verify(escalations).markReviewed(INCIDENT_ID);
    }

    @Test
    void doesNotReviewAlertWhenIncidentWasAlreadyAcknowledged() {
        when(incidents.acknowledge(INCIDENT_ID, NOW)).thenReturn(Optional.empty());

        assertFalse(service.acknowledge(INCIDENT_ID));

        verify(escalations, never()).markReviewed(INCIDENT_ID);
    }
}
