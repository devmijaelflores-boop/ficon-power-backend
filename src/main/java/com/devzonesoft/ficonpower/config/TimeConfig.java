package com.devzonesoft.ficonpower.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfig {

    @Bean
    ZoneId applicationTimeZone(@Value("${app.time-zone:America/Lima}") String value) {
        return ZoneId.of(value);
    }

    @Bean
    Clock clock(ZoneId applicationTimeZone) {
        return Clock.system(applicationTimeZone);
    }
}
