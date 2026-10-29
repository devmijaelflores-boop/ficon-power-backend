package com.devzonesoft.ficonpower.adapters.tuya.cloud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class TuyaAccessTokenProviderTest {

    @Test
    void reusesAValidToken() throws Exception {
        TuyaCloudTransport transport = mock(TuyaCloudTransport.class);
        var response = new ObjectMapper().readTree("""
                {
                  "success": true,
                  "result": {"access_token": "token-1", "expire_time": 7200}
                }
                """);
        when(transport.get("/v1.0/token?grant_type=1", "", "token"))
                .thenReturn(response);
        Clock clock = Clock.fixed(Instant.parse("2026-08-13T20:00:00Z"), ZoneOffset.UTC);
        TuyaAccessTokenProvider tokens = new TuyaAccessTokenProvider(transport, clock);

        assertEquals("token-1", tokens.accessToken());
        assertEquals("token-1", tokens.accessToken());

        verify(transport).get("/v1.0/token?grant_type=1", "", "token");
    }
}
