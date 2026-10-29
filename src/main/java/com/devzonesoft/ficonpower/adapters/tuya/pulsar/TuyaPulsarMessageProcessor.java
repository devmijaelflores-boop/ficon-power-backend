package com.devzonesoft.ficonpower.adapters.tuya.pulsar;

import com.devzonesoft.ficonpower.config.TuyaPulsarProperties;
import com.devzonesoft.ficonpower.adapters.tuya.cloud.TuyaDeviceSynchronizer;
import com.devzonesoft.ficonpower.domain.DeviceStatus;
import com.devzonesoft.ficonpower.ports.input.HandleDeviceConnectivity;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
public class TuyaPulsarMessageProcessor {

    private final TuyaPulsarProperties properties;
    private final TuyaMessageDecoder decoder;
    private final TuyaDeviceEventParser eventParser;
    private final HandleDeviceConnectivity handleDeviceConnectivity;
    private final TuyaDeviceSynchronizer deviceSynchronizer;
    private final Clock clock;

    public TuyaPulsarMessageProcessor(
            TuyaPulsarProperties properties,
            TuyaMessageDecoder decoder,
            TuyaDeviceEventParser eventParser,
            HandleDeviceConnectivity handleDeviceConnectivity,
            TuyaDeviceSynchronizer deviceSynchronizer,
            Clock clock
    ) {
        this.properties = properties;
        this.decoder = decoder;
        this.eventParser = eventParser;
        this.handleDeviceConnectivity = handleDeviceConnectivity;
        this.deviceSynchronizer = deviceSynchronizer;
        this.clock = clock;
    }

    public void process(byte[] payload, String encryptionMode) {
        JsonNode message = decoder.decode(payload, properties.getAccessKey(), encryptionMode);
        eventParser.connectivityEvent(message).ifPresent(event -> processConnectivityEvent(event, message));
        eventParser.lifecycleEvent(message).ifPresent(this::processLifecycleEvent);
    }

    private void processLifecycleEvent(TuyaDeviceLifecycleEvent event) {
        if (event.type() == TuyaDeviceLifecycleEvent.Type.BOUND) {
            deviceSynchronizer.registerFromBinding(
                    event.deviceId(), event.containerId(), event.containerType());
            return;
        }
        deviceSynchronizer.deactivate(
                event.deviceId(), event.containerId(), event.containerType());
    }

    private void processConnectivityEvent(TuyaConnectivityEvent event, JsonNode message) {
        deviceSynchronizer.ensureRegistered(event.deviceId());
        DeviceStatus status = event.status() == TuyaConnectivityEvent.Status.OFFLINE
                ? DeviceStatus.OFFLINE
                : DeviceStatus.ONLINE;
        Instant occurredAt = event.occurredAt() == null
                ? Instant.now(clock)
                : event.occurredAt();
        handleDeviceConnectivity.handle(event.deviceId(), status, occurredAt, message);
    }
}
