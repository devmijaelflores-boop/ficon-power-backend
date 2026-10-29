package com.devzonesoft.ficonpower.adapters.notification;

import com.devzonesoft.ficonpower.domain.MobilePushTestResult;
import com.devzonesoft.ficonpower.domain.WhatsAppTestResult;
import com.devzonesoft.ficonpower.ports.output.MobilePushTestSender;
import com.devzonesoft.ficonpower.ports.output.NotificationService;
import com.devzonesoft.ficonpower.ports.output.WhatsAppTestSender;
import java.util.List;
import java.time.Instant;
import java.util.UUID;

public class MultiNotificationService implements NotificationService, MobilePushTestSender,
        WhatsAppTestSender {

    private final List<NotificationChannel> channels;

    public MultiNotificationService(List<NotificationChannel> channels) {
        this.channels = List.copyOf(channels);
    }

    @Override
    public void notifyPowerOutage(String deviceName, Instant outageAt) {
        channels.forEach(channel -> channel.notifyPowerOutage(deviceName, outageAt));
    }

    @Override
    public void notifyPowerOutage(UUID incidentId, String deviceName, Instant outageAt) {
        channels.forEach(channel -> channel.notifyPowerOutage(incidentId, deviceName, outageAt));
    }

    @Override
    public void notifyPowerOutagePushOnly(UUID incidentId, String deviceName, Instant outageAt) {
        channels.stream()
                .filter(PushNotificationChannel.class::isInstance)
                .forEach(channel -> channel.notifyPowerOutage(incidentId, deviceName, outageAt));
    }

    @Override
    public MobilePushTestResult sendTest(UUID userId, String token) {
        return channels.stream()
                .filter(MobilePushTestChannel.class::isInstance)
                .map(MobilePushTestChannel.class::cast)
                .findFirst()
                .map(channel -> channel.sendTest(userId, token))
                .orElseGet(MobilePushTestResult::unavailable);
    }

    @Override
    public WhatsAppTestResult sendWhatsAppTest(String phone, String label) {
        return channels.stream()
                .filter(WhatsAppTestChannel.class::isInstance)
                .map(WhatsAppTestChannel.class::cast)
                .findFirst()
                .map(channel -> channel.sendTest(phone, label))
                .orElseGet(WhatsAppTestResult::unavailable);
    }

    @Override
    public void notifyPowerRestored(String deviceName, Instant outageAt, Instant restoredAt) {
        channels.forEach(channel ->
                channel.notifyPowerRestored(deviceName, outageAt, restoredAt));
    }

    @Override
    public void notifyPowerRestored(
            UUID incidentId,
            String deviceName,
            Instant outageAt,
            Instant restoredAt
    ) {
        channels.forEach(channel ->
                channel.notifyPowerRestored(incidentId, deviceName, outageAt, restoredAt));
    }
}
