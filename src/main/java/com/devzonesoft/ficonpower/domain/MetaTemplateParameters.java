package com.devzonesoft.ficonpower.domain;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class MetaTemplateParameters {

    private MetaTemplateParameters() {
    }

    public static List<String> parse(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(param -> !param.isBlank())
                .map(param -> param.toLowerCase(Locale.ROOT))
                .map(MetaTemplateParameters::canonicalName)
                .toList();
    }

    private static String canonicalName(String value) {
        return switch (value) {
            case "device", "dispositivo" -> "dispositivo";
            case "outage_at", "fecha_corte", "fechacorte" -> "fechaCorte";
            case "restored_at", "fecha_retorno", "fecharetorno" -> "fechaRetorno";
            default -> value;
        };
    }
}
