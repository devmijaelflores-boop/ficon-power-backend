package com.devzonesoft.ficonpower.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devzonesoft.ficonpower.domain.AppUser;
import com.devzonesoft.ficonpower.domain.RequestMetadata;
import com.devzonesoft.ficonpower.domain.UserRole;
import com.devzonesoft.ficonpower.ports.output.AuthSessionRepository;
import com.devzonesoft.ficonpower.ports.output.LoginAttemptRepository;
import com.devzonesoft.ficonpower.ports.output.UserRepository;
import com.devzonesoft.ficonpower.ports.output.WhatsAppRecipientRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

class AuthServiceTest {

    private static final String STRONG_ADMIN_PASSWORD = "Adm-FICON-2026-Mensajes!8qT2";

    private final UserRepository users = mock(UserRepository.class);
    private final AuditLogService audit = mock(AuditLogService.class);
    private final PasswordEncoder encoder =
            PasswordEncoderFactories.createDelegatingPasswordEncoder();
    private final LoginAttemptRepository loginAttemptRepository = mock(LoginAttemptRepository.class);
    private final LoginAttemptService loginAttempts =
            new LoginAttemptService(loginAttemptRepository);
    private final AuthSessionService authSessions = mock(AuthSessionService.class);
    private final WhatsAppRecipientRepository whatsappRecipients =
            mock(WhatsAppRecipientRepository.class);
    private final AuthService service =
            new AuthService(
                    users, encoder, audit, loginAttempts, authSessions, whatsappRecipients);

    @Test
    void loginUsesUsernameAndIpForFailedAttemptWindowAndSessionExpiry() {
        RequestMetadata request = requestFrom("10.0.0.8");
        AppUser user = new AppUser(
                UUID.randomUUID(),
                "admin",
                encoder.encode(STRONG_ADMIN_PASSWORD),
                UserRole.ADMIN,
                "Admin",
                true,
                Instant.now(),
                Instant.now());

        when(users.findByUsername("admin")).thenReturn(Optional.of(user));
        when(authSessions.create(user.id(), "10.0.0.8", "JUnit")).thenReturn(
                new AuthSessionService.Session(
                        "raw-token",
                        Instant.now().plusSeconds(24 * 60 * 60),
                        "raw-refresh-token",
                        Instant.now().plusSeconds(90L * 24 * 60 * 60)));

        AuthService.LoginResult result = service.login("admin", STRONG_ADMIN_PASSWORD, request);

        assertEquals("admin", result.user().username());
        verify(authSessions).create(user.id(), "10.0.0.8", "JUnit");
    }

    @Test
    void loginRejectsActiveBlockedUsernameAndIpPair() {
        RequestMetadata request = requestFrom("10.0.0.9");
        when(loginAttemptRepository.hasActiveBlock("admin", "10.0.0.9")).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.login("admin", STRONG_ADMIN_PASSWORD, request));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getStatusCode());
    }

    @Test
    void failedAttemptsForAnotherUsernameDoNotBlockCurrentUser() {
        RequestMetadata request = requestFrom("10.0.0.9");
        AppUser user = new AppUser(
                UUID.randomUUID(),
                "admin",
                encoder.encode(STRONG_ADMIN_PASSWORD),
                UserRole.ADMIN,
                "Admin",
                true,
                Instant.now(),
                Instant.now());
        when(users.findByUsername("admin")).thenReturn(Optional.of(user));
        when(authSessions.create(user.id(), "10.0.0.9", "JUnit")).thenReturn(
                new AuthSessionService.Session(
                        "raw-token",
                        Instant.now().plusSeconds(24 * 60 * 60),
                        "raw-refresh-token",
                        Instant.now().plusSeconds(90L * 24 * 60 * 60)));

        AuthService.LoginResult result = service.login("admin", STRONG_ADMIN_PASSWORD, request);

        assertEquals("admin", result.user().username());
    }

    private RequestMetadata requestFrom(String ip) {
        return new RequestMetadata(ip, "JUnit");
    }
}
