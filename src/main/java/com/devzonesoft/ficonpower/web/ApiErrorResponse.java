package com.devzonesoft.ficonpower.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;

public record ApiErrorResponse(
        String code,
        String message,
        List<ApiFieldError> fieldErrors,
        String traceId
) {

    private static final String TRACE_HEADER = "X-Trace-Id";
    private static final ObjectMapper JSON = new ObjectMapper();

    public ApiErrorResponse {
        fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
    }

    public static ApiErrorResponse of(
            String code,
            String message,
            List<ApiFieldError> fieldErrors,
            HttpServletRequest request
    ) {
        return new ApiErrorResponse(code, message, fieldErrors, traceId(request));
    }

    public static void write(
            HttpServletRequest request,
            HttpServletResponse response,
            int status,
            String code,
            String message
    ) throws IOException {
        ApiErrorResponse error = of(code, message, List.of(), request);
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(TRACE_HEADER, error.traceId());
        JSON.writeValue(response.getOutputStream(), error);
    }

    private static String traceId(HttpServletRequest request) {
        String supplied = request.getHeader("X-Request-Id");
        if (supplied != null && supplied.matches("[A-Za-z0-9._:-]{1,100}")) {
            return supplied;
        }
        return UUID.randomUUID().toString();
    }

    public record ApiFieldError(String field, String message) {
    }
}
