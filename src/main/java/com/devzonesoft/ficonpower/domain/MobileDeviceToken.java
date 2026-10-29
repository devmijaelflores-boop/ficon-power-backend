package com.devzonesoft.ficonpower.domain;

import java.time.Instant;
import java.util.UUID;

public record MobileDeviceToken(
        UUID id,
        UUID userId,
        String token,
        String platform,
        String deviceName,
        String appVersion,
        Instant lastSeenAt
) {
}
