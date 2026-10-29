package com.devzonesoft.ficonpower.adapters.tuya.cloud;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devzonesoft.ficonpower.domain.Device;
import com.devzonesoft.ficonpower.domain.DevicePlacement;
import com.devzonesoft.ficonpower.domain.DeviceStatus;
import com.devzonesoft.ficonpower.ports.input.HandleDeviceConnectivity;
import com.devzonesoft.ficonpower.ports.output.DeviceRepository;
import com.devzonesoft.ficonpower.ports.output.DeviceAssignmentRepository;
import com.devzonesoft.ficonpower.adapters.tuya.pulsar.TuyaDeviceLifecycleEvent.ContainerType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class TuyaDeviceSynchronizerTest {

    private static final Instant NOW = Instant.parse("2026-08-06T18:00:00Z");
    private static final UUID INTERNAL_ID = UUID.fromString("91c1720f-89fb-4945-a65a-3d8a94651702");
    private static final UUID SITE_ID = UUID.fromString("2a5907b1-42bd-4a02-b1ab-b60255ab3b93");
    private static final String TUYA_ID = "6596da38f03423bf9092af";

    private TuyaCloudClient cloudClient;
    private DeviceRepository deviceRepository;
    private DeviceAssignmentRepository assignments;
    private HandleDeviceConnectivity connectivity;
    private TuyaDeviceSynchronizer synchronizer;

    @BeforeEach
    void setUp() {
        cloudClient = mock(TuyaCloudClient.class);
        deviceRepository = mock(DeviceRepository.class);
        assignments = mock(DeviceAssignmentRepository.class);
        connectivity = mock(HandleDeviceConnectivity.class);
        synchronizer = new TuyaDeviceSynchronizer(
                cloudClient,
                deviceRepository,
                assignments,
                new TuyaSpaceNameNormalizer(),
                connectivity,
                Clock.fixed(NOW, ZoneOffset.UTC),
                new ObjectMapper());
    }

    @Test
    void resolvesLocalOfflineStateWhenTuyaCloudReportsDeviceOnline() {
        TuyaCloudDevice cloudDevice = cloudDevice(true);
        when(cloudClient.listDevices()).thenReturn(List.of(cloudDevice));
        when(deviceRepository.findByDeviceId(TUYA_ID))
                .thenReturn(Optional.of(device(DeviceStatus.OFFLINE)));

        synchronizer.synchronizeAtStartup();

        verify(connectivity).handle(
                eq(TUYA_ID),
                eq(DeviceStatus.ONLINE),
                eq(NOW),
                any(JsonNode.class));
        verify(deviceRepository).upsert(
                TUYA_ID,
                "WIFI Energy Meter Test",
                DeviceStatus.ONLINE,
                "dlq",
                "zblabrkwxcxezteq",
                "RPE1");
    }

    @Test
    void doesNotCreateConnectivityEventWhenLocalStatusAlreadyMatchesTuyaCloud() {
        TuyaCloudDevice cloudDevice = cloudDevice(false);
        when(cloudClient.listDevices()).thenReturn(List.of(cloudDevice));
        when(deviceRepository.findByDeviceId(TUYA_ID))
                .thenReturn(Optional.of(device(DeviceStatus.OFFLINE)));

        synchronizer.synchronizeAtStartup();

        verify(connectivity, never()).handle(any(), any(), any(), any());
        verify(deviceRepository).upsert(
                TUYA_ID,
                "WIFI Energy Meter Test",
                DeviceStatus.OFFLINE,
                "dlq",
                "zblabrkwxcxezteq",
                "RPE1");
    }

    @Test
    void keepsBackendStartupActiveWhenDeviceCatalogCannotBeLoaded() {
        when(cloudClient.listDevices()).thenThrow(new IllegalStateException("Tuya unavailable"));

        assertDoesNotThrow(synchronizer::synchronizeAtStartup);

        verify(deviceRepository, never()).upsert(any(), any(), any(), any(), any(), any());
    }

    @Test
    void continuesSynchronizingAfterOneDeviceFails() {
        TuyaCloudDevice first = cloudDevice(TUYA_ID, true);
        TuyaCloudDevice second = cloudDevice("second-device", false);
        when(cloudClient.listDevices()).thenReturn(List.of(first, second));
        when(deviceRepository.findByDeviceId(any())).thenReturn(Optional.empty());
        when(deviceRepository.upsert(
                eq(TUYA_ID), any(), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("Missing site"));

        assertDoesNotThrow(synchronizer::synchronizeAtStartup);

        verify(deviceRepository).upsert(
                "second-device",
                "WIFI Energy Meter Test",
                DeviceStatus.OFFLINE,
                "dlq",
                "zblabrkwxcxezteq",
                "RPE1");
    }

    @Test
    void registersBoundDeviceFromTheCloudCatalog() {
        TuyaCloudDevice cloudDevice = cloudDevice(true);
        when(cloudClient.listDevices()).thenReturn(List.of(cloudDevice));

        synchronizer.registerFromBinding(TUYA_ID);

        verify(deviceRepository).upsert(
                TUYA_ID,
                "WIFI Energy Meter Test",
                DeviceStatus.ONLINE,
                "dlq",
                "zblabrkwxcxezteq",
                "RPE1");
    }

    @Test
    void assignsABoundDeviceUsingTheNormalizedAssetName() {
        when(cloudClient.listDevices()).thenReturn(List.of(cloudDevice(true)));
        when(cloudClient.assetName("asset-1")).thenReturn(Optional.of("timax- juliaca"));

        synchronizer.registerFromBinding(TUYA_ID, "asset-1", ContainerType.ASSET);

        verify(assignments).assign(
                eq(TUYA_ID),
                eq(new DevicePlacement(
                        "TIMAX - Juliaca", "Juliaca", "timax- juliaca")),
                eq("asset-1"));
    }

    @Test
    void doesNotDowngradeAnOnlineDeviceFromAStaleBindingSnapshot() {
        when(deviceRepository.findByDeviceId(TUYA_ID))
                .thenReturn(Optional.of(device(DeviceStatus.ONLINE)));
        when(cloudClient.listDevices()).thenReturn(List.of(cloudDevice(false)));

        synchronizer.registerFromBinding(TUYA_ID, "asset-1", ContainerType.ASSET);

        verify(deviceRepository).upsert(
                TUYA_ID,
                "WIFI Energy Meter Test",
                DeviceStatus.ONLINE,
                "dlq",
                "zblabrkwxcxezteq",
                "RPE1");
    }

    @Test
    void promotesAnOfflineDeviceWhenTheBindingSnapshotIsOnline() {
        when(deviceRepository.findByDeviceId(TUYA_ID))
                .thenReturn(Optional.of(device(DeviceStatus.OFFLINE)));
        when(cloudClient.listDevices()).thenReturn(List.of(cloudDevice(true)));

        synchronizer.registerFromBinding(TUYA_ID, "asset-1", ContainerType.ASSET);

        verify(deviceRepository).upsert(
                TUYA_ID,
                "WIFI Energy Meter Test",
                DeviceStatus.ONLINE,
                "dlq",
                "zblabrkwxcxezteq",
                "RPE1");
    }

    @Test
    void deactivatesUnboundDeviceWithoutDeletingItsHistory() {
        when(assignments.closeActive(TUYA_ID, "asset-1")).thenReturn(true);

        synchronizer.deactivate(TUYA_ID, "asset-1", ContainerType.ASSET);

        verify(assignments).closeActive(TUYA_ID, "asset-1");
        verify(deviceRepository).deactivateByDeviceId(TUYA_ID);
    }

    @Test
    void ignoresAStaleUnbindFromThePreviousAsset() {
        when(assignments.closeActive(TUYA_ID, "old-asset")).thenReturn(false);

        synchronizer.deactivate(TUYA_ID, "old-asset", ContainerType.ASSET);

        verify(deviceRepository, never()).deactivateByDeviceId(TUYA_ID);
    }

    private TuyaCloudDevice cloudDevice(boolean online) {
        return cloudDevice(TUYA_ID, online);
    }

    private TuyaCloudDevice cloudDevice(String deviceId, boolean online) {
        return new TuyaCloudDevice(
                deviceId,
                "WIFI Energy Meter Test",
                online,
                "dlq",
                "zblabrkwxcxezteq",
                "RPE1");
    }

    private Device device(DeviceStatus status) {
        return new Device(
                INTERNAL_ID,
                TUYA_ID,
                "WIFI Energy Meter Test",
                SITE_ID,
                "WIFI Energy Meter",
                "tuya",
                "dlq",
                "zblabrkwxcxezteq",
                "RPE1",
                true,
                status,
                NOW.minusSeconds(300),
                NOW.minusSeconds(3600),
                NOW.minusSeconds(300));
    }
}
