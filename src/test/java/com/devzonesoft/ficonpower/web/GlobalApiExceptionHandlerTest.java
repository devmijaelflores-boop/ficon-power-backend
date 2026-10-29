package com.devzonesoft.ficonpower.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

class GlobalApiExceptionHandlerTest {

    private final GlobalApiExceptionHandler handler = new GlobalApiExceptionHandler();

    @Test
    void returnsFriendlyFieldErrorsWithTraceId() {
        var binding = new BeanPropertyBindingResult(new Object(), "request");
        binding.addError(new FieldError(
                "request", "password", "short", false,
                new String[]{"Size"}, null, "technical"));
        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(null, binding);

        var response = handler.validation(exception, request("request-123"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("validation_error", response.getBody().code());
        assertEquals("request-123", response.getBody().traceId());
        assertEquals(
                "La contraseña debe tener entre 8 y 120 caracteres.",
                response.getBody().fieldErrors().getFirst().message());
    }

    @Test
    void preservesControlledStatusMessagesInTheUniformContract() {
        var response = handler.status(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Dispositivo no encontrado"),
                request(null));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("not_found", response.getBody().code());
        assertEquals("Dispositivo no encontrado", response.getBody().message());
        assertFalse(response.getBody().traceId().isBlank());
        assertNotNull(response.getHeaders().getFirst("X-Trace-Id"));
    }

    private HttpServletRequest request(String requestId) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Request-Id")).thenReturn(requestId);
        return request;
    }
}
