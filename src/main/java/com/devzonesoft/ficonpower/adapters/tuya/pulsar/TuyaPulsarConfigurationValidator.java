package com.devzonesoft.ficonpower.adapters.tuya.pulsar;

import com.devzonesoft.ficonpower.config.TuyaPulsarProperties;
import org.springframework.stereotype.Component;

@Component
public class TuyaPulsarConfigurationValidator {

    public void validate(TuyaPulsarProperties properties) {
        if (!hasText(properties.getServiceUrl())) {
            throw new IllegalStateException("TUYA_PULSAR_URL is required");
        }
        if (!hasText(properties.getAccessId())) {
            throw new IllegalStateException(
                    "TUYA_ACCESS_ID is required when Tuya Pulsar is enabled");
        }
        if (properties.getAccessKey() == null || properties.getAccessKey().length() < 24) {
            throw new IllegalStateException(
                    "TUYA_ACCESS_KEY must contain at least 24 characters when Tuya Pulsar is enabled");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
