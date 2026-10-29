package com.devzonesoft.ficonpower.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.security.sessions")
public class AuthSessionProperties {

    private Duration accessDuration = Duration.ofHours(24);
    private Duration refreshDuration = Duration.ofDays(90);

    public Duration getAccessDuration() {
        return accessDuration;
    }

    public void setAccessDuration(Duration accessDuration) {
        this.accessDuration = accessDuration;
    }

    public Duration getRefreshDuration() {
        return refreshDuration;
    }

    public void setRefreshDuration(Duration refreshDuration) {
        this.refreshDuration = refreshDuration;
    }
}
