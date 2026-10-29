package com.devzonesoft.ficonpower.web;

import com.devzonesoft.ficonpower.adapters.tuya.pulsar.TuyaPulsarConsumer;
import com.devzonesoft.ficonpower.adapters.tuya.pulsar.TuyaPulsarConsumer.ConnectionSnapshot;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TuyaPulsarHealthController {

    private final TuyaPulsarConsumer consumer;

    public TuyaPulsarHealthController(TuyaPulsarConsumer consumer) {
        this.consumer = consumer;
    }

    @GetMapping("/health/tuya-pulsar")
    public ConnectionSnapshot health() {
        return consumer.snapshot();
    }
}
