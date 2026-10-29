package com.devzonesoft.ficonpower.ports.input;

import com.devzonesoft.ficonpower.domain.NotificationTemplate;
import java.util.List;

public interface GetNotificationTemplates {

    List<NotificationTemplate> getAll();
}
