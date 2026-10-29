package com.devzonesoft.ficonpower.web;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devzonesoft.ficonpower.domain.AdminActivityViews.BlockedLoginUserView;
import com.devzonesoft.ficonpower.domain.AuthenticatedUser;
import com.devzonesoft.ficonpower.domain.RequestMetadata;
import com.devzonesoft.ficonpower.domain.UserRole;
import com.devzonesoft.ficonpower.ports.output.AdminActivityRepository;
import com.devzonesoft.ficonpower.ports.output.CurrentUserProvider;
import com.devzonesoft.ficonpower.usecase.AuditLogService;
import com.devzonesoft.ficonpower.usecase.AuthSessionService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AdminActivityControllerTest {

    private final AdminActivityRepository activity = mock(AdminActivityRepository.class);
    private final CurrentUserProvider currentUsers = mock(CurrentUserProvider.class);
    private final AuditLogService audit = mock(AuditLogService.class);
    private final AuthSessionService authSessions = mock(AuthSessionService.class);
    private final AdminActivityController controller =
            new AdminActivityController(activity, currentUsers, audit, authSessions);

    @Test
    void revokesAnAdministrativeSessionAndRecordsItsOrigin() {
        UUID sessionId = UUID.randomUUID();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("User-Agent")).thenReturn("JUnit");
        when(request.getRemoteAddr()).thenReturn("10.0.0.8");
        when(authSessions.revoke(sessionId)).thenReturn(true);

        AdminActivityController.SessionRevocationResponse response =
                controller.revokeSession(sessionId, request);

        assertTrue(response.revoked());
        verify(audit).record(
                "auth_session.revoke",
                "auth_session",
                sessionId.toString(),
                "{\"source\":\"superadmin_console\"}",
                new RequestMetadata("10.0.0.8", "JUnit"));
    }

    @Test
    void unblocksWithTheDefaultReasonAndRecordsTheAudit() {
        UUID blockedId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        HttpServletRequest request = mock(HttpServletRequest.class);
        BlockedLoginUserView expected = blockedUser(blockedId);
        when(currentUsers.currentUser()).thenReturn(Optional.of(new AuthenticatedUser(
                actorId, "superadmin", UserRole.SUPERADMIN, "Superadmin")));
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("User-Agent")).thenReturn("JUnit");
        when(request.getRemoteAddr()).thenReturn("10.0.0.8");
        when(activity.unblock(blockedId, actorId, "manual_superadmin_unlock")).thenReturn(1);
        when(activity.findBlockedUser(blockedId)).thenReturn(expected);

        BlockedLoginUserView result = controller.unblockBlockedUser(
                blockedId,
                new AdminActivityController.UnblockBlockedUserRequest(" "),
                request);

        assertSame(expected, result);
        verify(audit).record(
                "blocked_user.unlock",
                "blocked_login_user",
                blockedId.toString(),
                "{\"reason\":\"manual_superadmin_unlock\"}",
                new RequestMetadata("10.0.0.8", "JUnit"));
    }

    @Test
    void doesNotRecordAnAuditWhenNoBlockedUserWasUpdated() {
        UUID blockedId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(currentUsers.currentUser()).thenReturn(Optional.of(new AuthenticatedUser(
                actorId, "superadmin", UserRole.SUPERADMIN, "Superadmin")));
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("User-Agent")).thenReturn("JUnit");
        when(request.getRemoteAddr()).thenReturn("10.0.0.8");
        when(activity.unblock(blockedId, actorId, "reviewed")).thenReturn(0);
        when(activity.findBlockedUser(blockedId)).thenReturn(blockedUser(blockedId));

        controller.unblockBlockedUser(
                blockedId,
                new AdminActivityController.UnblockBlockedUserRequest("reviewed"),
                request);

        verify(audit, never()).record(
                "blocked_user.unlock",
                "blocked_login_user",
                blockedId.toString(),
                "{\"reason\":\"manual_superadmin_unlock\"}",
                new RequestMetadata("10.0.0.8", "JUnit"));
    }

    private BlockedLoginUserView blockedUser(UUID id) {
        return new BlockedLoginUserView(
                id.toString(),
                "admin",
                null,
                null,
                "10.0.0.8",
                "JUnit",
                5,
                "too_many_failed_attempts",
                Instant.now(),
                null,
                null,
                null,
                null,
                null,
                false);
    }
}
