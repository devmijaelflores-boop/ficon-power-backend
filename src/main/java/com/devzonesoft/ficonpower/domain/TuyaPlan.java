package com.devzonesoft.ficonpower.domain;

import java.time.Instant;
import java.time.LocalDate;

public record TuyaPlan(
        long id,
        String projectName,
        String planName,
        LocalDate lastRenewedOn,
        LocalDate nextRenewalOn,
        int renewalIntervalMonths,
        int reminderDays,
        String notes,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
