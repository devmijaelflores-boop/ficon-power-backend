package com.devzonesoft.ficonpower.adapters.notification;

import com.devzonesoft.ficonpower.config.FcmProperties;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.util.StringUtils;

public class FcmAccessTokenProvider {

    private static final List<String> SCOPES =
            List.of("https://www.googleapis.com/auth/firebase.messaging");

    private final GoogleCredentials credentials;

    public FcmAccessTokenProvider(FcmProperties properties) {
        this.credentials = loadCredentials(properties);
    }

    public String accessToken() {
        try {
            credentials.refreshIfExpired();
            return credentials.getAccessToken().getTokenValue();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo obtener token OAuth para FCM", e);
        }
    }

    private GoogleCredentials loadCredentials(FcmProperties properties) {
        try (InputStream input = credentialsStream(properties)) {
            return GoogleCredentials.fromStream(input).createScoped(SCOPES);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudieron cargar credenciales FCM", e);
        }
    }

    private InputStream credentialsStream(FcmProperties properties) throws IOException {
        if (StringUtils.hasText(properties.getCredentialsJson())) {
            return new ByteArrayInputStream(
                    properties.getCredentialsJson().getBytes(StandardCharsets.UTF_8));
        }
        return new FileInputStream(properties.getCredentialsPath());
    }
}
