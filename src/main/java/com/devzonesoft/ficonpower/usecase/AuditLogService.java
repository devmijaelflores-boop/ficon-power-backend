package com.devzonesoft.ficonpower.usecase;

import com.devzonesoft.ficonpower.domain.AuthenticatedUser;
import com.devzonesoft.ficonpower.domain.AuditLogEntry;
import com.devzonesoft.ficonpower.domain.RequestMetadata;
import com.devzonesoft.ficonpower.ports.output.AuditLogRepository;
import com.devzonesoft.ficonpower.ports.output.CurrentUserProvider;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogs;
    private final CurrentUserProvider currentUsers;

    public AuditLogService(AuditLogRepository auditLogs, CurrentUserProvider currentUsers) {
        this.auditLogs = auditLogs;
        this.currentUsers = currentUsers;
    }

    public void record(
            String action,
            String entityType,
            String entityId,
            String metadata,
            RequestMetadata request
    ) {
        AuthenticatedUser user = currentUsers.currentUser().orElse(null);
        save(
                user == null ? null : user.id(),
                user == null ? null : user.username(),
                user == null ? null : user.role().name(),
                action,
                entityType,
                entityId,
                request.ipAddress(),
                request.userAgent(),
                metadata);
    }

    public void recordForUser(
            AuthenticatedUser user,
            String action,
            String entityType,
            String entityId,
            String metadata,
            RequestMetadata request
    ) {
        save(
                user.id(),
                user.username(),
                user.role().name(),
                action,
                entityType,
                entityId,
                request.ipAddress(),
                request.userAgent(),
                metadata);
    }

    private void save(
            java.util.UUID actorUserId,
            String actorUsername,
            String actorRole,
            String action,
            String entityType,
            String entityId,
            String ipAddress,
            String userAgent,
            String metadata
    ) {
        auditLogs.save(new AuditLogEntry(
                actorUserId,
                actorUsername,
                actorRole,
                action,
                entityType,
                entityId,
                ipAddress,
                userAgent,
                metadata));
    }
}
