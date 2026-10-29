package com.devzonesoft.ficonpower.adapters.tuya.pulsar;

import java.util.Optional;
import java.time.Instant;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
public class TuyaDeviceEventParser {

    public Optional<TuyaConnectivityEvent> connectivityEvent(JsonNode message) {
        String bizCode = text(message, "bizCode");
        String eventType = text(message, "eventType");

        TuyaConnectivityEvent.Status status = switch (bizCode) {
            case "deviceOffline", "offline" -> TuyaConnectivityEvent.Status.OFFLINE;
            case "deviceOnline", "online" -> TuyaConnectivityEvent.Status.ONLINE;
            default -> switch (eventType) {
                case "offline" -> TuyaConnectivityEvent.Status.OFFLINE;
                case "online" -> TuyaConnectivityEvent.Status.ONLINE;
                default -> null;
            };
        };

        if (status == null) {
            return Optional.empty();
        }

        return firstNonBlank(
                message.path("data").path("devId"),
                message.path("bizData").path("devId"),
                message.path("devId")
        ).map(deviceId -> new TuyaConnectivityEvent(deviceId, status, eventTime(message).orElse(null)));
    }

    public Optional<TuyaDeviceLifecycleEvent> lifecycleEvent(JsonNode message) {
        String bizCode = text(message, "bizCode");
        String eventType = text(message, "eventType");

        TuyaDeviceLifecycleEvent.Type type = switch (bizCode) {
            case "industryAssetDeviceBind", "deviceBindSpace" ->
                TuyaDeviceLifecycleEvent.Type.BOUND;
            case "industryAssetDeviceUnbind", "deviceUnbindSpace", "delete" ->
                TuyaDeviceLifecycleEvent.Type.UNBOUND;
            case "device" -> switch (eventType) {
                case "bind" -> TuyaDeviceLifecycleEvent.Type.BOUND;
                case "unbind", "reset" -> TuyaDeviceLifecycleEvent.Type.UNBOUND;
                default -> null;
            };
            default -> null;
        };

        if (type == null) {
            return Optional.empty();
        }

        Optional<String> deviceId = firstNonBlank(
                message.path("data").path("devId"),
                message.path("bizData").path("devId"),
                message.path("devId")
        );
        if (deviceId.isEmpty()) {
            return Optional.empty();
        }

        Optional<String> assetId = firstNonBlank(
                message.path("data").path("assetId"),
                message.path("bizData").path("assetId"),
                message.path("assetId"));
        Optional<String> spaceId = firstNonBlank(
                message.path("data").path("spaceId"),
                message.path("bizData").path("spaceId"),
                message.path("spaceId"));
        if (assetId.isPresent()) {
            return Optional.of(new TuyaDeviceLifecycleEvent(
                    deviceId.get(), type, assetId.get(),
                    TuyaDeviceLifecycleEvent.ContainerType.ASSET));
        }
        if (spaceId.isPresent()) {
            return Optional.of(new TuyaDeviceLifecycleEvent(
                    deviceId.get(), type, spaceId.get(),
                    TuyaDeviceLifecycleEvent.ContainerType.SPACE));
        }
        return Optional.of(new TuyaDeviceLifecycleEvent(deviceId.get(), type));
    }

    public Optional<String> offlineDeviceId(JsonNode message) {
        return connectivityEvent(message)
                .filter(event -> event.status() == TuyaConnectivityEvent.Status.OFFLINE)
                .map(TuyaConnectivityEvent::deviceId);
    }

    private Optional<String> firstNonBlank(JsonNode... candidates) {
        for (JsonNode candidate : candidates) {
            if (candidate != null && candidate.isString() && !candidate.asString().isBlank()) {
                return Optional.of(candidate.asString());
            }
        }
        return Optional.empty();
    }

    private String text(JsonNode message, String field) {
        JsonNode value = message.get(field);
        return value == null ? "" : value.asString("");
    }

    private Optional<Instant> eventTime(JsonNode message) {
        JsonNode[] candidates = {
                message.path("ts"),
                message.path("t"),
                message.path("bizData").path("time"),
                message.path("bizData").path("t"),
                message.path("data").path("time"),
                message.path("data").path("t"),
                message.path("data").path("ts")
        };
        for (JsonNode candidate : candidates) {
            long value = candidate.asLong(0);
            if (value > 0) {
                return Optional.of(value > 10_000_000_000L
                        ? Instant.ofEpochMilli(value)
                        : Instant.ofEpochSecond(value));
            }
        }
        return Optional.empty();
    }
}
