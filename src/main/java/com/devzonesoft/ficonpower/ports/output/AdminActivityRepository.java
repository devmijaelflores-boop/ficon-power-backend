package com.devzonesoft.ficonpower.ports.output;

import com.devzonesoft.ficonpower.domain.AdminActivityViews.AuditLogView;
import com.devzonesoft.ficonpower.domain.AdminActivityViews.AuthSessionView;
import com.devzonesoft.ficonpower.domain.AdminActivityViews.BlockedLoginUserView;
import com.devzonesoft.ficonpower.domain.AdminActivityViews.LoginAttemptView;
import com.devzonesoft.ficonpower.domain.PagedResult;
import java.util.List;
import java.util.UUID;

public interface AdminActivityRepository {

    List<AuditLogView> findAuditLogs(int limit);

    PagedResult<AuditLogView> findAuditLogsPage(
            int page,
            int size,
            String search,
            String actor,
            String action,
            String entityType,
            String ipAddress
    );

    List<LoginAttemptView> findLoginAttempts(int limit);

    PagedResult<LoginAttemptView> findLoginAttemptsPage(
            int page,
            int size,
            String search,
            String username,
            String success,
            String ipAddress
    );

    List<AuthSessionView> findSessions(int limit);

    PagedResult<AuthSessionView> findSessionsPage(
            int page,
            int size,
            String search,
            String username,
            String role,
            String ipAddress
    );

    List<BlockedLoginUserView> findBlockedUsers(int limit);

    PagedResult<BlockedLoginUserView> findBlockedUsersPage(
            int page,
            int size,
            String search,
            String username,
            String active,
            String ipAddress
    );

    int unblock(UUID id, UUID actorId, String reason);

    BlockedLoginUserView findBlockedUser(UUID id);
}
