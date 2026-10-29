package com.devzonesoft.ficonpower.domain;

public enum IncidentStatus {
    ACTIVE,
    ACKNOWLEDGED,
    RESOLVED;

    public String databaseValue() {
        return name().toLowerCase();
    }

    public static IncidentStatus fromDatabase(String value) {
        return valueOf(value.toUpperCase());
    }
}
