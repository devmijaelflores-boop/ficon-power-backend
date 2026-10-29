package com.devzonesoft.ficonpower.adapters.tuya.cloud;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
public class TuyaCloudDeviceMapper {

    public List<TuyaCloudDevice> mapDevices(JsonNode response) {
        JsonNode result = response.path("result");
        List<TuyaCloudDevice> devices = new ArrayList<>();
        if (result.isArray()) {
            result.forEach(node -> mapDevice(node).ifPresent(devices::add));
            return devices;
        }

        JsonNode entries = result.path("devices");
        if (!entries.isArray()) {
            entries = result.path("list");
        }
        if (entries.isArray()) {
            entries.forEach(node -> mapDevice(node).ifPresent(devices::add));
        }
        return devices;
    }

    Optional<TuyaCloudDevice> mapDevice(JsonNode node) {
        String deviceId = firstText(node, "id", "device_id");
        if (deviceId.isBlank()) {
            return Optional.empty();
        }
        String name = firstText(
                node,
                "customName",
                "custom_name",
                "name",
                "device_name",
                "productName",
                "product_name");
        if (name.isBlank()) {
            name = "Dispositivo Tuya";
        }
        boolean online = node.path("isOnline").asBoolean(false)
                || node.path("online").asBoolean(false)
                || node.path("is_online").asBoolean(false);
        return Optional.of(new TuyaCloudDevice(
                deviceId,
                name,
                online,
                nullable(firstText(node, "category", "category_code")),
                nullable(firstText(node, "product_id", "productId")),
                nullable(firstText(node, "model", "product_model"))));
    }

    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = node.path(field).asString("");
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String nullable(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
