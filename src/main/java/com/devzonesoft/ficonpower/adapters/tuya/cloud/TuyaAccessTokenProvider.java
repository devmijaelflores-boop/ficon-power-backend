package com.devzonesoft.ficonpower.adapters.tuya.cloud;

import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
public class TuyaAccessTokenProvider {

    private static final String TOKEN_PATH = "/v1.0/token?grant_type=1";

    private final TuyaCloudTransport transport;
    private final Clock clock;
    private volatile Token token;

    public TuyaAccessTokenProvider(TuyaCloudTransport transport, Clock clock) {
        this.transport = transport;
        this.clock = clock;
    }

    public synchronized String accessToken() {
        Instant now = Instant.now(clock);
        if (token != null && token.expiresAt().isAfter(now.plusSeconds(60))) {
            return token.value();
        }

        JsonNode response = transport.get(TOKEN_PATH, "", "token");
        String value = response.path("result").path("access_token").asString();
        long expiresIn = response.path("result").path("expire_time").asLong(7200);
        token = new Token(value, now.plusSeconds(expiresIn));
        return value;
    }

    private record Token(String value, Instant expiresAt) {
    }
}
