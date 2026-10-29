package com.devzonesoft.ficonpower;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "app.tuya.pulsar.enabled=false",
        "app.tuya.cloud.enabled=false",
        "app.notifications.power-alerts.enabled=false",
        "DATABASE_URL=jdbc:h2:mem:ficon-test;DB_CLOSE_DELAY=-1",
        "spring.flyway.enabled=false"
})
class FiconPowerApplicationTest {

    @Test
    void contextLoads() {
    }
}
