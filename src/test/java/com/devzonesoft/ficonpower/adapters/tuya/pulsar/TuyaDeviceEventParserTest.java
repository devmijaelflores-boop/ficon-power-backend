package com.devzonesoft.ficonpower.adapters.tuya.pulsar;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class TuyaDeviceEventParserTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TuyaDeviceEventParser parser = new TuyaDeviceEventParser();

    @Test
    void parsesTheIotCoreOfflineFormat() {
        var message = objectMapper.readTree("""
                {
                  "bizCode": "deviceOffline",
                  "bizData": {"devId": "iot-core-device"}
                }
                """);

        assertThat(parser.offlineDeviceId(message)).contains("iot-core-device");
    }

    @Test
    void parsesTheLegacyOfflineFormat() {
        var message = objectMapper.readTree("""
                {
                  "devId": "legacy-device",
                  "bizCode": "offline",
                  "bizData": {"time": 1721653200000}
                }
                """);

        assertThat(parser.offlineDeviceId(message)).contains("legacy-device");
    }

    @Test
    void parsesTheDeviceEventOfflineFormat() {
        var message = objectMapper.readTree("""
                {
                  "bizCode": "device",
                  "eventType": "offline",
                  "data": {"devId": "event-device"}
                }
                """);

        assertThat(parser.offlineDeviceId(message)).contains("event-device");
    }

    @Test
    void ignoresNonOfflineEvents() {
        var message = objectMapper.readTree("""
                {"bizCode": "deviceOnline", "bizData": {"devId": "device-1"}}
                """);

        assertThat(parser.offlineDeviceId(message)).isEmpty();
    }

    @Test
    void parsesAnOnlineConnectivityEvent() {
        var message = objectMapper.readTree("""
                {"bizCode": "deviceOnline", "bizData": {"devId": "device-1"}}
                """);

        assertThat(parser.connectivityEvent(message))
                .contains(new TuyaConnectivityEvent(
                        "device-1", TuyaConnectivityEvent.Status.ONLINE, null));
    }

    @Test
    void parsesAnIndustrialAssetDeviceBind() {
        var message = objectMapper.readTree("""
                {
                  "bizCode": "industryAssetDeviceBind",
                  "bizData": {"devId": "device-1", "assetId": "asset-1"}
                }
                """);

        assertThat(parser.lifecycleEvent(message)).contains(
                new TuyaDeviceLifecycleEvent(
                        "device-1", TuyaDeviceLifecycleEvent.Type.BOUND,
                        "asset-1", TuyaDeviceLifecycleEvent.ContainerType.ASSET));
    }

    @Test
    void parsesAnIndustrialAssetDeviceUnbind() {
        var message = objectMapper.readTree("""
                {
                  "bizCode": "industryAssetDeviceUnbind",
                  "bizData": {"devId": "device-1", "assetId": "asset-1"}
                }
                """);

        assertThat(parser.lifecycleEvent(message)).contains(
                new TuyaDeviceLifecycleEvent(
                        "device-1", TuyaDeviceLifecycleEvent.Type.UNBOUND,
                        "asset-1", TuyaDeviceLifecycleEvent.ContainerType.ASSET));
    }

    @Test
    void parsesLegacyBindAndUnbindFormats() {
        var bind = objectMapper.readTree("""
                {"bizCode":"device","eventType":"bind","data":{"devId":"device-1"}}
                """);
        var unbind = objectMapper.readTree("""
                {"bizCode":"deviceUnbindSpace","bizData":{"devId":"device-1"}}
                """);

        assertThat(parser.lifecycleEvent(bind).map(TuyaDeviceLifecycleEvent::type))
                .contains(TuyaDeviceLifecycleEvent.Type.BOUND);
        assertThat(parser.lifecycleEvent(unbind).map(TuyaDeviceLifecycleEvent::type))
                .contains(TuyaDeviceLifecycleEvent.Type.UNBOUND);
    }
}
