package com.devzonesoft.ficonpower.adapters.tuya.pulsar;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.timeout;

import com.devzonesoft.ficonpower.config.TuyaPulsarProperties;
import org.junit.jupiter.api.Test;
import org.apache.pulsar.client.api.PulsarClientException;

class TuyaPulsarConsumerTest {

    private final TuyaPulsarConfigurationValidator validator =
            mock(TuyaPulsarConfigurationValidator.class);
    private final TuyaPulsarConnectionFactory connections =
            mock(TuyaPulsarConnectionFactory.class);

    @Test
    void doesNotStartWhenPulsarIsDisabled() {
        TuyaPulsarProperties properties = new TuyaPulsarProperties();
        TuyaPulsarConsumer consumer = new TuyaPulsarConsumer(
                properties, validator, connections);

        consumer.start();

        assertFalse(consumer.isRunning());
        verifyNoInteractions(validator, connections);
    }

    @Test
    void opensAndClosesOneManagedConnection() throws Exception {
        TuyaPulsarProperties properties = new TuyaPulsarProperties();
        properties.setEnabled(true);
        properties.setAccessId("access-id");
        TuyaPulsarConnection connection = mock(TuyaPulsarConnection.class);
        when(connections.connect(properties)).thenReturn(connection);
        TuyaPulsarConsumer consumer = new TuyaPulsarConsumer(
                properties, validator, connections);

        consumer.start();
        verify(connections, timeout(1_000)).connect(properties);
        awaitStatus(consumer, TuyaPulsarConsumer.ConnectionStatus.CONNECTED);
        assertTrue(consumer.isRunning());
        consumer.stop();

        assertFalse(consumer.isRunning());
        verify(validator).validate(properties);
        verify(connection).close();
    }

    @Test
    void keepsBackendLifecycleActiveWhenInitialConnectionFails() throws Exception {
        TuyaPulsarProperties properties = new TuyaPulsarProperties();
        properties.setEnabled(true);
        properties.setAccessId("access-id");
        when(connections.connect(properties))
                .thenThrow(new PulsarClientException("TLS unavailable"));
        TuyaPulsarConsumer consumer = new TuyaPulsarConsumer(
                properties, validator, connections);

        consumer.start();

        verify(connections, timeout(1_000)).connect(properties);
        awaitStatus(consumer, TuyaPulsarConsumer.ConnectionStatus.DEGRADED);
        assertTrue(consumer.isRunning());
        assertEquals(1, consumer.snapshot().retryAttempt());
        consumer.stop();
    }

    private void awaitStatus(
            TuyaPulsarConsumer consumer,
            TuyaPulsarConsumer.ConnectionStatus expected
    ) throws InterruptedException {
        long deadline = System.nanoTime() + 1_000_000_000L;
        while (consumer.snapshot().status() != expected && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertEquals(expected, consumer.snapshot().status());
    }
}
