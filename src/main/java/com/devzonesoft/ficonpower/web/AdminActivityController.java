package com.devzonesoft.ficonpower.web;

import com.devzonesoft.ficonpower.domain.AuthenticatedUser;
import com.devzonesoft.ficonpower.domain.AdminActivityViews.AuditLogView;
import com.devzonesoft.ficonpower.domain.AdminActivityViews.AuthSessionView;
import com.devzonesoft.ficonpower.domain.AdminActivityViews.BlockedLoginUserView;
import com.devzonesoft.ficonpower.domain.AdminActivityViews.LoginAttemptView;
import com.devzonesoft.ficonpower.domain.PagedResult;
import com.devzonesoft.ficonpower.ports.output.AdminActivityRepository;
import com.devzonesoft.ficonpower.ports.output.CurrentUserProvider;
import com.devzonesoft.ficonpower.usecase.AuditLogService;
import com.devzonesoft.ficonpower.usecase.AuthSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin")
public class AdminActivityController {

    private final AdminActivityRepository activity;
    private final CurrentUserProvider currentUsers;
    private final AuditLogService audit;
    private final AuthSessionService authSessions;

    public AdminActivityController(
            AdminActivityRepository activity,
            CurrentUserProvider currentUsers,
            AuditLogService audit,
            AuthSessionService authSessions
    ) {
        this.activity = activity;
        this.currentUsers = currentUsers;
        this.audit = audit;
        this.authSessions = authSessions;
    }

    @GetMapping("/audit-logs")
    public Object auditLogs(
            @RequestParam(defaultValue = "100") @Min(1) @Max(500) int limit,
            @RequestParam Optional<@Min(0) Integer> page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "") String actor,
            @RequestParam(defaultValue = "") String action,
            @RequestParam(defaultValue = "") String entityType,
            @RequestParam(defaultValue = "") String ipAddress
    ) {
        if (page.isPresent()) {
            return activity.findAuditLogsPage(page.get(), size, search, actor, action, entityType, ipAddress);
        }
        return activity.findAuditLogs(limit);
    }

    @GetMapping("/login-attempts")
    public Object loginAttempts(
            @RequestParam(defaultValue = "100") @Min(1) @Max(500) int limit,
            @RequestParam Optional<@Min(0) Integer> page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "") String username,
            @RequestParam(defaultValue = "") String success,
            @RequestParam(defaultValue = "") String ipAddress
    ) {
        if (page.isPresent()) {
            return activity.findLoginAttemptsPage(page.get(), size, search, username, success, ipAddress);
        }
        return activity.findLoginAttempts(limit);
    }

    @GetMapping("/sessions")
    public Object sessions(
            @RequestParam(defaultValue = "100") @Min(1) @Max(500) int limit,
            @RequestParam Optional<@Min(0) Integer> page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "") String username,
            @RequestParam(defaultValue = "") String role,
            @RequestParam(defaultValue = "") String ipAddress
    ) {
        if (page.isPresent()) {
            return activity.findSessionsPage(page.get(), size, search, username, role, ipAddress);
        }
        return activity.findSessions(limit);
    }

    @PostMapping("/sessions/{id}/revoke")
    public SessionRevocationResponse revokeSession(
            @PathVariable UUID id,
            HttpServletRequest servletRequest
    ) {
        boolean revoked = authSessions.revoke(id);
        if (!revoked) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "La sesión ya estaba cerrada o no existe");
        }
        audit.record(
                "auth_session.revoke",
                "auth_session",
                id.toString(),
                "{\"source\":\"superadmin_console\"}",
                HttpRequestMetadata.from(servletRequest));
        return new SessionRevocationResponse(id.toString(), true);
    }

    @GetMapping("/blocked-users")
    public Object blockedUsers(
            @RequestParam(defaultValue = "100") @Min(1) @Max(500) int limit,
            @RequestParam Optional<@Min(0) Integer> page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "") String username,
            @RequestParam(defaultValue = "") String active,
            @RequestParam(defaultValue = "") String ipAddress
    ) {
        if (page.isPresent()) {
            return activity.findBlockedUsersPage(page.get(), size, search, username, active, ipAddress);
        }
        return activity.findBlockedUsers(limit);
    }

    @PostMapping("/blocked-users/{id}/unblock")
    public BlockedLoginUserView unblockBlockedUser(
            @PathVariable UUID id,
            @Valid @RequestBody UnblockBlockedUserRequest request,
            HttpServletRequest servletRequest
    ) {
        AuthenticatedUser actor = currentUsers.currentUser()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Sesion requerida"));
        String reason = request.reason() == null || request.reason().isBlank()
                ? "manual_superadmin_unlock"
                : request.reason();
        int updated = activity.unblock(id, actor.id(), reason);
        if (updated > 0) {
            audit.record("blocked_user.unlock", "blocked_login_user", id.toString(),
                    "{\"reason\":\"manual_superadmin_unlock\"}",
                    HttpRequestMetadata.from(servletRequest));
        }
        return activity.findBlockedUser(id);
    }

    public record UnblockBlockedUserRequest(String reason) {
    }

    public record SessionRevocationResponse(String id, boolean revoked) {
    }
}
