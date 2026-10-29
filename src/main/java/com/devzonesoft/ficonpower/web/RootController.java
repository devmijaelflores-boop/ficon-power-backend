package com.devzonesoft.ficonpower.web;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {

    private final Clock clock;
    private final ZoneId timeZone;

    public RootController(Clock clock, ZoneId applicationTimeZone) {
        this.clock = clock;
        this.timeZone = applicationTimeZone;
    }

    @GetMapping("/")
    public Map<String, Object> home() {
        return Map.of(
                "service", "ficon-power-backend",
                "status", "UP",
                "timeZone", timeZone.getId(),
                "currentTime", Instant.now(clock).atZone(timeZone).toOffsetDateTime()
        );
    }
}
