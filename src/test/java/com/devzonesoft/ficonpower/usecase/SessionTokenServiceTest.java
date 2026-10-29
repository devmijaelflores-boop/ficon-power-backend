package com.devzonesoft.ficonpower.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Base64;
import org.junit.jupiter.api.Test;

class SessionTokenServiceTest {

    private final SessionTokenService service = new SessionTokenService();

    @Test
    void generatesUniqueUrlSafeTokensWith256Bits() {
        String first = service.generate();
        String second = service.generate();

        assertNotEquals(first, second);
        assertEquals(32, Base64.getUrlDecoder().decode(first).length);
        assertEquals(32, Base64.getUrlDecoder().decode(second).length);
    }

    @Test
    void hashesTokensWithSha256UsingTheExistingFormat() {
        assertEquals(
                "2a8b315fc7e11eff36584f29688b75821385ab56bcd2f0571ef4e0a190d8ea89",
                service.hash("ficon-token"));
    }
}
