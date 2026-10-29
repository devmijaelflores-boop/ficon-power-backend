package com.devzonesoft.ficonpower.ports.output;

import com.devzonesoft.ficonpower.domain.WhatsAppTestResult;

public interface WhatsAppTestSender {
    WhatsAppTestResult sendWhatsAppTest(String phone, String label);
}
