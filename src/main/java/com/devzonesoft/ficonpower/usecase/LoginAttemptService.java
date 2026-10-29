package com.devzonesoft.ficonpower.usecase;

import com.devzonesoft.ficonpower.ports.output.LoginAttemptRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class LoginAttemptService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration ATTEMPT_WINDOW = Duration.ofMinutes(10);

    private final LoginAttemptRepository loginAttempts;

    public LoginAttemptService(LoginAttemptRepository loginAttempts) {
        this.loginAttempts = loginAttempts;
    }

    public boolean hasActiveBlock(String username, String ip) {
        return loginAttempts.hasActiveBlock(username, ip);
    }

    public void blockAfterFailedAttempt(
            String username,
            UUID userId,
            String ip,
            String userAgent
    ) {
        Instant now = Instant.now();
        int failed = loginAttempts.countFailedAttemptsSinceLastSuccess(
                username,
                ip,
                now.minus(ATTEMPT_WINDOW));
        if (failed < MAX_FAILED_ATTEMPTS) return;

        loginAttempts.upsertBlock(
                username,
                userId,
                ip,
                userAgent,
                failed,
                now.plus(ATTEMPT_WINDOW));
    }

    public void clearExpiredOrManualBlock(String username, String ip) {
        loginAttempts.clearExpiredOrManualBlock(username, ip);
    }

    public void record(
            String username,
            UUID userId,
            String ip,
            String userAgent,
            boolean success,
            String reason
    ) {
        loginAttempts.record(username, userId, ip, userAgent, success, reason);
    }
}
