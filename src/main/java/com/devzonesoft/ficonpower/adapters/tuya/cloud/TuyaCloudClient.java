package com.devzonesoft.ficonpower.adapters.tuya.cloud;

import com.devzonesoft.ficonpower.config.TuyaCloudProperties;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
public class TuyaCloudClient {

    private static final String DEVICE_LIST_PATH = "/v2.0/cloud/thing/device?page_size=20";

    private final TuyaCloudProperties properties;
    private final TuyaCloudTransport transport;
    private final TuyaAccessTokenProvider tokens;
    private final TuyaCloudDeviceMapper deviceMapper;

    public TuyaCloudClient(
            TuyaCloudProperties properties,
            TuyaCloudTransport transport,
            TuyaAccessTokenProvider tokens,
            TuyaCloudDeviceMapper deviceMapper
    ) {
        this.properties = properties;
        this.transport = transport;
        this.tokens = tokens;
        this.deviceMapper = deviceMapper;
    }

    public List<TuyaCloudDevice> listDevices() {
        if (!properties.isEnabled()) {
            return List.of();
        }

        JsonNode response = transport.get(
                DEVICE_LIST_PATH, tokens.accessToken(), "device list");
        return deviceMapper.mapDevices(response);
    }

    public Optional<String> assetName(String assetId) {
        return containerName(
                "/v1.0/iot-03/assets/" + assetId,
                "asset details",
                "asset_name", "assetName", "name");
    }

    public Optional<String> spaceName(String spaceId) {
        return containerName(
                "/v2.0/cloud/space/" + spaceId,
                "space details",
                "name", "space_name", "spaceName");
    }

    private Optional<String> containerName(
            String path,
            String operation,
            String... fields
    ) {
        if (!properties.isEnabled()) {
            return Optional.empty();
        }
        JsonNode result = transport.get(path, tokens.accessToken(), operation).path("result");
        for (String field : fields) {
            String value = result.path(field).asString("").trim();
            if (!value.isEmpty()) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }
}
