package com.devzonesoft.ficonpower.web;

import java.util.List;

public record NotificationTemplateView(
        String eventType,
        String metaTemplateName,
        String metaTemplateLanguage,
        List<String> metaTemplateParams,
        List<TemplateVariableView> availableVariables
) {
}
