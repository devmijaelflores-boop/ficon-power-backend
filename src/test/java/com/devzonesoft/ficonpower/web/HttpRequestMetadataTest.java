package com.devzonesoft.ficonpower.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.devzonesoft.ficonpower.domain.RequestMetadata;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

class HttpRequestMetadataTest {

    @Test
    void usesTheFirstForwardedAddressAndUserAgent() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For"))
                .thenReturn("198.51.100.4, 10.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("JUnit");

        RequestMetadata metadata = HttpRequestMetadata.from(request);

        assertEquals("198.51.100.4", metadata.ipAddress());
        assertEquals("JUnit", metadata.userAgent());
    }

    @Test
    void fallsBackToTheRemoteAddress() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(" ");
        when(request.getRemoteAddr()).thenReturn("10.0.0.8");

        assertEquals("10.0.0.8", HttpRequestMetadata.from(request).ipAddress());
    }
}
