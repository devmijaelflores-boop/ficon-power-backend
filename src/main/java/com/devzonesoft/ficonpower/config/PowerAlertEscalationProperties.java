package com.devzonesoft.ficonpower.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.notifications.power-alerts")
public class PowerAlertEscalationProperties {

    private static final List<Duration> DEFAULT_RETRY_DELAYS = List.of(Duration.ofSeconds(30));

    private boolean enabled = true;
    private int batchSize = 50;
    private List<Duration> retryDelays = DEFAULT_RETRY_DELAYS;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public List<Duration> getRetryDelays() {
        return retryDelays;
    }

    public void setRetryDelays(List<Duration> retryDelays) {
        this.retryDelays = retryDelays == null || retryDelays.isEmpty()
                ? DEFAULT_RETRY_DELAYS
                : List.copyOf(retryDelays);
    }
}
