package com.devzonesoft.ficonpower.adapters.notification;

import com.devzonesoft.ficonpower.domain.WhatsAppTestResult;

public interface WhatsAppTestChannel {
    WhatsAppTestResult sendTest(String phone, String label);
}
