package com.devzonesoft.ficonpower.adapters.tuya.pulsar;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.junit.jupiter.api.Test;

class TuyaPulsarMessageListenerTest {

    private final TuyaPulsarMessageProcessor processor = mock(TuyaPulsarMessageProcessor.class);
    private final TuyaPulsarMessageListener listener = new TuyaPulsarMessageListener(processor);

    @Test
    @SuppressWarnings("unchecked")
    void acknowledgesProcessedMessages() {
        Consumer<byte[]> consumer = mock(Consumer.class);
        Message<byte[]> message = mock(Message.class);
        byte[] payload = {1, 2, 3};
        org.mockito.Mockito.when(message.getData()).thenReturn(payload);
        org.mockito.Mockito.when(message.getProperty("em")).thenReturn("aes-gcm");

        listener.received(consumer, message);

        verify(processor).process(payload, "aes-gcm");
        verify(consumer).acknowledgeCumulativeAsync(message);
    }

    @Test
    @SuppressWarnings("unchecked")
    void negativelyAcknowledgesFailedMessages() {
        Consumer<byte[]> consumer = mock(Consumer.class);
        Message<byte[]> message = mock(Message.class);
        byte[] payload = {1, 2, 3};
        org.mockito.Mockito.when(message.getData()).thenReturn(payload);
        org.mockito.Mockito.when(message.getProperty("em")).thenReturn("aes-gcm");
        doThrow(new IllegalArgumentException("invalid payload"))
                .when(processor).process(payload, "aes-gcm");

        listener.received(consumer, message);

        verify(consumer).negativeAcknowledge(message);
    }
}
