package com.devzonesoft.ficonpower.domain;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record OperationsCalendarEntry(
        String id,
        String type,
        String title,
        LocalDate date,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        String status,
        UUID deviceId,
        String deviceName,
        UUID siteId,
        String siteName,
        Long tuyaPlanId,
        String description
) {
}
