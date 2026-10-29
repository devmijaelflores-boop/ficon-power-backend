package com.devzonesoft.ficonpower.domain;

import java.util.UUID;

public record RefreshSession(UUID sessionId, AuthenticatedUser user) {
}
