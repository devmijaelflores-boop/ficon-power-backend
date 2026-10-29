package com.devzonesoft.ficonpower.web;

import com.devzonesoft.ficonpower.config.WhatsAppProperties;
import com.devzonesoft.ficonpower.domain.MetaTemplateParameters;
import com.devzonesoft.ficonpower.domain.NotificationEventType;
import com.devzonesoft.ficonpower.domain.NotificationTemplate;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class NotificationTemplateViewMapper {

    private final WhatsAppProperties whatsapp;

    public NotificationTemplateViewMapper(WhatsAppProperties whatsapp) {
        this.whatsapp = whatsapp;
    }

    public NotificationTemplateView map(NotificationTemplate template) {
        String templateName = template.eventType() == NotificationEventType.POWER_OUTAGE
                ? whatsapp.getOutageTemplateName()
                : whatsapp.getRestoredTemplateName();
        String templateParams = template.eventType() == NotificationEventType.POWER_OUTAGE
                ? whatsapp.getOutageTemplateParams()
                : whatsapp.getRestoredTemplateParams();
        return new NotificationTemplateView(
                template.eventType().code(),
                hasText(templateName) ? templateName : null,
                whatsapp.getTemplateLanguage(),
                MetaTemplateParameters.parse(templateParams),
                availableVariables(template.eventType()));
    }

    private List<TemplateVariableView> availableVariables(NotificationEventType eventType) {
        TemplateVariableView device = new TemplateVariableView(
                "dispositivo",
                "Nombre del dispositivo",
                "{dispositivo}",
                true,
                true,
                "Nombre visible del medidor o protector.");
        TemplateVariableView outageAt = new TemplateVariableView(
                "fechaCorte",
                "Fecha del corte",
                "{fechaCorte}",
                true,
                true,
                "Fecha y hora en que se detectó la pérdida de energía.");
        if (eventType == NotificationEventType.POWER_OUTAGE) {
            return List.of(device, outageAt);
        }
        return List.of(
                device,
                outageAt,
                new TemplateVariableView(
                        "fechaRetorno",
                        "Fecha de retorno",
                        "{fechaRetorno}",
                        true,
                        true,
                        "Fecha y hora en que volvió la energía."));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
