package com.devzonesoft.ficonpower.adapters.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.devzonesoft.ficonpower.domain.MobilePushTestResult;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MultiNotificationServiceTest {

    @Test
    void sendsEscalationRemindersOnlyThroughPushChannels() {
        RecordingPushChannel push = new RecordingPushChannel();
        RecordingChannel whatsapp = new RecordingChannel();
        MultiNotificationService service = new MultiNotificationService(List.of(push, whatsapp));

        service.notifyPowerOutagePushOnly(
                UUID.fromString("e1c0e9a2-d9f9-41de-9ad3-e91562ab9fb3"),
                "WIFI Energy Meter",
                Instant.parse("2026-08-26T15:00:00Z"));

        assertThat(push.powerOutageCalls).isEqualTo(1);
        assertThat(whatsapp.powerOutageCalls).isZero();
    }

    @Test
    void sendsTestOnlyThroughTheConfiguredPushChannel() {
        RecordingPushChannel push = new RecordingPushChannel();
        MultiNotificationService service = new MultiNotificationService(List.of(
                push, new RecordingChannel()));
        UUID userId = UUID.fromString("f768047c-b56b-4432-8c84-b183595486d9");

        MobilePushTestResult result = service.sendTest(userId, "current-token");

        assertThat(result.accepted()).isTrue();
        assertThat(push.testCalls).isEqualTo(1);
    }

    @Test
    void reportsWhenNoPushChannelIsConfigured() {
        MultiNotificationService service = new MultiNotificationService(List.of(
                new RecordingChannel()));

        MobilePushTestResult result = service.sendTest(UUID.randomUUID(), "token");

        assertThat(result.configured()).isFalse();
        assertThat(result.accepted()).isFalse();
    }

    private static class RecordingChannel implements NotificationChannel {
        int powerOutageCalls;

        @Override
        public void notifyPowerOutage(String deviceName, Instant outageAt) {
            powerOutageCalls++;
        }

        @Override
        public void notifyPowerOutage(UUID incidentId, String deviceName, Instant outageAt) {
            powerOutageCalls++;
        }

        @Override
        public void notifyPowerRestored(String deviceName, Instant outageAt, Instant restoredAt) {
        }
    }

    private static class RecordingPushChannel extends RecordingChannel
            implements MobilePushTestChannel {
        int testCalls;

        @Override
        public MobilePushTestResult sendTest(UUID userId, String token) {
            testCalls++;
            return new MobilePushTestResult(true, true, true, "accepted");
        }
    }
}
