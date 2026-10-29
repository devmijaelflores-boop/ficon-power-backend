package com.devzonesoft.ficonpower.adapters.tuya.pulsar;

public record TuyaDeviceLifecycleEvent(
        String deviceId,
        Type type,
        String containerId,
        ContainerType containerType
) {

    public TuyaDeviceLifecycleEvent(String deviceId, Type type) {
        this(deviceId, type, null, ContainerType.NONE);
    }

    public enum Type {
        BOUND,
        UNBOUND
    }

    public enum ContainerType {
        ASSET,
        SPACE,
        NONE
    }
}
