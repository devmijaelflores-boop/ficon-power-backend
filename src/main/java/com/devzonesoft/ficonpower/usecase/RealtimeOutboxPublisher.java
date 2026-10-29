package com.devzonesoft.ficonpower.usecase;

import com.devzonesoft.ficonpower.domain.RealtimeChange;
import com.devzonesoft.ficonpower.ports.output.RealtimeOutboxRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class RealtimeOutboxPublisher {

    private static final Logger logger = LoggerFactory.getLogger(RealtimeOutboxPublisher.class);

    private final RealtimeOutboxRepository outbox;
    private final RealtimeSseHub hub;
    private final int batchSize;
    private final int retentionDays;

    public RealtimeOutboxPublisher(
            RealtimeOutboxRepository outbox,
            RealtimeSseHub hub,
            @Value("${app.realtime.batch-size:100}") int batchSize,
            @Value("${app.realtime.retention-days:3}") int retentionDays
    ) {
        this.outbox = outbox;
        this.hub = hub;
        this.batchSize = batchSize;
        this.retentionDays = retentionDays;
    }

    @Scheduled(fixedDelayString = "${app.realtime.poll-delay:750}")
    public void publishPending() {
        List<RealtimeChange> changes = outbox.claimPending(batchSize);
        changes.forEach(hub::publish);
        outbox.markPublished(changes.stream().map(RealtimeChange::id).toList());
    }

    @Scheduled(cron = "${app.realtime.cleanup-cron:0 17 3 * * *}")
    public void cleanup() {
        int deleted = outbox.deletePublishedOlderThanDays(retentionDays);
        if (deleted > 0) {
            logger.info("Removed {} published realtime outbox records", deleted);
        }
    }
}
