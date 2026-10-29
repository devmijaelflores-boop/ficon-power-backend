package com.devzonesoft.ficonpower.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WhatsAppPropertiesTest {

    @Test
    void requiresUrlAndTokenToEnableTheChannel() {
        WhatsAppProperties properties = new WhatsAppProperties();

        assertFalse(properties.isConfigured());

        properties.setApiUrl("https://graph.facebook.test/messages");
        properties.setPhone("51999999999");
        properties.setToken("token");

        assertTrue(properties.isConfigured());
    }

    @Test
    void allowsBlankFallbackPhoneWhenRecipientsComeFromDatabase() {
        WhatsAppProperties properties = new WhatsAppProperties();
        properties.setApiUrl("https://graph.facebook.test/messages");
        properties.setPhone("   ");
        properties.setToken("token");

        assertTrue(properties.isConfigured());
    }
}
