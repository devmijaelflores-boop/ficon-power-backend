package com.devzonesoft.ficonpower.domain;

public record WhatsAppTestResult(boolean configured, boolean accepted, String message) {

    public static WhatsAppTestResult unavailable() {
        return new WhatsAppTestResult(false, false, "WhatsApp no está configurado.");
    }
}
