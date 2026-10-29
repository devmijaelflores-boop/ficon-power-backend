package com.devzonesoft.ficonpower.adapters.tuya.pulsar;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.devzonesoft.ficonpower.config.TuyaPulsarProperties;
import org.junit.jupiter.api.Test;

class TuyaPulsarConfigurationValidatorTest {

    private final TuyaPulsarConfigurationValidator validator =
            new TuyaPulsarConfigurationValidator();

    @Test
    void acceptsACompleteConfiguration() {
        assertDoesNotThrow(() -> validator.validate(validProperties()));
    }

    @Test
    void rejectsMissingAccessId() {
        TuyaPulsarProperties properties = validProperties();
        properties.setAccessId(" ");

        assertThrows(IllegalStateException.class, () -> validator.validate(properties));
    }

    @Test
    void rejectsShortAccessKeys() {
        TuyaPulsarProperties properties = validProperties();
        properties.setAccessKey("too-short");

        assertThrows(IllegalStateException.class, () -> validator.validate(properties));
    }

    private TuyaPulsarProperties validProperties() {
        TuyaPulsarProperties properties = new TuyaPulsarProperties();
        properties.setServiceUrl("pulsar+ssl://mqe.tuyaus.com:7285/");
        properties.setAccessId("access-id");
        properties.setAccessKey("123456789012345678901234");
        return properties;
    }
}
