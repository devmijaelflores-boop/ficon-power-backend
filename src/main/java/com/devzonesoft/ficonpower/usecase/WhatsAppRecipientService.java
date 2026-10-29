package com.devzonesoft.ficonpower.usecase;

import com.devzonesoft.ficonpower.domain.WhatsAppRecipient;
import com.devzonesoft.ficonpower.domain.WhatsAppTestResult;
import com.devzonesoft.ficonpower.domain.NotificationDelivery;
import com.devzonesoft.ficonpower.domain.NotificationDeliveryStatus;
import com.devzonesoft.ficonpower.ports.output.NotificationDeliveryRepository;
import com.devzonesoft.ficonpower.ports.output.WhatsAppRecipientRepository;
import com.devzonesoft.ficonpower.ports.output.WhatsAppTestSender;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WhatsAppRecipientService {

    private static final Pattern PERU_MOBILE = Pattern.compile("^519[0-9]{8}$");

    private final WhatsAppRecipientRepository recipients;
    private final WhatsAppTestSender testSender;
    private final NotificationDeliveryRepository deliveries;

    public WhatsAppRecipientService(
            WhatsAppRecipientRepository recipients,
            WhatsAppTestSender testSender,
            NotificationDeliveryRepository deliveries
    ) {
        this.recipients = recipients;
        this.testSender = testSender;
        this.deliveries = deliveries;
    }

    public List<WhatsAppRecipient> findAll() {
        return recipients.findAll();
    }

    public WhatsAppRecipient create(String label, String phone, UUID siteId,
            boolean notifyOutage, boolean notifyRestored, boolean active) {
        validate(label, phone, notifyOutage, notifyRestored);
        return recipients.create(label.trim(), normalizePhone(phone), siteId,
                notifyOutage, notifyRestored, active);
    }

    public WhatsAppRecipient update(UUID id, String label, String phone, UUID siteId,
            boolean notifyOutage, boolean notifyRestored, boolean active) {
        validate(label, phone, notifyOutage, notifyRestored);
        return recipients.update(id, label.trim(), normalizePhone(phone), siteId,
                        notifyOutage, notifyRestored, active)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Destinatario no encontrado"));
    }

    public void delete(UUID id) {
        if (!recipients.delete(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Destinatario no encontrado");
        }
    }

    public WhatsAppTestResult test(UUID id) {
        WhatsAppRecipient recipient = recipients.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Destinatario no encontrado"));
        WhatsAppTestResult result = testSender.sendWhatsAppTest(
                recipient.phone(), recipient.label());
        recipients.recordTest(id, result.accepted(), result.message());
        return result;
    }

    public WhatsAppTestResult testUser(UUID userId) {
        WhatsAppRecipient recipient = recipients.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "El usuario no tiene un número de WhatsApp registrado"));
        WhatsAppTestResult result = testSender.sendWhatsAppTest(
                recipient.phone(), recipient.label());
        NotificationDeliveryStatus status = !result.configured()
                ? NotificationDeliveryStatus.SKIPPED
                : result.accepted()
                        ? NotificationDeliveryStatus.SENT
                        : NotificationDeliveryStatus.FAILED;
        deliveries.save(NotificationDelivery.whatsapp(
                null,
                userId,
                recipient.phone(),
                null,
                null,
                status,
                result.accepted() ? null : result.message()));
        return result;
    }

    private void validate(String label, String phone, boolean outage, boolean restored) {
        if (label == null || label.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre es obligatorio");
        }
        if (label.trim().length() > 120) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "El nombre no puede superar los 120 caracteres");
        }
        if (!PERU_MOBILE.matcher(normalizePhone(phone)).matches()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ingresa un celular peruano de 9 dígitos, por ejemplo 987 654 321");
        }
        if (!outage && !restored) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Selecciona al menos un tipo de alerta");
        }
    }

    public static String normalizePhone(String phone) {
        if (phone == null) return "";
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.startsWith("0051")) digits = digits.substring(2);
        if (digits.matches("^9[0-9]{8}$")) return "51" + digits;
        return digits;
    }

    public static String requirePeruMobile(String phone) {
        String normalized = normalizePhone(phone);
        if (!PERU_MOBILE.matcher(normalized).matches()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ingresa un celular peruano de 9 dígitos, por ejemplo 987 654 321");
        }
        return normalized;
    }
}
