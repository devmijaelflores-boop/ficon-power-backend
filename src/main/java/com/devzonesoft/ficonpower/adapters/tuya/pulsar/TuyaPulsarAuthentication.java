package com.devzonesoft.ficonpower.adapters.tuya.pulsar;

import java.io.IOException;
import java.io.Serial;
import java.util.Map;
import org.apache.pulsar.client.api.Authentication;
import org.apache.pulsar.client.api.AuthenticationDataProvider;
import org.apache.pulsar.client.api.PulsarClientException;

/**
 * Tuya's auth1 implementation, adapted from the official Tuya Pulsar Java SDK.
 */
@SuppressWarnings("deprecation")
public class TuyaPulsarAuthentication implements Authentication {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String accessId;
    private final String accessKey;

    public TuyaPulsarAuthentication(String accessId, String accessKey) {
        this.accessId = accessId;
        this.accessKey = accessKey;
    }

    @Override
    public String getAuthMethodName() {
        return "auth1";
    }

    @Override
    public AuthenticationDataProvider getAuthData() throws PulsarClientException {
        return new TuyaPulsarAuthenticationData(accessId, accessKey);
    }

    @Override
    public AuthenticationDataProvider getAuthData(String brokerHostName) throws PulsarClientException {
        return new TuyaPulsarAuthenticationData(accessId, accessKey);
    }

    @Override
    public void configure(Map<String, String> authParams) {
        // Credentials are supplied through the constructor.
    }

    @Override
    public void start() throws PulsarClientException {
        // No external resources are required.
    }

    @Override
    public void close() throws IOException {
        // No external resources are held.
    }
}
