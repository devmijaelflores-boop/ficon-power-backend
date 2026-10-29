package com.devzonesoft.ficonpower.adapters.tuya.pulsar;

import java.time.Instant;

public record TuyaConnectivityEvent(String deviceId, Status status, Instant occurredAt) {

    public enum Status {
        ONLINE,
        OFFLINE
    }
}
