package com.devzonesoft.ficonpower.ports.input;

import com.devzonesoft.ficonpower.domain.DeviceStatus;
import java.time.Instant;
import tools.jackson.databind.JsonNode;

public interface HandleDeviceConnectivity {

    void handle(String deviceId, DeviceStatus status, Instant occurredAt, JsonNode payload);
}
