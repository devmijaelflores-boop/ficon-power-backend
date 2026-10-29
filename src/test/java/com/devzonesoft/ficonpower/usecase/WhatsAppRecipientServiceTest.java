package com.devzonesoft.ficonpower.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devzonesoft.ficonpower.domain.NotificationDeliveryStatus;
import com.devzonesoft.ficonpower.domain.WhatsAppRecipient;
import com.devzonesoft.ficonpower.domain.WhatsAppTestResult;
import com.devzonesoft.ficonpower.ports.output.NotificationDeliveryRepository;
import com.devzonesoft.ficonpower.ports.output.WhatsAppRecipientRepository;
import com.devzonesoft.ficonpower.ports.output.WhatsAppTestSender;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class WhatsAppRecipientServiceTest {

    private final WhatsAppRecipientRepository recipients =
            mock(WhatsAppRecipientRepository.class);
    private final WhatsAppTestSender sender = mock(WhatsAppTestSender.class);
    private final NotificationDeliveryRepository deliveries =
            mock(NotificationDeliveryRepository.class);
    private final WhatsAppRecipientService service =
            new WhatsAppRecipientService(recipients, sender, deliveries);

    @Test
    void normalizesInternationalPhoneWhenCreatingRecipient() {
        UUID id = UUID.randomUUID();
        WhatsAppRecipient stored = recipient(id, "51987654321");
        when(recipients.create("Operaciones", "51987654321", null, true, false, true))
                .thenReturn(stored);

        WhatsAppRecipient result = service.create(
                "  Operaciones  ", " +51987654321 ", null, true, false, true);

        assertEquals(stored, result);
        verify(recipients).create("Operaciones", "51987654321", null, true, false, true);
    }

    @Test
    void completesCountryCodeAndRemovesFormattingFromLocalPhone() {
        UUID id = UUID.randomUUID();
        WhatsAppRecipient stored = recipient(id, "51987654321");
        when(recipients.create("Operaciones", "51987654321", null, true, true, true))
                .thenReturn(stored);

        assertEquals(stored, service.create(
                "Operaciones", "987 654 321", null, true, true, true));
        verify(recipients).create("Operaciones", "51987654321", null, true, true, true);
    }

    @Test
    void acceptsCopiedPeruPhoneWithPrefixAndSeparators() {
        UUID id = UUID.randomUUID();
        WhatsAppRecipient stored = recipient(id, "51987654321");
        when(recipients.create("Operaciones", "51987654321", null, true, true, true))
                .thenReturn(stored);

        assertEquals(stored, service.create(
                "Operaciones", "+51 987-654-321", null, true, true, true));
    }

    @Test
    void rejectsPhoneOutsidePeruMobileFormat() {
        assertThrows(ResponseStatusException.class, () -> service.create(
                "Operaciones", "+1 202 555 0123", null, true, true, true));
    }

    @Test
    void rejectsRecipientWithoutSelectedEvents() {
        assertThrows(ResponseStatusException.class, () -> service.create(
                "Operaciones", "51987654321", null, false, false, true));
    }

    @Test
    void recordsResultOfManualTest() {
        UUID id = UUID.randomUUID();
        when(recipients.findById(id)).thenReturn(Optional.of(recipient(id, "51987654321")));
        WhatsAppTestResult accepted =
                new WhatsAppTestResult(true, true, "Mensaje de prueba aceptado por Meta.");
        when(sender.sendWhatsAppTest("51987654321", "Operaciones")).thenReturn(accepted);

        assertEquals(accepted, service.test(id));
        verify(recipients).recordTest(id, true, accepted.message());
    }

    @Test
    void recordsUserTestInDeliveryHistory() {
        UUID recipientId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        WhatsAppRecipient linked = new WhatsAppRecipient(
                recipientId, userId, "Operaciones", "51987654321", null, null,
                true, true, true, null, null, null, Instant.now(), Instant.now());
        WhatsAppTestResult accepted =
                new WhatsAppTestResult(true, true, "Mensaje de prueba aceptado por Meta.");
        when(recipients.findByUserId(userId)).thenReturn(Optional.of(linked));
        when(sender.sendWhatsAppTest(linked.phone(), linked.label())).thenReturn(accepted);

        assertEquals(accepted, service.testUser(userId));
        verify(deliveries).save(org.mockito.ArgumentMatchers.argThat(delivery ->
                delivery.userId().equals(userId)
                        && delivery.recipient().equals("51987654321")
                        && delivery.status() == NotificationDeliveryStatus.SENT));
    }

    private WhatsAppRecipient recipient(UUID id, String phone) {
        return new WhatsAppRecipient(
                id, "Operaciones", phone, null, null, true, false, true,
                null, null, null, Instant.now(), Instant.now());
    }
}
