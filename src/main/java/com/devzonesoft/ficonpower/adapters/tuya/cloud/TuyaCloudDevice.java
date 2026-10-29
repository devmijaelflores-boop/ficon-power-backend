package com.devzonesoft.ficonpower.adapters.tuya.cloud;

public record TuyaCloudDevice(
        String deviceId,
        String name,
        boolean online,
        String category,
        String productId,
        String model
) {
}
