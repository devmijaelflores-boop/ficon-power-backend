package com.devzonesoft.ficonpower.web;

import com.devzonesoft.ficonpower.ports.input.GetNotificationTemplates;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notification-templates")
public class NotificationTemplateController {

    private final GetNotificationTemplates getTemplates;
    private final NotificationTemplateViewMapper viewMapper;

    public NotificationTemplateController(
            GetNotificationTemplates getTemplates,
            NotificationTemplateViewMapper viewMapper
    ) {
        this.getTemplates = getTemplates;
        this.viewMapper = viewMapper;
    }

    @GetMapping
    public List<NotificationTemplateView> templates() {
        return getTemplates.getAll().stream()
                .map(viewMapper::map)
                .toList();
    }
}
