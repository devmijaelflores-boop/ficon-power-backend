package com.devzonesoft.ficonpower.ports.output;

import com.devzonesoft.ficonpower.domain.Incident;
import java.util.Optional;
import java.util.UUID;

public interface IncidentRepository {

    Incident create(Incident incident);

    Optional<Incident> findActiveByDevice(UUID deviceId);

    Optional<Incident> acknowledge(UUID id, java.time.Instant acknowledgedAt);

    Optional<Incident> resolveOpenByDevice(UUID deviceId, java.time.Instant endTime);
}
