package com.devzonesoft.ficonpower.usecase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devzonesoft.ficonpower.domain.RealtimeChange;
import com.devzonesoft.ficonpower.domain.RealtimeChange.Audience;
import com.devzonesoft.ficonpower.ports.output.RealtimeOutboxRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class RealtimeOutboxPublisherTest {

    @Test
    void publishesAndMarksTheClaimedBatch() {
        RealtimeOutboxRepository outbox = mock(RealtimeOutboxRepository.class);
        RealtimeSseHub hub = mock(RealtimeSseHub.class);
        RealtimeChange change = new RealtimeChange(
                17L,
                "event.created",
                "event",
                "81",
                Audience.AUTHENTICATED,
                null,
                new ObjectMapper().createObjectNode(),
                Instant.parse("2026-09-13T12:00:00Z"));
        when(outbox.claimPending(25)).thenReturn(List.of(change));
        RealtimeOutboxPublisher publisher =
                new RealtimeOutboxPublisher(outbox, hub, 25, 3);

        publisher.publishPending();

        verify(hub).publish(change);
        verify(outbox).markPublished(List.of(17L));
    }
}
