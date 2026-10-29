package com.devzonesoft.ficonpower.adapters.notification;

import com.devzonesoft.ficonpower.config.FcmProperties;
import com.devzonesoft.ficonpower.domain.MobileDeviceToken;
import com.devzonesoft.ficonpower.domain.MobilePushTestResult;
import com.devzonesoft.ficonpower.domain.NotificationDelivery;
import com.devzonesoft.ficonpower.domain.NotificationDeliveryStatus;
import com.devzonesoft.ficonpower.domain.NotificationEventType;
import com.devzonesoft.ficonpower.ports.output.MobileDeviceTokenRepository;
import com.devzonesoft.ficonpower.ports.output.NotificationDeliveryRepository;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

public class FcmPushNotificationAdapter implements MobilePushTestChannel {

    private static final Logger logger = LoggerFactory.getLogger(FcmPushNotificationAdapter.class);
    private static final String POWER_ALERT_CHANNEL_ID = "fcm_fallback_notification_channel";

    private final RestClient restClient;
    private final FcmAccessTokenProvider accessTokens;
    private final MobileDeviceTokenRepository tokens;
    private final NotificationDeliveryRepository deliveries;
    private final boolean dryRun;
    private final DateTimeFormatter dateTimeFormatter;

    public FcmPushNotificationAdapter(
            RestClient.Builder builder,
            FcmProperties properties,
            FcmAccessTokenProvider accessTokens,
            MobileDeviceTokenRepository tokens,
            NotificationDeliveryRepository deliveries,
            ZoneId timeZone
    ) {
        this.restClient = builder.baseUrl(
                "https://fcm.googleapis.com/v1/projects/%s".formatted(properties.getProjectId()))
                .build();
        this.accessTokens = accessTokens;
        this.tokens = tokens;
        this.deliveries = deliveries;
        this.dryRun = properties.isDryRun();
        this.dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
                .withZone(timeZone);
    }

    @Override
    public void notifyPowerOutage(String deviceName, Instant outageAt) {
        notifyPowerOutage(null, deviceName, outageAt);
    }

    @Override
    public void notifyPowerOutage(UUID incidentId, String deviceName, Instant outageAt) {
        sendToActiveTokens(
                incidentId,
                NotificationEventType.POWER_OUTAGE,
                "Corte de energía",
                "%s quedó fuera de línea a las %s".formatted(
                        deviceName, dateTimeFormatter.format(outageAt)),
                withIncidentId(incidentId, Map.of(
                        "eventType", NotificationEventType.POWER_OUTAGE.code(),
                        "deviceName", deviceName,
                        "outageAt", outageAt.toString())));
    }

    @Override
    public void notifyPowerRestored(String deviceName, Instant outageAt, Instant restoredAt) {
        notifyPowerRestored(null, deviceName, outageAt, restoredAt);
    }

    @Override
    public void notifyPowerRestored(
            UUID incidentId,
            String deviceName,
            Instant outageAt,
            Instant restoredAt
    ) {
        sendToActiveTokens(
                incidentId,
                NotificationEventType.POWER_RESTORED,
                "Retorno de energía",
                "%s volvió en línea a las %s".formatted(
                        deviceName, dateTimeFormatter.format(restoredAt)),
                withIncidentId(incidentId, Map.of(
                        "eventType", NotificationEventType.POWER_RESTORED.code(),
                        "deviceName", deviceName,
                        "outageAt", outageAt.toString(),
                        "restoredAt", restoredAt.toString())));
    }

    @Override
    public MobilePushTestResult sendTest(UUID userId, String requestedToken) {
        MobileDeviceToken token = tokens.findActive().stream()
                .filter(candidate -> candidate.userId().equals(userId))
                .filter(candidate -> candidate.token().equals(requestedToken))
                .findFirst()
                .orElse(null);
        if (token == null) {
            return MobilePushTestResult.tokenNotRegistered();
        }
        if (dryRun) {
            return new MobilePushTestResult(
                    true,
                    true,
                    false,
                    "FCM_DRY_RUN está activo; Firebase solo validaría el mensaje.");
        }

        try {
            sendRequest(
                    token.token(),
                    "Prueba FICON Power",
                    "El registro FCM y el sonido de alertas funcionan correctamente.",
                    Map.of("eventType", "push_test"));
            return new MobilePushTestResult(
                    true, true, true, "Firebase aceptó la notificación de prueba.");
        } catch (RestClientResponseException e) {
            revokeInvalidToken(token, e.getResponseBodyAsString());
            logger.warn("FCM test push failed for user {}: {}", userId, e.getResponseBodyAsString());
            return new MobilePushTestResult(
                    true, true, false, "Firebase rechazó la notificación de prueba.");
        } catch (RuntimeException e) {
            logger.warn("FCM test push failed for user {}: {}", userId, e.getMessage());
            return new MobilePushTestResult(
                    true, true, false, "No se pudo contactar a Firebase.");
        }
    }

    private void sendToActiveTokens(
            UUID incidentId,
            NotificationEventType eventType,
            String title,
            String body,
            Map<String, String> data
    ) {
        List<MobileDeviceToken> activeTokens = tokens.findActive();
        UUID notificationKey = notificationKey(incidentId, eventType);
        if (activeTokens.isEmpty()) {
            deliveries.save(NotificationDelivery.push(
                    incidentId,
                    notificationKey,
                    null,
                    null,
                    eventType,
                    NotificationDeliveryStatus.SKIPPED,
                    null,
                    "no_active_tokens",
                    "No hay tokens móviles activos para enviar push.",
                    title,
                    body,
                    data));
            return;
        }
        activeTokens.forEach(token -> sendOne(
                incidentId, notificationKey, token, eventType, title, body, data));
    }

    private void sendOne(
            UUID incidentId,
            UUID notificationKey,
            MobileDeviceToken token,
            NotificationEventType eventType,
            String title,
            String body,
            Map<String, String> data
    ) {
        try {
            FcmSendResponse response = sendRequest(token.token(), title, body, data);
            String messageId = response == null ? null : response.name();
            deliveries.save(NotificationDelivery.push(
                    incidentId,
                    notificationKey,
                    token.userId(),
                    token.token(),
                    eventType,
                    NotificationDeliveryStatus.SENT,
                    messageId,
                    null,
                    null,
                    title,
                    body,
                    data));
        } catch (RestClientResponseException e) {
            logger.warn("FCM push failed for user {}: {}", token.userId(), e.getResponseBodyAsString());
            revokeInvalidToken(token, e.getResponseBodyAsString());
            deliveries.save(NotificationDelivery.push(
                    incidentId,
                    notificationKey,
                    token.userId(),
                    token.token(),
                    eventType,
                    NotificationDeliveryStatus.FAILED,
                    null,
                    String.valueOf(e.getStatusCode().value()),
                    e.getResponseBodyAsString(),
                    title,
                    body,
                    data));
        } catch (RuntimeException e) {
            logger.warn("FCM push failed for user {}: {}", token.userId(), e.getMessage());
            deliveries.save(NotificationDelivery.push(
                    incidentId,
                    notificationKey,
                    token.userId(),
                    token.token(),
                    eventType,
                    NotificationDeliveryStatus.FAILED,
                    null,
                    e.getClass().getSimpleName(),
                    e.getMessage(),
                    title,
                    body,
                    data));
        }
    }

    private FcmSendResponse sendRequest(
            String token,
            String title,
            String body,
            Map<String, String> data
    ) {
        return restClient.post()
                .uri(uri -> uri.path("/messages:send")
                        .queryParam("validate_only", dryRun)
                        .build())
                .headers(headers -> headers.setBearerAuth(accessTokens.accessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload(token, title, body, data))
                .retrieve()
                .body(FcmSendResponse.class);
    }

    private void revokeInvalidToken(MobileDeviceToken token, String responseBody) {
        if (responseBody == null || !responseBody.contains("UNREGISTERED")) {
            return;
        }
        tokens.revoke(token.userId(), token.token());
        logger.info("Revoked unregistered FCM token for user {}", token.userId());
    }

    private Map<String, Object> payload(
            String token,
            String title,
            String body,
            Map<String, String> data
    ) {
        return Map.of("message", Map.of(
                "token", token,
                "notification", Map.of(
                        "title", title,
                        "body", body),
                "data", data,
                "android", Map.of(
                        "priority", "HIGH",
                        "notification", Map.of(
                                "channel_id", POWER_ALERT_CHANNEL_ID,
                                "sound", "power_alarm",
                                "tag", notificationTag(data),
                                "default_vibrate_timings", true))));
    }

    private UUID notificationKey(UUID incidentId, NotificationEventType eventType) {
        if (incidentId == null) {
            return UUID.randomUUID();
        }
        return UUID.nameUUIDFromBytes(
                "%s:%s".formatted(eventType.code(), incidentId).getBytes(StandardCharsets.UTF_8));
    }

    private String notificationTag(Map<String, String> data) {
        String incidentId = data.get("incidentId");
        String eventType = data.getOrDefault("eventType", "power_alert");
        return incidentId == null ? eventType : "%s:%s".formatted(eventType, incidentId);
    }

    private Map<String, String> withIncidentId(UUID incidentId, Map<String, String> data) {
        if (incidentId == null) {
            return data;
        }
        java.util.HashMap<String, String> enriched = new java.util.HashMap<>(data);
        enriched.put("incidentId", incidentId.toString());
        return Map.copyOf(enriched);
    }

    private record FcmSendResponse(String name) {
    }
}
