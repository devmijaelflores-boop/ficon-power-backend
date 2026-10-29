package com.devzonesoft.ficonpower.domain;

public enum NotificationEventType {
    POWER_OUTAGE("power_outage"),
    POWER_RESTORED("power_restored");

    private final String code;

    NotificationEventType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static NotificationEventType fromCode(String value) {
        for (NotificationEventType type : values()) {
            if (type.code.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Tipo de evento de notificacion desconocido: " + value);
    }
}
