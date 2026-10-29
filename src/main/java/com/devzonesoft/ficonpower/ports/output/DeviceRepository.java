package com.devzonesoft.ficonpower.ports.output;

import com.devzonesoft.ficonpower.domain.Device;
import com.devzonesoft.ficonpower.domain.DeviceStatus;
import java.util.Optional;
import java.time.Instant;
import java.util.UUID;

public interface DeviceRepository {

    Optional<Device> findByDeviceId(String deviceId);

    Optional<Device> findByDeviceIdForUpdate(String deviceId);

    Device upsert(
            String deviceId,
            String name,
            DeviceStatus status,
            String category,
            String productId,
            String model
    );

    void updateStatus(UUID id, DeviceStatus status, Instant occurredAt);

    boolean deactivateByDeviceId(String deviceId);

    Optional<Device> updateDetails(UUID id, String name, UUID siteId);
}
