package com.devzonesoft.ficonpower.adapters.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.devzonesoft.ficonpower.domain.NotificationDelivery;
import com.devzonesoft.ficonpower.domain.NotificationDeliveryStatus;
import com.devzonesoft.ficonpower.domain.NotificationEventType;
import com.devzonesoft.ficonpower.domain.WhatsAppRecipient;
import com.devzonesoft.ficonpower.ports.output.NotificationDeliveryRepository;
import com.devzonesoft.ficonpower.ports.output.WhatsAppRecipientRepository;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class WhatsAppNotificationAdapterTest {

    @Test
    void recordsTheDeliveryAgainstTheLinkedUser() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        WhatsAppRecipientRepository recipients = mock(WhatsAppRecipientRepository.class);
        NotificationDeliveryRepository deliveries = mock(NotificationDeliveryRepository.class);
        UUID incidentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        WhatsAppRecipient recipient = new WhatsAppRecipient(
                UUID.randomUUID(), userId, "Responsable", "51911111111", null, null,
                true, true, true, null, null, null, Instant.now(), Instant.now());
        when(recipients.findActiveForIncident(incidentId, NotificationEventType.POWER_OUTAGE))
                .thenReturn(List.of(recipient));
        WhatsAppNotificationAdapter adapter = new WhatsAppNotificationAdapter(
                builder, "https://graph.facebook.test/messages", "", "token",
                recipients, "power_alert", "es", "", "", "", "",
                ZoneId.of("America/Lima"), deliveries);
        server.expect(requestTo("https://graph.facebook.test/messages"))
                .andRespond(withSuccess());

        adapter.notifyPowerOutage(
                incidentId, "Medidor", Instant.parse("2026-07-22T12:00:00Z"));

        ArgumentCaptor<NotificationDelivery> captor =
                ArgumentCaptor.forClass(NotificationDelivery.class);
        verify(deliveries).save(captor.capture());
        assertEquals(userId, captor.getValue().userId());
        assertEquals(NotificationDeliveryStatus.SENT, captor.getValue().status());
        server.verify();
    }

    @Test
    void sendsAnIncidentToAllMatchingDatabaseRecipients() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        WhatsAppRecipientRepository recipients = mock(WhatsAppRecipientRepository.class);
        UUID incidentId = UUID.randomUUID();
        when(recipients.findActiveForIncident(incidentId, NotificationEventType.POWER_OUTAGE))
                .thenReturn(List.of(recipient("51911111111"), recipient("51922222222")));
        WhatsAppNotificationAdapter adapter = new WhatsAppNotificationAdapter(
                builder, "https://graph.facebook.test/messages", "51999999999", "token",
                recipients, "power_alert", "es", "", "", "", "",
                ZoneId.of("America/Lima"));

        server.expect(requestTo("https://graph.facebook.test/messages"))
                .andExpect(content().json("{\"to\":\"51911111111\"}"))
                .andRespond(withSuccess());
        server.expect(requestTo("https://graph.facebook.test/messages"))
                .andExpect(content().json("{\"to\":\"51922222222\"}"))
                .andRespond(withSuccess());

        adapter.notifyPowerOutage(
                incidentId, "Medidor", Instant.parse("2026-07-22T12:00:00Z"));

        server.verify();
    }

    @Test
    void doesNotUseLegacyPhoneAfterDatabaseConfigurationExists() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        WhatsAppRecipientRepository recipients = mock(WhatsAppRecipientRepository.class);
        UUID incidentId = UUID.randomUUID();
        when(recipients.findActiveForIncident(incidentId, NotificationEventType.POWER_OUTAGE))
                .thenReturn(List.of());
        when(recipients.hasAny()).thenReturn(true);
        WhatsAppNotificationAdapter adapter = new WhatsAppNotificationAdapter(
                builder, "https://graph.facebook.test/messages", "51999999999", "token",
                recipients, "power_alert", "es", "", "", "", "",
                ZoneId.of("America/Lima"));

        adapter.notifyPowerOutage(
                incidentId, "Medidor", Instant.parse("2026-07-22T12:00:00Z"));

        server.verify();
    }

    @Test
    void skipsNotificationWhenNoMetaTemplateIsConfigured() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        WhatsAppNotificationAdapter adapter = newAdapterWithoutTemplates(builder);

        adapter.notifyPowerOutage("Medidor principal", Instant.parse("2026-07-22T12:00:00Z"));

        server.verify();
    }

    @Test
    void sendsMetaTemplateWithoutParametersWhenLegacyTemplateNameIsConfigured() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        WhatsAppNotificationAdapter adapter = newAdapterWithTemplateName(builder);

        server.expect(requestTo("https://graph.facebook.test/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "messaging_product": "whatsapp",
                          "to": "51999999999",
                          "type": "template",
                          "template": {
                            "name": "approved_meta_template",
                            "language": {
                              "code": "en_US"
                            }
                          }
                        }
                        """))
                .andRespond(withSuccess());

        adapter.notifyPowerOutage("Medidor principal", Instant.parse("2026-07-22T12:00:00Z"));

        server.verify();
    }

    @Test
    void sendsOutageMetaTemplateWithConfiguredBodyParameters() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        WhatsAppNotificationAdapter adapter = newAdapterWithEventTemplates(builder);

        server.expect(requestTo("https://graph.facebook.test/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "messaging_product": "whatsapp",
                          "to": "51999999999",
                          "type": "template",
                          "template": {
                            "name": "power_outage_alert",
                            "language": {
                              "code": "es"
                            },
                            "components": [
                              {
                                "type": "body",
                                "parameters": [
                                  {
                                    "type": "text",
                                    "text": "Medidor principal"
                                  },
                                  {
                                    "type": "text",
                                    "text": "22/07/2026 07:00:00"
                                  }
                                ]
                              }
                            ]
                          }
                        }
                        """))
                .andRespond(withSuccess());

        adapter.notifyPowerOutage("Medidor principal", Instant.parse("2026-07-22T12:00:00Z"));

        server.verify();
    }

    @Test
    void sendsRestoredMetaTemplateWithConfiguredBodyParameters() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        WhatsAppNotificationAdapter adapter = newAdapterWithEventTemplates(builder);

        server.expect(requestTo("https://graph.facebook.test/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "messaging_product": "whatsapp",
                          "to": "51999999999",
                          "type": "template",
                          "template": {
                            "name": "power_restored_alert",
                            "language": {
                              "code": "es"
                            },
                            "components": [
                              {
                                "type": "body",
                                "parameters": [
                                  {
                                    "type": "text",
                                    "text": "Medidor principal"
                                  },
                                  {
                                    "type": "text",
                                    "text": "22/07/2026 07:00:00"
                                  },
                                  {
                                    "type": "text",
                                    "text": "22/07/2026 07:05:00"
                                  }
                                ]
                              }
                            ]
                          }
                        }
                        """))
                .andRespond(withSuccess());

        adapter.notifyPowerRestored(
                "Medidor principal",
                Instant.parse("2026-07-22T12:00:00Z"),
                Instant.parse("2026-07-22T12:05:00Z"));

        server.verify();
    }

    private WhatsAppNotificationAdapter newAdapterWithoutTemplates(RestClient.Builder builder) {
        return new WhatsAppNotificationAdapter(
                builder,
                "https://graph.facebook.test/messages",
                "51999999999",
                "test-token",
                mock(WhatsAppRecipientRepository.class),
                "",
                "en_US",
                "",
                "",
                "",
                "",
                ZoneId.of("America/Lima")
        );
    }

    private WhatsAppNotificationAdapter newAdapterWithTemplateName(RestClient.Builder builder) {
        return new WhatsAppNotificationAdapter(
                builder,
                "https://graph.facebook.test/messages",
                "51999999999",
                "test-token",
                mock(WhatsAppRecipientRepository.class),
                "approved_meta_template",
                "en_US",
                "",
                "",
                "",
                "",
                ZoneId.of("America/Lima")
        );
    }

    private WhatsAppNotificationAdapter newAdapterWithEventTemplates(RestClient.Builder builder) {
        return new WhatsAppNotificationAdapter(
                builder,
                "https://graph.facebook.test/messages",
                "51999999999",
                "test-token",
                mock(WhatsAppRecipientRepository.class),
                "",
                "es",
                "power_outage_alert",
                "dispositivo,fechaCorte",
                "power_restored_alert",
                "dispositivo,fechaCorte,fechaRetorno",
                ZoneId.of("America/Lima")
        );
    }

    private WhatsAppRecipient recipient(String phone) {
        return new WhatsAppRecipient(
                UUID.randomUUID(), "Responsable", phone, null, null,
                true, true, true, null, null, null, Instant.now(), Instant.now());
    }
}
