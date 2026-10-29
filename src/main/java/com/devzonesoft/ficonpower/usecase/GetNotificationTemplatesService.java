package com.devzonesoft.ficonpower.usecase;

import com.devzonesoft.ficonpower.domain.NotificationTemplate;
import com.devzonesoft.ficonpower.ports.input.GetNotificationTemplates;
import com.devzonesoft.ficonpower.ports.output.NotificationTemplateRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GetNotificationTemplatesService implements GetNotificationTemplates {

    private final NotificationTemplateRepository templates;

    public GetNotificationTemplatesService(NotificationTemplateRepository templates) {
        this.templates = templates;
    }

    @Override
    public List<NotificationTemplate> getAll() {
        return templates.findAll();
    }
}
