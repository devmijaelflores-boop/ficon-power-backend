package com.devzonesoft.ficonpower.usecase;

import com.devzonesoft.ficonpower.ports.output.IncidentRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class IncidentAttendanceService {

    private final IncidentRepository incidents;
    private final PowerAlertEscalationService escalations;
    private final Clock clock;

    public IncidentAttendanceService(
            IncidentRepository incidents,
            PowerAlertEscalationService escalations,
            Clock clock
    ) {
        this.incidents = incidents;
        this.escalations = escalations;
        this.clock = clock;
    }

    public boolean acknowledge(UUID incidentId) {
        return incidents.acknowledge(incidentId, Instant.now(clock))
                .map(incident -> {
                    escalations.markReviewed(incident.id());
                    return true;
                })
                .orElse(false);
    }
}
