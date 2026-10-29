package com.devzonesoft.ficonpower.web;

public record TemplateVariableView(
        String key,
        String label,
        String token,
        boolean requiredForText,
        boolean requiredForMetaTemplate,
        String description
) {
}
