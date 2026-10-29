package com.devzonesoft.ficonpower.config;

import com.devzonesoft.ficonpower.usecase.AuthSessionService;
import com.devzonesoft.ficonpower.web.ApiErrorResponse;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    /**
     * Si API_KEY está configurada, /api/** exige el header X-API-Key y la raíz
     * queda pública para el healthcheck. Si no está configurada, la API queda
     * abierta (solo apto para desarrollo local) y se registra una advertencia.
     */
    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Value("${app.security.api-key:}") String apiKey,
            AuthSessionService authSessions
    ) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers.contentSecurityPolicy(csp -> csp
                        .policyDirectives("default-src 'none'; frame-ancestors 'none'")))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) ->
                                ApiErrorResponse.write(
                                        request,
                                        response,
                                        401,
                                        "unauthorized",
                                        "La sesión no es válida. Vuelve a iniciar sesión."))
                        .accessDeniedHandler((request, response, exception) ->
                                ApiErrorResponse.write(
                                        request,
                                        response,
                                        403,
                                        "forbidden",
                                        "No tienes permisos para realizar esta acción.")));

        http.addFilterBefore(
                new AuthTokenAuthenticationFilter(authSessions), AuthorizationFilter.class);

        if (StringUtils.hasText(apiKey)) {
            http.addFilterBefore(
                    new ApiKeyAuthenticationFilter(apiKey), AuthorizationFilter.class);
        } else {
            logger.warn("API_KEY no configurada: la API REST queda desprotegida. "
                    + "Use login de dashboard o defina la variable API_KEY para integraciones.");
        }
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/").permitAll()
                .requestMatchers("/health/tuya-pulsar").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/refresh").permitAll()
                .requestMatchers("/api/realtime/**")
                .hasAnyRole("USER", "ADMIN", "SUPERADMIN")
                .requestMatchers("/api/admin/**").hasAnyRole("SUPERADMIN", "API_CLIENT")
                .requestMatchers("/api/users/**").hasAnyRole("SUPERADMIN", "API_CLIENT")
                .requestMatchers("/api/mobile/**").hasAnyRole("USER", "ADMIN", "SUPERADMIN")
                .requestMatchers("/api/whatsapp-recipients/**")
                .hasAnyRole("ADMIN", "SUPERADMIN", "API_CLIENT")
                .requestMatchers(HttpMethod.POST, "/api/calendar/**")
                .hasAnyRole("ADMIN", "SUPERADMIN", "API_CLIENT")
                .requestMatchers(HttpMethod.PUT, "/api/calendar/**")
                .hasAnyRole("ADMIN", "SUPERADMIN", "API_CLIENT")
                .requestMatchers(HttpMethod.DELETE, "/api/calendar/**")
                .hasAnyRole("ADMIN", "SUPERADMIN", "API_CLIENT")
                .requestMatchers(HttpMethod.GET, "/api/**")
                .hasAnyRole("USER", "ADMIN", "SUPERADMIN", "API_CLIENT")
                .requestMatchers(HttpMethod.PUT, "/api/devices/**")
                .hasAnyRole("ADMIN", "SUPERADMIN", "API_CLIENT")
                .requestMatchers(HttpMethod.PATCH, "/api/incidents/**")
                .hasAnyRole("ADMIN", "SUPERADMIN", "API_CLIENT")
                .requestMatchers(HttpMethod.POST, "/api/sites")
                .hasAnyRole("ADMIN", "SUPERADMIN", "API_CLIENT")
                .requestMatchers(HttpMethod.POST, "/api/auth/logout")
                .authenticated()
                .anyRequest().denyAll());
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /**
     * CORS queda deshabilitado salvo que se definan orígenes permitidos; es una
     * API máquina-a-máquina, así que por defecto no se emiten cabeceras CORS.
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.security.cors.allowed-origins:}") String allowedOrigins,
            @Value("${app.security.cors.allowed-methods:GET}") String allowedMethods,
            @Value("${app.security.cors.allowed-headers:X-API-Key,Authorization,Content-Type}")
            String allowedHeaders
    ) {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        if (StringUtils.hasText(allowedOrigins)) {
            CorsConfiguration config = new CorsConfiguration();
            config.setAllowedOrigins(split(allowedOrigins));
            config.setAllowedMethods(split(allowedMethods));
            config.setAllowedHeaders(split(allowedHeaders));
            source.registerCorsConfiguration("/api/**", config);
        }
        return source;
    }

    private List<String> split(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }
}
