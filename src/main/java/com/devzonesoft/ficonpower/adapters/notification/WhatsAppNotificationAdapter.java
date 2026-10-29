package com.devzonesoft.ficonpower.adapters.notification;

import com.devzonesoft.ficonpower.domain.MetaTemplateParameters;
import com.devzonesoft.ficonpower.domain.NotificationDelivery;
import com.devzonesoft.ficonpower.domain.NotificationDeliveryStatus;
import com.devzonesoft.ficonpower.domain.NotificationEventType;
import com.devzonesoft.ficonpower.domain.WhatsAppRecipient;
import com.devzonesoft.ficonpower.domain.WhatsAppTestResult;
import com.devzonesoft.ficonpower.ports.output.NotificationDeliveryRepository;
import com.devzonesoft.ficonpower.ports.output.WhatsAppRecipientRepository;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

public class WhatsAppNotificationAdapter implements NotificationChannel, WhatsAppTestChannel {

    private static final Logger logger = LoggerFactory.getLogger(WhatsAppNotificationAdapter.class);

    private final RestClient restClient;
    private final String fallbackPhone;
    private final WhatsAppRecipientRepository recipients;
    private final NotificationDeliveryRepository deliveries;
    private final String defaultTemplateName;
    private final String templateLanguage;
    private final String outageTemplateName;
    private final List<String> outageTemplateParams;
    private final String restoredTemplateName;
    private final List<String> restoredTemplateParams;
    private final DateTimeFormatter dateTimeFormatter;

    public WhatsAppNotificationAdapter(
            RestClient.Builder builder,
            String apiUrl,
            String phone,
            String token,
            WhatsAppRecipientRepository recipients,
            String templateName,
            String templateLanguage,
            String outageTemplateName,
            String outageTemplateParams,
            String restoredTemplateName,
            String restoredTemplateParams,
            ZoneId timeZone,
            NotificationDeliveryRepository deliveries
    ) {
        this.restClient = builder.baseUrl(apiUrl)
                .defaultHeader("Authorization", "Bearer " + token)
                .build();
        this.fallbackPhone = blankToNull(phone);
        this.recipients = recipients;
        this.deliveries = deliveries;
        this.defaultTemplateName = blankToNull(templateName);
        this.templateLanguage = StringUtils.hasText(templateLanguage) ? templateLanguage : "en_US";
        this.outageTemplateName = blankToNull(outageTemplateName);
        this.outageTemplateParams = MetaTemplateParameters.parse(outageTemplateParams);
        this.restoredTemplateName = blankToNull(restoredTemplateName);
        this.restoredTemplateParams = MetaTemplateParameters.parse(restoredTemplateParams);
        this.dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
                .withZone(timeZone);
    }

    public WhatsAppNotificationAdapter(
            RestClient.Builder builder,
            String apiUrl,
            String phone,
            String token,
            WhatsAppRecipientRepository recipients,
            String templateName,
            String templateLanguage,
            String outageTemplateName,
            String outageTemplateParams,
            String restoredTemplateName,
            String restoredTemplateParams,
            ZoneId timeZone
    ) {
        this(builder, apiUrl, phone, token, recipients, templateName, templateLanguage,
                outageTemplateName, outageTemplateParams, restoredTemplateName,
                restoredTemplateParams, timeZone, null);
    }

    @Override
    public void notifyPowerOutage(String deviceName, Instant outageAt) {
        notifyPowerOutage(null, deviceName, outageAt);
    }

    @Override
    public void notifyPowerOutage(UUID incidentId, String deviceName, Instant outageAt) {
        Map<String, String> values = Map.of(
                "dispositivo", deviceName,
                "fechaCorte", dateTimeFormatter.format(outageAt)
        );
        sendNotification(incidentId, NotificationEventType.POWER_OUTAGE, values);
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
        Map<String, String> values = Map.of(
                "dispositivo", deviceName,
                "fechaCorte", dateTimeFormatter.format(outageAt),
                "fechaRetorno", dateTimeFormatter.format(restoredAt)
        );
        sendNotification(incidentId, NotificationEventType.POWER_RESTORED, values);
    }

    @Override
    public WhatsAppTestResult sendTest(String phone, String label) {
        String templateName = metaTemplateNameFor(NotificationEventType.POWER_OUTAGE);
        if (!StringUtils.hasText(templateName)) {
            return new WhatsAppTestResult(
                    true, false, "No hay una plantilla de corte configurada en Meta.");
        }
        Map<String, String> values = Map.of(
                "dispositivo", "Prueba FICON - " + label,
                "fechaCorte", dateTimeFormatter.format(Instant.now()));
        try {
            sendPayload(phone, templatePayload(templateName,
                    paramsFor(NotificationEventType.POWER_OUTAGE, values)));
            return new WhatsAppTestResult(true, true, "Mensaje de prueba aceptado por Meta.");
        } catch (RuntimeException exception) {
            logger.warn("WhatsApp test failed for recipient {}: {}", phone, exception.getMessage());
            return new WhatsAppTestResult(true, false, "Meta rechazó el mensaje de prueba.");
        }
    }

    private void sendNotification(
            UUID incidentId,
            NotificationEventType eventType,
            Map<String, String> values
    ) {
        String templateName = metaTemplateNameFor(eventType);
        if (!StringUtils.hasText(templateName)) {
            logger.warn("WhatsApp Meta template not configured for event {}; notification skipped",
                    eventType.code());
            return;
        }
        Map<String, WhatsAppRecipient> targets = new LinkedHashMap<>();
        recipients.findActiveForIncident(incidentId, eventType).forEach(recipient ->
                targets.merge(recipient.phone(), recipient,
                        (current, candidate) -> current.userId() != null ? current : candidate));
        if (targets.isEmpty() && fallbackPhone != null && !recipients.hasAny()) {
            targets.put(fallbackPhone, null);
        }
        if (targets.isEmpty()) {
            logger.info("No active WhatsApp recipients for incident {} and event {}",
                    incidentId, eventType.code());
            return;
        }
        Map<String, Object> payload = templatePayload(
                templateName, paramsFor(eventType, values));
        for (Map.Entry<String, WhatsAppRecipient> target : targets.entrySet()) {
            String phone = target.getKey();
            UUID userId = target.getValue() == null ? null : target.getValue().userId();
            try {
                sendPayload(phone, payload);
                recordDelivery(incidentId, userId, phone, eventType, templateName,
                        NotificationDeliveryStatus.SENT, null);
            } catch (RuntimeException exception) {
                recordDelivery(incidentId, userId, phone, eventType, templateName,
                        NotificationDeliveryStatus.FAILED, safeError(exception));
                logger.warn("WhatsApp delivery failed for recipient {} and event {}: {}",
                        phone, eventType.code(), exception.getMessage());
            }
        }
    }

    private void recordDelivery(
            UUID incidentId,
            UUID userId,
            String phone,
            NotificationEventType eventType,
            String templateName,
            NotificationDeliveryStatus status,
            String errorMessage
    ) {
        if (deliveries == null) return;
        try {
            deliveries.save(NotificationDelivery.whatsapp(
                    incidentId, userId, phone, eventType, templateName, status, errorMessage));
        } catch (RuntimeException exception) {
            logger.warn("Could not persist WhatsApp delivery history for incident {}: {}",
                    incidentId, exception.getMessage());
        }
    }

    private String safeError(RuntimeException exception) {
        String message = exception.getMessage();
        if (!StringUtils.hasText(message)) return "Meta rechazó el envío";
        return message.length() <= 500 ? message : message.substring(0, 500);
    }

    private String metaTemplateNameFor(NotificationEventType eventType) {
        return switch (eventType) {
            case POWER_OUTAGE -> outageTemplateName != null ? outageTemplateName : defaultTemplateName;
            case POWER_RESTORED -> restoredTemplateName != null ? restoredTemplateName : defaultTemplateName;
        };
    }

    private List<String> paramsFor(NotificationEventType eventType, Map<String, String> values) {
        List<String> configured = switch (eventType) {
            case POWER_OUTAGE -> outageTemplateParams;
            case POWER_RESTORED -> restoredTemplateParams;
        };
        return configured.stream()
                .map(key -> values.getOrDefault(key, ""))
                .toList();
    }

    private void sendPayload(String phone, Map<String, Object> payload) {
        restClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(withRecipient(payload, phone))
                .retrieve()
                .toBodilessEntity();
    }

    private Map<String, Object> templatePayload(String name, List<String> params) {
        Map<String, Object> template = params.isEmpty()
                ? Map.of(
                        "name", name,
                        "language", Map.of("code", templateLanguage)
                )
                : Map.of(
                        "name", name,
                        "language", Map.of("code", templateLanguage),
                        "components", List.of(Map.of(
                                "type", "body",
                                "parameters", params.stream()
                                        .map(value -> Map.of(
                                                "type", "text",
                                                "text", value
                                        ))
                                        .toList()
                        ))
                );
        return Map.of(
                "messaging_product", "whatsapp",
                "type", "template",
                "template", template
        );
    }

    private Map<String, Object> withRecipient(Map<String, Object> payload, String phone) {
        java.util.HashMap<String, Object> result = new java.util.HashMap<>(payload);
        result.put("to", phone);
        return Map.copyOf(result);
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }
}
