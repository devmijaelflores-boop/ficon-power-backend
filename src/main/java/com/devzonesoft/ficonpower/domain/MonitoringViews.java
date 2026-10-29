package com.devzonesoft.ficonpower.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

public final class MonitoringViews {

    private MonitoringViews() {
    }

    public record SiteView(
            UUID id,
            String name,
            String address,
            String district,
            String province,
            String department,
            BigDecimal latitude,
            BigDecimal longitude,
            String timeZone,
            boolean active,
            long deviceCount,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
    }

    public record DeviceView(
            UUID id,
            String externalId,
            String name,
            String provider,
            String category,
            String productId,
            String model,
            boolean active,
            String status,
            OffsetDateTime lastStatusChange,
            UUID siteId,
            String siteName,
            String district,
            String province,
            String department,
            BigDecimal latitude,
            BigDecimal longitude,
            OffsetDateTime currentIncidentStartedAt,
            long eventCount,
            long incidentCount,
            long totalDowntimeSeconds,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
    }

    public record DeviceDetailView(
            DeviceView device,
            DeviceDetailMetrics metrics,
            List<FrequencyPointView> dailyIncidents,
            List<FrequencyPointView> weeklyIncidents,
            List<FrequencyPointView> monthlyIncidents,
            List<FrequencyPointView> weekdayIncidents,
            List<FrequencyPointView> timeOfDayIncidents,
            List<IncidentView> recentIncidents,
            List<EventView> recentEvents
    ) {
    }

    public record DeviceDetailMetrics(
            long incidentsLast7Days,
            long incidentsLast30Days,
            long incidentsLast90Days,
            long averageDowntimeSeconds,
            long longestDowntimeSeconds,
            BigDecimal availabilityLast30Days
    ) {
    }

    public record FrequencyPointView(String label, long count) {
    }

    public record IncidentView(
            UUID id,
            String status,
            UUID deviceId,
            String deviceName,
            String siteName,
            OffsetDateTime startTime,
            OffsetDateTime endTime,
            Long durationSeconds,
            String acknowledgedBy,
            OffsetDateTime acknowledgedAt,
            String notes,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
    }

    public record EventView(
            String id,
            String eventType,
            UUID deviceId,
            String deviceName,
            String siteName,
            OffsetDateTime occurredAt,
            OffsetDateTime receivedAt,
            long deliveryLatencyMillis,
            JsonNode payload
    ) {
    }
}
