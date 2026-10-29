package com.devzonesoft.ficonpower.adapters.tuya.cloud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.devzonesoft.ficonpower.config.TuyaCloudProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class TuyaRequestSignerTest {

    @Test
    void createsDeterministicHeadersForAGetRequest() {
        TuyaCloudProperties properties = new TuyaCloudProperties();
        properties.setAccessId("access-id");
        properties.setAccessKey("access-key");
        Clock clock = Clock.fixed(Instant.parse("2026-08-13T20:00:00Z"), ZoneOffset.UTC);
        TuyaRequestSigner signer = new TuyaRequestSigner(properties, clock);

        HttpHeaders headers = signer.headersForGet("/v1.0/devices", "token");

        assertEquals("access-id", headers.getFirst("client_id"));
        assertEquals("1786651200000", headers.getFirst("t"));
        assertEquals("HMAC-SHA256", headers.getFirst("sign_method"));
        assertEquals("token", headers.getFirst("access_token"));
        assertFalse(headers.getFirst("sign").isBlank());
    }
}
