package com.devzonesoft.ficonpower.domain;

public enum DeviceStatus {
    ONLINE,
    OFFLINE;

    public String databaseValue() {
        return name().toLowerCase();
    }

    public static DeviceStatus fromDatabase(String value) {
        return valueOf(value.toUpperCase());
    }
}
