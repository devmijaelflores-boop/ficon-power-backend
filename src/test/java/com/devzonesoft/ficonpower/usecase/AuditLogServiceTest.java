package com.devzonesoft.ficonpower.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devzonesoft.ficonpower.domain.AuditLogEntry;
import com.devzonesoft.ficonpower.domain.AuthenticatedUser;
import com.devzonesoft.ficonpower.domain.RequestMetadata;
import com.devzonesoft.ficonpower.domain.UserRole;
import com.devzonesoft.ficonpower.ports.output.AuditLogRepository;
import com.devzonesoft.ficonpower.ports.output.CurrentUserProvider;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class AuditLogServiceTest {

    private final AuditLogRepository auditLogs = mock(AuditLogRepository.class);
    private final CurrentUserProvider currentUsers = mock(CurrentUserProvider.class);
    private final AuditLogService service = new AuditLogService(auditLogs, currentUsers);

    @Test
    void recordsTheAuthenticatedUserAndFirstForwardedIp() {
        AuthenticatedUser user = new AuthenticatedUser(
                UUID.randomUUID(), "admin", UserRole.ADMIN, "Admin");
        when(currentUsers.currentUser()).thenReturn(Optional.of(user));
        RequestMetadata request = new RequestMetadata("198.51.100.4", "JUnit");
        AtomicReference<AuditLogEntry> saved = captureSavedEntry();

        service.record("device.update", "device", "device-1", "{}", request);

        AuditLogEntry entry = saved.get();
        assertEquals(user.id(), entry.actorUserId());
        assertEquals("admin", entry.actorUsername());
        assertEquals("ADMIN", entry.actorRole());
        assertEquals("198.51.100.4", entry.ipAddress());
        assertEquals("JUnit", entry.userAgent());
    }

    @Test
    void recordsAnonymousActivityWithTheRemoteAddress() {
        when(currentUsers.currentUser()).thenReturn(Optional.empty());
        RequestMetadata request = new RequestMetadata("10.0.0.8", "JUnit");
        AtomicReference<AuditLogEntry> saved = captureSavedEntry();

        service.record("auth.logout", "auth_session", null, null, request);

        AuditLogEntry entry = saved.get();
        assertNull(entry.actorUserId());
        assertNull(entry.actorUsername());
        assertNull(entry.actorRole());
        assertEquals("10.0.0.8", entry.ipAddress());
    }

    @Test
    void recordsAnExplicitUserDuringLogin() {
        AuthenticatedUser user = new AuthenticatedUser(
                UUID.randomUUID(), "admin", UserRole.ADMIN, "Admin");
        RequestMetadata request = new RequestMetadata("10.0.0.8", "JUnit");
        AtomicReference<AuditLogEntry> saved = captureSavedEntry();

        service.recordForUser(user, "auth.login", "auth_session", "session-1", "{}", request);

        assertEquals(user.id(), saved.get().actorUserId());
        assertEquals("auth.login", saved.get().action());
    }

    private AtomicReference<AuditLogEntry> captureSavedEntry() {
        AtomicReference<AuditLogEntry> saved = new AtomicReference<>();
        org.mockito.Mockito.doAnswer(invocation -> {
            saved.set(invocation.getArgument(0));
            return null;
        }).when(auditLogs).save(any(AuditLogEntry.class));
        return saved;
    }
}
