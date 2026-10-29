package com.devzonesoft.ficonpower.domain;

public record NotificationTemplate(NotificationEventType eventType, String body) {

    private static final String DEFAULT_OUTAGE_BODY =
            "DEVICE OFF\nDispositivo: {dispositivo}\nFecha y hora del corte: {fechaCorte}";
    private static final String DEFAULT_RESTORED_BODY =
            "DEVICE ON\nDispositivo: {dispositivo}\nFecha y hora del corte: {fechaCorte}\nFecha y hora de retorno: {fechaRetorno}";

    public static NotificationTemplate defaultFor(NotificationEventType eventType) {
        return new NotificationTemplate(eventType, switch (eventType) {
            case POWER_OUTAGE -> DEFAULT_OUTAGE_BODY;
            case POWER_RESTORED -> DEFAULT_RESTORED_BODY;
        });
    }
}
