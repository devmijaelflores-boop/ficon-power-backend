package com.devzonesoft.ficonpower.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.tuya.pulsar")
public class TuyaPulsarProperties {

    private boolean enabled;
    private String serviceUrl = "pulsar+ssl://mqe.tuyaus.com:7285/";
    private String accessId = "";
    private String accessKey = "";
    private Environment environment = Environment.PROD;
    private String subscriptionName = "";
    private boolean directNotifications;
    private String tlsTrustCertsPath = "";

    public enum Environment {
        PROD("event"),
        TEST("event-test");

        private final String topicSuffix;

        Environment(String topicSuffix) {
            this.topicSuffix = topicSuffix;
        }

        public String topicSuffix() {
            return topicSuffix;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getServiceUrl() {
        return serviceUrl;
    }

    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    public String getAccessId() {
        return accessId;
    }

    public void setAccessId(String accessId) {
        this.accessId = accessId;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    public String getSubscriptionName() {
        return subscriptionName;
    }

    public void setSubscriptionName(String subscriptionName) {
        this.subscriptionName = subscriptionName;
    }

    public boolean isDirectNotifications() {
        return directNotifications;
    }

    public void setDirectNotifications(boolean directNotifications) {
        this.directNotifications = directNotifications;
    }

    public String getTlsTrustCertsPath() {
        return tlsTrustCertsPath;
    }

    public void setTlsTrustCertsPath(String tlsTrustCertsPath) {
        this.tlsTrustCertsPath = tlsTrustCertsPath;
    }

    public String topic() {
        return "%s/out/%s".formatted(accessId, environment.topicSuffix());
    }

    public String effectiveSubscriptionName() {
        return subscriptionName == null || subscriptionName.isBlank()
                ? accessId + "-sub"
                : subscriptionName;
    }
}
