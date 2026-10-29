package com.devzonesoft.ficonpower.usecase;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devzonesoft.ficonpower.ports.output.LoginAttemptRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LoginAttemptServiceTest {

    private final LoginAttemptRepository loginAttempts = mock(LoginAttemptRepository.class);
    private final LoginAttemptService service = new LoginAttemptService(loginAttempts);

    @Test
    void detectsAnActiveBlockForTheUsernameAndIpPair() {
        when(loginAttempts.hasActiveBlock("admin", "10.0.0.8")).thenReturn(true);

        assertTrue(service.hasActiveBlock("admin", "10.0.0.8"));
    }

    @Test
    void doesNotBlockBeforeTheFifthFailedAttempt() {
        when(loginAttempts.countFailedAttemptsSinceLastSuccess(
                eq("admin"), eq("10.0.0.8"), any(Instant.class))).thenReturn(4);

        service.blockAfterFailedAttempt("admin", null, "10.0.0.8", "JUnit");

        verify(loginAttempts, never()).upsertBlock(
                any(), any(), any(), any(), any(Integer.class), any(Instant.class));
    }

    @Test
    void blocksTheUsernameAndIpPairOnTheFifthFailedAttempt() {
        UUID userId = UUID.randomUUID();
        when(loginAttempts.countFailedAttemptsSinceLastSuccess(
                eq("admin"), eq("10.0.0.8"), any(Instant.class))).thenReturn(5);

        service.blockAfterFailedAttempt("admin", userId, "10.0.0.8", "JUnit");

        verify(loginAttempts).upsertBlock(
                eq("admin"),
                eq(userId),
                eq("10.0.0.8"),
                eq("JUnit"),
                eq(5),
                any(Instant.class));
    }

    @Test
    void delegatesSuccessfulBlockCleanup() {
        service.clearExpiredOrManualBlock("admin", "10.0.0.8");

        verify(loginAttempts).clearExpiredOrManualBlock("admin", "10.0.0.8");
    }

    @Test
    void delegatesAttemptRecordingWithoutChangingValues() {
        UUID userId = UUID.randomUUID();

        service.record("admin", userId, "10.0.0.8", "JUnit", false, "invalid_credentials");

        verify(loginAttempts).record(
                "admin", userId, "10.0.0.8", "JUnit", false, "invalid_credentials");
    }
}
