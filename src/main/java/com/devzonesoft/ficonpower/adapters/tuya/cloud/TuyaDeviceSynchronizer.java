package com.devzonesoft.ficonpower.adapters.tuya.cloud;

import com.devzonesoft.ficonpower.domain.Device;
import com.devzonesoft.ficonpower.domain.DeviceStatus;
import com.devzonesoft.ficonpower.ports.input.HandleDeviceConnectivity;
import com.devzonesoft.ficonpower.ports.output.DeviceRepository;
import com.devzonesoft.ficonpower.ports.output.DeviceAssignmentRepository;
import com.devzonesoft.ficonpower.adapters.tuya.pulsar.TuyaDeviceLifecycleEvent.ContainerType;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class TuyaDeviceSynchronizer {

    private static final Logger logger = LoggerFactory.getLogger(TuyaDeviceSynchronizer.class);

    private final TuyaCloudClient cloudClient;
    private final DeviceRepository deviceRepository;
    private final DeviceAssignmentRepository assignments;
    private final TuyaSpaceNameNormalizer spaceNames;
    private final HandleDeviceConnectivity handleDeviceConnectivity;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    public TuyaDeviceSynchronizer(
            TuyaCloudClient cloudClient,
            DeviceRepository deviceRepository,
            DeviceAssignmentRepository assignments,
            TuyaSpaceNameNormalizer spaceNames,
            HandleDeviceConnectivity handleDeviceConnectivity,
            Clock clock,
            ObjectMapper objectMapper
    ) {
        this.cloudClient = cloudClient;
        this.deviceRepository = deviceRepository;
        this.assignments = assignments;
        this.spaceNames = spaceNames;
        this.handleDeviceConnectivity = handleDeviceConnectivity;
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void synchronizeAtStartup() {
        try {
            List<TuyaCloudDevice> devices = cloudClient.listDevices();
            int synchronizedDevices = 0;
            for (TuyaCloudDevice device : devices) {
                try {
                    synchronize(device);
                    synchronizedDevices++;
                } catch (RuntimeException exception) {
                    logger.error(
                            "Could not synchronize Tuya device {}; continuing startup",
                            device.deviceId(),
                            exception);
                }
            }
            if (!devices.isEmpty()) {
                logger.info(
                        "Synchronized {}/{} Tuya device(s) with the database",
                        synchronizedDevices,
                        devices.size());
            }
        } catch (RuntimeException exception) {
            logger.error(
                    "Could not load Tuya devices during startup; backend remains active",
                    exception);
        }
    }

    public Device ensureRegistered(String deviceId) {
        return deviceRepository.findByDeviceId(deviceId).orElseGet(() -> cloudClient.listDevices().stream()
                    .filter(device -> device.deviceId().equals(deviceId))
                    .findFirst()
                    .map(this::upsert)
                    .orElseGet(() -> deviceRepository.upsert(
                            deviceId, "Dispositivo Tuya", DeviceStatus.ONLINE,
                            null, null, null)));
    }

    public Device registerFromBinding(String deviceId) {
        return registerFromBinding(deviceId, null, ContainerType.NONE);
    }

    @Transactional
    public Device registerFromBinding(
            String deviceId,
            String containerId,
            ContainerType containerType
    ) {
        Optional<Device> existing = deviceRepository.findByDeviceId(deviceId);
        Device device = cloudClient.listDevices().stream()
                .filter(cloudDevice -> cloudDevice.deviceId().equals(deviceId))
                .findFirst()
                .map(cloudDevice -> upsertFromBinding(cloudDevice, existing))
                .orElseGet(() -> deviceRepository.upsert(
                        deviceId, "Dispositivo Tuya",
                        existing.map(Device::status).orElse(DeviceStatus.ONLINE),
                        null, null, null));
        resolveContainerName(containerId, containerType)
                .flatMap(spaceNames::normalize)
                .ifPresentOrElse(
                        placement -> {
                            assignments.assign(deviceId, placement, containerId);
                            logger.info(
                                    "Assigned Tuya device {} to {} from {} {}",
                                    deviceId,
                                    placement.deviceName(),
                                    containerType,
                                    containerId);
                        },
                        () -> logger.warn(
                                "Could not infer a supported placement for Tuya device {} from {} {}",
                                deviceId,
                                containerType,
                                containerId));
        return deviceRepository.findByDeviceId(deviceId).orElse(device);
    }

    @Transactional
    public void deactivate(String deviceId) {
        deactivate(deviceId, null, ContainerType.NONE);
    }

    @Transactional
    public void deactivate(
            String deviceId,
            String containerId,
            ContainerType containerType
    ) {
        if (assignments.closeActive(deviceId, containerId)) {
            deviceRepository.deactivateByDeviceId(deviceId);
            logger.info("Deactivated Tuya device {} and closed its active placement", deviceId);
            return;
        }
        logger.info(
                "Ignored stale Tuya unbind for device {} from {} {}; current placement differs",
                deviceId,
                containerType,
                containerId);
    }

    private Optional<String> resolveContainerName(
            String containerId,
            ContainerType containerType
    ) {
        if (containerId == null || containerId.isBlank()) {
            return Optional.empty();
        }
        try {
            return switch (containerType) {
                case ASSET -> cloudClient.assetName(containerId);
                case SPACE -> cloudClient.spaceName(containerId);
                case NONE -> Optional.empty();
            };
        } catch (RuntimeException exception) {
            logger.warn(
                    "Could not resolve Tuya {} {} while binding a device",
                    containerType,
                    containerId,
                    exception);
            return Optional.empty();
        }
    }

    private Device synchronize(TuyaCloudDevice device) {
        DeviceStatus cloudStatus = statusOf(device);
        deviceRepository.findByDeviceId(device.deviceId())
                .filter(existing -> existing.status() != cloudStatus)
                .ifPresent(existing -> handleDeviceConnectivity.handle(
                        device.deviceId(),
                        cloudStatus,
                        Instant.now(clock),
                        cloudSyncPayload(device)));
        return upsert(device);
    }

    private Device upsert(TuyaCloudDevice device) {
        return deviceRepository.upsert(
                device.deviceId(),
                device.name(),
                statusOf(device),
                device.category(),
                device.productId(),
                device.model());
    }

    private Device upsertFromBinding(
            TuyaCloudDevice cloudDevice,
            Optional<Device> existing
    ) {
        DeviceStatus status = cloudDevice.online()
                ? DeviceStatus.ONLINE
                : existing.map(Device::status).orElse(DeviceStatus.OFFLINE);
        return deviceRepository.upsert(
                cloudDevice.deviceId(),
                cloudDevice.name(),
                status,
                cloudDevice.category(),
                cloudDevice.productId(),
                cloudDevice.model());
    }

    private DeviceStatus statusOf(TuyaCloudDevice device) {
        return device.online() ? DeviceStatus.ONLINE : DeviceStatus.OFFLINE;
    }

    private JsonNode cloudSyncPayload(TuyaCloudDevice device) {
        return objectMapper.createObjectNode()
                .put("source", "tuya-cloud-sync")
                .put("deviceId", device.deviceId())
                .put("online", device.online());
    }
}
