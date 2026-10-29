package com.devzonesoft.ficonpower.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.devzonesoft.ficonpower.domain.NotificationEventType;
import com.devzonesoft.ficonpower.domain.NotificationTemplate;
import com.devzonesoft.ficonpower.ports.input.GetNotificationTemplates;
import java.util.List;
import org.junit.jupiter.api.Test;

class NotificationTemplateControllerTest {

    @Test
    void exposesTheSameCanonicalParametersUsedByWhatsApp() {
        GetNotificationTemplates templates = mock(GetNotificationTemplates.class);
        when(templates.getAll()).thenReturn(List.of(
                NotificationTemplate.defaultFor(NotificationEventType.POWER_OUTAGE),
                NotificationTemplate.defaultFor(NotificationEventType.POWER_RESTORED)));
        NotificationTemplateViewMapper mapper = mock(NotificationTemplateViewMapper.class);
        NotificationTemplateView outageView = new NotificationTemplateView(
                "power_outage", "power_outage_alert", "es", List.of(), List.of());
        NotificationTemplateView restoredView = new NotificationTemplateView(
                "power_restored", "power_restored_alert", "es", List.of(), List.of());
        when(mapper.map(NotificationTemplate.defaultFor(NotificationEventType.POWER_OUTAGE)))
                .thenReturn(outageView);
        when(mapper.map(NotificationTemplate.defaultFor(NotificationEventType.POWER_RESTORED)))
                .thenReturn(restoredView);
        NotificationTemplateController controller =
                new NotificationTemplateController(templates, mapper);

        List<NotificationTemplateView> views = controller.templates();

        assertEquals(List.of(outageView, restoredView), views);
    }
}
