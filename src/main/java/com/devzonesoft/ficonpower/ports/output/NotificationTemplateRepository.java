package com.devzonesoft.ficonpower.ports.output;

import com.devzonesoft.ficonpower.domain.NotificationEventType;
import com.devzonesoft.ficonpower.domain.NotificationTemplate;
import java.util.List;
import java.util.Optional;

public interface NotificationTemplateRepository {

    List<NotificationTemplate> findAll();

    Optional<NotificationTemplate> findByEventType(NotificationEventType eventType);
}
