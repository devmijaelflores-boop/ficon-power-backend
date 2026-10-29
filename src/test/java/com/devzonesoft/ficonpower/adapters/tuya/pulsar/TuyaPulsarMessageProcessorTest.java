package com.devzonesoft.ficonpower.adapters.tuya.pulsar;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devzonesoft.ficonpower.adapters.tuya.cloud.TuyaDeviceSynchronizer;
import com.devzonesoft.ficonpower.config.TuyaPulsarProperties;
import com.devzonesoft.ficonpower.domain.DeviceStatus;
import com.devzonesoft.ficonpower.ports.input.HandleDeviceConnectivity;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class TuyaPulsarMessageProcessorTest {

    private static final Instant NOW = Instant.parse("2026-07-22T12:00:00Z");

    @Test
    void persistsAnOfflineMessageThroughTheConnectivityUseCase() {
        TuyaPulsarProperties properties = properties();
        TuyaMessageDecoder decoder = mock(TuyaMessageDecoder.class);
        HandleDeviceConnectivity handler = mock(HandleDeviceConnectivity.class);
        TuyaDeviceSynchronizer synchronizer = mock(TuyaDeviceSynchronizer.class);
        byte[] payload = "encrypted-envelope".getBytes(StandardCharsets.UTF_8);
        var decoded = new ObjectMapper().readTree("""
                {"bizCode":"deviceOffline","bizData":{"devId":"tuya-device-1"}}
                """);
        when(decoder.decode(payload, properties.getAccessKey(), "aes_gcm")).thenReturn(decoded);

        TuyaPulsarMessageProcessor processor = new TuyaPulsarMessageProcessor(
                properties,
                decoder,
                new TuyaDeviceEventParser(),
                handler,
                synchronizer,
                Clock.fixed(NOW, ZoneOffset.UTC));

        processor.process(payload, "aes_gcm");

        verify(synchronizer).ensureRegistered("tuya-device-1");
        verify(handler).handle("tuya-device-1", DeviceStatus.OFFLINE, NOW, decoded);
    }

    @Test
    void usesTheTuyaTimestampForAnOnlineMessage() {
        TuyaPulsarProperties properties = properties();
        TuyaMessageDecoder decoder = mock(TuyaMessageDecoder.class);
        HandleDeviceConnectivity handler = mock(HandleDeviceConnectivity.class);
        TuyaDeviceSynchronizer synchronizer = mock(TuyaDeviceSynchronizer.class);
        byte[] payload = "encrypted-envelope".getBytes(StandardCharsets.UTF_8);
        var decoded = new ObjectMapper().readTree("""
                {"bizCode":"deviceOnline","bizData":{"devId":"tuya-device-1"},"ts":1784721600000}
                """);
        when(decoder.decode(payload, properties.getAccessKey(), "aes_gcm")).thenReturn(decoded);

        TuyaPulsarMessageProcessor processor = new TuyaPulsarMessageProcessor(
                properties,
                decoder,
                new TuyaDeviceEventParser(),
                handler,
                synchronizer,
                Clock.fixed(NOW, ZoneOffset.UTC));

        processor.process(payload, "aes_gcm");

        verify(handler).handle(
                "tuya-device-1",
                DeviceStatus.ONLINE,
                Instant.ofEpochMilli(1784721600000L),
                decoded);
    }

    @Test
    void registersADeviceWhenTuyaBindsItToAnIndustrialAsset() {
        TuyaPulsarProperties properties = properties();
        TuyaMessageDecoder decoder = mock(TuyaMessageDecoder.class);
        HandleDeviceConnectivity handler = mock(HandleDeviceConnectivity.class);
        TuyaDeviceSynchronizer synchronizer = mock(TuyaDeviceSynchronizer.class);
        byte[] payload = "encrypted-envelope".getBytes(StandardCharsets.UTF_8);
        var decoded = new ObjectMapper().readTree("""
                {"bizCode":"industryAssetDeviceBind","bizData":{"devId":"tuya-device-1","assetId":"asset-1"}}
                """);
        when(decoder.decode(payload, properties.getAccessKey(), "aes_gcm")).thenReturn(decoded);

        new TuyaPulsarMessageProcessor(
                properties, decoder, new TuyaDeviceEventParser(), handler,
                synchronizer, Clock.fixed(NOW, ZoneOffset.UTC))
                .process(payload, "aes_gcm");

        verify(synchronizer).registerFromBinding(
                "tuya-device-1",
                "asset-1",
                TuyaDeviceLifecycleEvent.ContainerType.ASSET);
    }

    @Test
    void deactivatesADeviceWhenTuyaUnbindsItFromAnIndustrialAsset() {
        TuyaPulsarProperties properties = properties();
        TuyaMessageDecoder decoder = mock(TuyaMessageDecoder.class);
        HandleDeviceConnectivity handler = mock(HandleDeviceConnectivity.class);
        TuyaDeviceSynchronizer synchronizer = mock(TuyaDeviceSynchronizer.class);
        byte[] payload = "encrypted-envelope".getBytes(StandardCharsets.UTF_8);
        var decoded = new ObjectMapper().readTree("""
                {"bizCode":"industryAssetDeviceUnbind","bizData":{"devId":"tuya-device-1","assetId":"asset-1"}}
                """);
        when(decoder.decode(payload, properties.getAccessKey(), "aes_gcm")).thenReturn(decoded);

        new TuyaPulsarMessageProcessor(
                properties, decoder, new TuyaDeviceEventParser(), handler,
                synchronizer, Clock.fixed(NOW, ZoneOffset.UTC))
                .process(payload, "aes_gcm");

        verify(synchronizer).deactivate(
                "tuya-device-1",
                "asset-1",
                TuyaDeviceLifecycleEvent.ContainerType.ASSET);
    }

    private TuyaPulsarProperties properties() {
        TuyaPulsarProperties properties = new TuyaPulsarProperties();
        properties.setAccessKey("12345678abcdefghijklmnop12345678");
        return properties;
    }
}
