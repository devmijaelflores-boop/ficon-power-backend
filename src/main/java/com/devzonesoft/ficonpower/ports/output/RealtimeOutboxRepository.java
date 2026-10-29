package com.devzonesoft.ficonpower.ports.output;

import com.devzonesoft.ficonpower.domain.RealtimeChange;
import java.util.List;

public interface RealtimeOutboxRepository {

    List<RealtimeChange> claimPending(int limit);

    void markPublished(List<Long> ids);

    int deletePublishedOlderThanDays(int retentionDays);
}
