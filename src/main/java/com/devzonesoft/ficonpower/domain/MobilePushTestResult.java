package com.devzonesoft.ficonpower.domain;

public record MobilePushTestResult(
        boolean configured,
        boolean tokenRegistered,
        boolean accepted,
        String message
) {

    public static MobilePushTestResult unavailable() {
        return new MobilePushTestResult(
                false, false, false, "Firebase Cloud Messaging no está configurado.");
    }

    public static MobilePushTestResult tokenNotRegistered() {
        return new MobilePushTestResult(
                true, false, false, "El token no está registrado o fue revocado.");
    }
}
