package com.devzonesoft.ficonpower.adapters.tuya.pulsar;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.PulsarClient;
import org.junit.jupiter.api.Test;

class TuyaPulsarConnectionTest {

    @Test
    @SuppressWarnings("unchecked")
    void closesConsumerAndClientOnlyOnce() throws Exception {
        PulsarClient client = mock(PulsarClient.class);
        Consumer<byte[]> consumer = mock(Consumer.class);
        TuyaPulsarConnection connection = new TuyaPulsarConnection(client, consumer);

        connection.close();
        connection.close();

        verify(consumer, times(1)).close();
        verify(client, times(1)).close();
    }
}
