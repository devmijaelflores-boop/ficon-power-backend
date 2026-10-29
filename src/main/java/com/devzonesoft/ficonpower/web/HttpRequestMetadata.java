package com.devzonesoft.ficonpower.web;

import com.devzonesoft.ficonpower.domain.RequestMetadata;
import jakarta.servlet.http.HttpServletRequest;

public final class HttpRequestMetadata {

    private HttpRequestMetadata() {
    }

    public static RequestMetadata from(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        String ipAddress = forwardedFor != null && !forwardedFor.isBlank()
                ? forwardedFor.split(",")[0].trim()
                : request.getRemoteAddr();
        return new RequestMetadata(ipAddress, request.getHeader("User-Agent"));
    }
}
