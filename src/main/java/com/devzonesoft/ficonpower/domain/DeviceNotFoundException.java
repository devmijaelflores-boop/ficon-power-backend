package com.devzonesoft.ficonpower.domain;

public class DeviceNotFoundException extends RuntimeException {

    public DeviceNotFoundException(String deviceId) {
        super("Device not found: " + deviceId);
    }
}
