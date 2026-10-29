package com.devzonesoft.ficonpower.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.devzonesoft.ficonpower.config.WhatsAppProperties;
import com.devzonesoft.ficonpower.domain.NotificationEventType;
import com.devzonesoft.ficonpower.domain.NotificationTemplate;
import java.util.List;
import org.junit.jupiter.api.Test;

class NotificationTemplateViewMapperTest {

    private final WhatsAppProperties whatsapp = properties();
    private final NotificationTemplateViewMapper mapper =
            new NotificationTemplateViewMapper(whatsapp);

    @Test
    void mapsOutageTemplateMetadata() {
        NotificationTemplateView view = mapper.map(
                NotificationTemplate.defaultFor(NotificationEventType.POWER_OUTAGE));

        assertEquals("power_outage", view.eventType());
        assertEquals("power_outage_alert", view.metaTemplateName());
        assertEquals("es", view.metaTemplateLanguage());
        assertEquals(List.of("dispositivo", "fechaCorte"), view.metaTemplateParams());
        assertEquals(2, view.availableVariables().size());
    }

    @Test
    void mapsRestoredTemplateMetadata() {
        NotificationTemplateView view = mapper.map(
                NotificationTemplate.defaultFor(NotificationEventType.POWER_RESTORED));

        assertEquals("power_restored_alert", view.metaTemplateName());
        assertEquals(
                List.of("dispositivo", "fechaCorte", "fechaRetorno"),
                view.metaTemplateParams());
        assertEquals(3, view.availableVariables().size());
    }

    @Test
    void exposesNoTemplateNameWhenTheEventTemplateIsBlank() {
        whatsapp.setOutageTemplateName("  ");

        NotificationTemplateView view = mapper.map(
                NotificationTemplate.defaultFor(NotificationEventType.POWER_OUTAGE));

        assertNull(view.metaTemplateName());
    }

    private WhatsAppProperties properties() {
        WhatsAppProperties properties = new WhatsAppProperties();
        properties.setTemplateLanguage("es");
        properties.setOutageTemplateName("power_outage_alert");
        properties.setOutageTemplateParams("device, outage_at");
        properties.setRestoredTemplateName("power_restored_alert");
        properties.setRestoredTemplateParams("dispositivo, fecha_corte, restored_at");
        return properties;
    }
}
