package com.devzonesoft.ficonpower.ports.output;

import com.devzonesoft.ficonpower.domain.PowerAlertEscalation;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PowerAlertEscalationRepository {

    void start(UUID incidentId, Instant firstReminderAt);

    List<PowerAlertEscalation> findDue(Instant now, int limit);

    void markAttempt(UUID incidentId, int attemptCount, Instant sentAt, Instant nextAlertAt);

    void markReviewed(UUID incidentId, Instant reviewedAt);

    void markCompleted(UUID incidentId, Instant completedAt);
}
