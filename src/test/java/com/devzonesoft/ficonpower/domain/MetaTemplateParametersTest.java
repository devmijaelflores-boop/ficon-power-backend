package com.devzonesoft.ficonpower.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class MetaTemplateParametersTest {

    @Test
    void returnsNoParametersForMissingConfiguration() {
        assertEquals(List.of(), MetaTemplateParameters.parse(null));
        assertEquals(List.of(), MetaTemplateParameters.parse("   "));
    }

    @Test
    void normalizesAllSupportedAliases() {
        assertEquals(
                List.of(
                        "dispositivo",
                        "dispositivo",
                        "fechaCorte",
                        "fechaCorte",
                        "fechaCorte",
                        "fechaRetorno",
                        "fechaRetorno",
                        "fechaRetorno"),
                MetaTemplateParameters.parse(
                        "device, dispositivo, outage_at, fecha_corte, fechaCorte, "
                                + "restored_at, fecha_retorno, fechaRetorno"));
    }

    @Test
    void removesEmptyEntriesAndPreservesUnknownParametersInLowercase() {
        assertEquals(
                List.of("dispositivo", "customvalue"),
                MetaTemplateParameters.parse(" dispositivo, , CustomValue, "));
    }
}
