package com.devzonesoft.ficonpower.usecase;

import com.devzonesoft.ficonpower.domain.AppUser;
import com.devzonesoft.ficonpower.domain.AuthenticatedUser;
import com.devzonesoft.ficonpower.domain.RequestMetadata;
import com.devzonesoft.ficonpower.ports.output.UserRepository;
import com.devzonesoft.ficonpower.ports.output.WhatsAppRecipientRepository;
import java.time.Instant;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService audit;
    private final LoginAttemptService loginAttempts;
    private final AuthSessionService authSessions;
    private final WhatsAppRecipientRepository whatsappRecipients;

    public AuthService(
            UserRepository users,
            PasswordEncoder passwordEncoder,
            AuditLogService audit,
            LoginAttemptService loginAttempts,
            AuthSessionService authSessions,
            WhatsAppRecipientRepository whatsappRecipients
    ) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
        this.loginAttempts = loginAttempts;
        this.authSessions = authSessions;
        this.whatsappRecipients = whatsappRecipients;
    }

    public LoginResult login(String username, String password, RequestMetadata request) {
        String ip = request.ipAddress();
        if (loginAttempts.hasActiveBlock(username, ip)) {
            loginAttempts.record(username, null, ip, request.userAgent(), false, "ip_blocked");
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Usuario bloqueado temporalmente por intentos fallidos");
        }

        Optional<AppUser> candidate = users.findByUsername(username);
        if (candidate.isEmpty()
                || !candidate.get().active()
                || !passwordEncoder.matches(password, candidate.get().passwordHash())) {
            loginAttempts.record(
                    username,
                    candidate.map(AppUser::id).orElse(null),
                    ip,
                    request.userAgent(),
                    false,
                    "invalid_credentials");
            loginAttempts.blockAfterFailedAttempt(
                    username,
                    candidate.map(AppUser::id).orElse(null),
                    ip,
                    request.userAgent());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales invalidas");
        }

        AppUser user = candidate.get();
        loginAttempts.clearExpiredOrManualBlock(username, ip);
        AuthSessionService.Session session = authSessions.create(user.id(), ip, request.userAgent());
        loginAttempts.record(username, user.id(), ip, request.userAgent(), true, null);
        AuthenticatedUser authenticated = toAuthenticatedUser(user);
        audit.recordForUser(
                authenticated,
                "auth.login",
                "auth_session",
                user.id().toString(),
                "{\"username\":\"" + user.username() + "\"}",
                request);
        return new LoginResult(
                session.token(),
                session.expiresAt(),
                session.refreshToken(),
                session.refreshExpiresAt(),
                authenticated);
    }

    private AuthenticatedUser toAuthenticatedUser(AppUser user) {
        String whatsappPhone = whatsappRecipients.findByUserId(user.id())
                .map(recipient -> recipient.phone())
                .orElse(null);
        return new AuthenticatedUser(
                user.id(), user.username(), user.role(), user.displayName(),
                user.email(), whatsappPhone);
    }

    public record LoginResult(
            String token,
            Instant expiresAt,
            String refreshToken,
            Instant refreshExpiresAt,
            AuthenticatedUser user
    ) {
    }
}
