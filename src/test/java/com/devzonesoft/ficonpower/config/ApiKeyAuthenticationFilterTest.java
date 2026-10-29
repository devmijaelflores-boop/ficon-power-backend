package com.devzonesoft.ficonpower.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.ObjectMapper;

class ApiKeyAuthenticationFilterTest {

    private static final String API_KEY = "clave-de-prueba";

    private final ApiKeyAuthenticationFilter filter =
            new ApiKeyAuthenticationFilter(API_KEY);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsRequestWithoutApiKey() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/devices");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        var error = new ObjectMapper().readTree(response.getContentAsString());
        assertEquals("unauthorized", error.get("code").asText());
        assertEquals(
                "La API key es incorrecta o no fue enviada.",
                error.get("message").asText());
        assertNotNull(response.getHeader("X-Trace-Id"));
        assertNull(chain.getRequest());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void rejectsRequestWithWrongApiKey() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/incidents");
        request.addHeader(ApiKeyAuthenticationFilter.API_KEY_HEADER, "otra-clave");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertNull(chain.getRequest());
    }

    @Test
    void acceptsRequestWithValidApiKey() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/events");
        request.addHeader(ApiKeyAuthenticationFilter.API_KEY_HEADER, API_KEY);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest());
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals("api-client", authentication.getPrincipal());
    }

    @Test
    void skipsPathsOutsideApi() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void refusesBlankApiKey() {
        assertThrows(IllegalArgumentException.class,
                () -> new ApiKeyAuthenticationFilter("  "));
    }
}
