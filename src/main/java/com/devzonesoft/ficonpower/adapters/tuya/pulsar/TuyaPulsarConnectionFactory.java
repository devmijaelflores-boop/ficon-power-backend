package com.devzonesoft.ficonpower.adapters.tuya.pulsar;

import com.devzonesoft.ficonpower.config.TuyaPulsarProperties;
import java.util.concurrent.TimeUnit;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.ClientBuilder;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.client.api.SubscriptionType;
import org.springframework.stereotype.Component;

@Component
public class TuyaPulsarConnectionFactory {

    private final TuyaPulsarMessageListener listener;

    public TuyaPulsarConnectionFactory(TuyaPulsarMessageListener listener) {
        this.listener = listener;
    }

    public TuyaPulsarConnection connect(TuyaPulsarProperties properties)
            throws PulsarClientException {
        ClientBuilder clientBuilder = PulsarClient.builder()
                .serviceUrl(properties.getServiceUrl())
                .authentication(new TuyaPulsarAuthentication(
                        properties.getAccessId(), properties.getAccessKey()));
        if (properties.getTlsTrustCertsPath() != null
                && !properties.getTlsTrustCertsPath().isBlank()) {
            clientBuilder.tlsTrustCertsFilePath(properties.getTlsTrustCertsPath());
        }
        PulsarClient client = clientBuilder.build();
        try {
            Consumer<byte[]> consumer = client.newConsumer(Schema.BYTES)
                    .topic(properties.topic())
                    .subscriptionName(properties.effectiveSubscriptionName())
                    .subscriptionType(SubscriptionType.Failover)
                    .autoUpdatePartitions(false)
                    .negativeAckRedeliveryDelay(10, TimeUnit.SECONDS)
                    .messageListener(listener)
                    .subscribe();
            return new TuyaPulsarConnection(client, consumer);
        } catch (PulsarClientException exception) {
            try {
                client.close();
            } catch (PulsarClientException closeException) {
                exception.addSuppressed(closeException);
            }
            throw exception;
        }
    }
}
