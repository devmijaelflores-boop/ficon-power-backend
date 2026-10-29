package com.devzonesoft.ficonpower.adapters.tuya.pulsar;

import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TuyaPulsarConnection implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(TuyaPulsarConnection.class);

    private final PulsarClient client;
    private final Consumer<byte[]> consumer;
    private boolean closed;

    TuyaPulsarConnection(PulsarClient client, Consumer<byte[]> consumer) {
        this.client = client;
        this.consumer = consumer;
    }

    public boolean isConnected() {
        return !closed && consumer.isConnected();
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closeConsumer();
        closeClient();
        closed = true;
    }

    private void closeConsumer() {
        try {
            consumer.close();
        } catch (PulsarClientException exception) {
            logger.warn("Unable to close Tuya Pulsar consumer cleanly", exception);
        }
    }

    private void closeClient() {
        try {
            client.close();
        } catch (PulsarClientException exception) {
            logger.warn("Unable to close Tuya Pulsar client cleanly", exception);
        }
    }
}
