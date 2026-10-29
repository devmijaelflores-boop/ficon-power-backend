package com.devzonesoft.ficonpower.adapters.tuya.cloud;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TuyaSpaceNameNormalizerTest {

    private final TuyaSpaceNameNormalizer normalizer = new TuyaSpaceNameNormalizer();

    @ParameterizedTest
    @CsvSource({
        "TIMAX_puno, TIMAX - Puno, Puno",
        "timax-putina, TIMAX - Putina, Putina",
        "' Timax   -   lampa ', TIMAX - Lampa, Lampa",
        "timax_juliaca, TIMAX - Juliaca, Juliaca",
        "'timax- juliaca', TIMAX - Juliaca, Juliaca",
        "'timax – juliaca', TIMAX - Juliaca, Juliaca",
        "'GRUPO NORTE_lampa', Grupo Norte - Lampa, Lampa"
    })
    void canonicalizesSupportedPunoPlacements(
            String source,
            String expectedName,
            String expectedDistrict
    ) {
        assertThat(normalizer.normalize(source))
                .get()
                .satisfies(placement -> {
                    assertThat(placement.deviceName()).isEqualTo(expectedName);
                    assertThat(placement.district()).isEqualTo(expectedDistrict);
                });
    }

    @Test
    void rejectsUnknownOrAmbiguousPlacements() {
        assertThat(normalizer.normalize("almacen-central")).isEmpty();
        assertThat(normalizer.normalize("Puno")).isEmpty();
    }
}
