package com.devzonesoft.ficonpower.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.devzonesoft.ficonpower.domain.MonitoringViews.SiteView;
import com.devzonesoft.ficonpower.ports.output.SiteRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CreateSiteServiceTest {

    private final SiteRepository siteRepository = (name, district, province) -> new SiteView(
            UUID.randomUUID(), name, null, district, province, null,
            null, null, "America/Lima", true, 0,
            OffsetDateTime.now(), OffsetDateTime.now());

    private final CreateSiteService service = new CreateSiteService(siteRepository);

    @Test
    void createsSiteWithOptionalFields() {
        SiteView created = service.create("Lampa", "Lampa", "Puno");

        assertEquals("Lampa", created.name());
        assertEquals("Lampa", created.district());
        assertEquals("Puno", created.province());
    }

    @Test
    void infersDistrictFromTuyaSpatialStyleName() {
        SiteView created = service.create("  Timax - Puno  ", null, null);

        assertEquals("Timax - Puno", created.name());
        assertEquals("Puno", created.district());
    }

    @Test
    void infersDistrictFromTheLastNameSegment() {
        SiteView created = service.create("Timax - Zona Sur - Puno", " ", null);

        assertEquals("Puno", created.district());
    }

    @Test
    void keepsAnExplicitDistrictAndTrimsOptionalFields() {
        SiteView created = service.create("Timax - Puno", "  Juliaca  ", "  San Roman  ");

        assertEquals("Juliaca", created.district());
        assertEquals("San Roman", created.province());
    }

    @Test
    void doesNotTreatAnOrdinaryHyphenAsLocationSeparator() {
        SiteView created = service.create("Timax-Puno", null, null);

        assertNull(created.district());
    }

    @Test
    void rejectsBlankName() {
        assertThrows(IllegalArgumentException.class,
                () -> service.create("   ", null, null));
    }

    @Test
    void rejectsTooLongName() {
        String longName = "x".repeat(CreateSiteService.MAX_NAME_LENGTH + 1);

        assertThrows(IllegalArgumentException.class,
                () -> service.create(longName, null, null));
    }

    @Test
    void rejectsTooLongInferredDistrict() {
        String district = "x".repeat(CreateSiteService.MAX_DISTRICT_LENGTH + 1);

        assertThrows(IllegalArgumentException.class,
                () -> service.create("S - " + district, null, null));
    }
}
