package com.devzonesoft.ficonpower.config;

import com.devzonesoft.ficonpower.web.ApiErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Autentica las peticiones a /api/** comparando el header X-API-Key contra la
 * clave configurada. La comparación es de tiempo constante para no filtrar la
 * clave por diferencias de latencia.
 */
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-API-Key";

    private final byte[] expectedKey;

    public ApiKeyAuthenticationFilter(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalArgumentException("La API key no puede estar vacía");
        }
        this.expectedKey = apiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !uri.startsWith("/api/")
                || "/api/auth/login".equals(uri);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        String providedKey = request.getHeader(API_KEY_HEADER);
        String authorization = request.getHeader("Authorization");
        if (!StringUtils.hasText(providedKey)
                && StringUtils.hasText(authorization)
                && authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!StringUtils.hasText(providedKey)
                || !MessageDigest.isEqual(
                        providedKey.getBytes(StandardCharsets.UTF_8), expectedKey)) {
            SecurityContextHolder.clearContext();
            ApiErrorResponse.write(
                    request,
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "unauthorized",
                    "La API key es incorrecta o no fue enviada.");
            return;
        }

        var authentication = new UsernamePasswordAuthenticationToken(
                "api-client", null,
                List.of(new SimpleGrantedAuthority("ROLE_API_CLIENT")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }
}
