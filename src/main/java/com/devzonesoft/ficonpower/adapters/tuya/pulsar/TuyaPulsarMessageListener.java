package com.devzonesoft.ficonpower.adapters.tuya.pulsar;

import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TuyaPulsarMessageListener implements MessageListener<byte[]> {

    private static final Logger logger =
            LoggerFactory.getLogger(TuyaPulsarMessageListener.class);
    private static final String ENCRYPTION_MODE_PROPERTY = "em";

    private final TuyaPulsarMessageProcessor processor;

    public TuyaPulsarMessageListener(TuyaPulsarMessageProcessor processor) {
        this.processor = processor;
    }

    @Override
    public void received(Consumer<byte[]> consumer, Message<byte[]> message) {
        try {
            processor.process(
                    message.getData(),
                    message.getProperty(ENCRYPTION_MODE_PROPERTY));
            consumer.acknowledgeCumulativeAsync(message);
        } catch (Exception exception) {
            logger.error(
                    "Unable to process Tuya Pulsar message {}",
                    message.getMessageId(),
                    exception);
            consumer.negativeAcknowledge(message);
        }
    }
}
