package com.devzonesoft.ficonpower.config;

import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.bootstrap.admin")
public record InitialAdminProperties(String name, String email, String password) {

    public boolean isConfigured() {
        return hasText(name) && hasText(email) && hasText(password);
    }

    public String normalizedName() {
        return name.trim();
    }

    public String normalizedEmail() {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
