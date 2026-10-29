package com.devzonesoft.ficonpower.ports.output;

import com.devzonesoft.ficonpower.domain.AuditLogEntry;

public interface AuditLogRepository {

    void save(AuditLogEntry entry);
}
