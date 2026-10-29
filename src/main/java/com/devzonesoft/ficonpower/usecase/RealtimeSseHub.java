package com.devzonesoft.ficonpower.usecase;

import com.devzonesoft.ficonpower.domain.AuthenticatedUser;
import com.devzonesoft.ficonpower.domain.RealtimeChange;
import com.devzonesoft.ficonpower.domain.UserRole;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class RealtimeSseHub {

    private static final long CONNECTION_TIMEOUT_MILLIS = 30 * 60 * 1000L;

    private final AtomicLong subscriptionIds = new AtomicLong();
    private final Map<Long, Subscription> subscriptions = new ConcurrentHashMap<>();

    public SseEmitter subscribe(AuthenticatedUser user) {
        long subscriptionId = subscriptionIds.incrementAndGet();
        SseEmitter emitter = new SseEmitter(CONNECTION_TIMEOUT_MILLIS);
        subscriptions.put(subscriptionId, new Subscription(user, emitter));
        emitter.onCompletion(() -> subscriptions.remove(subscriptionId));
        emitter.onTimeout(() -> subscriptions.remove(subscriptionId));
        emitter.onError(error -> subscriptions.remove(subscriptionId));
        send(subscriptionId, emitter, SseEmitter.event()
                .name("ready")
                .data(Map.of("connected", true)));
        return emitter;
    }

    public void publish(RealtimeChange change) {
        subscriptions.forEach((id, subscription) -> {
            if (canReceive(subscription.user(), change)) {
                send(id, subscription.emitter(), SseEmitter.event()
                        .id(Long.toString(change.id()))
                        .name(change.eventType())
                        .data(change));
            }
        });
    }

    @Scheduled(fixedDelayString = "${app.realtime.heartbeat-delay:20000}")
    public void heartbeat() {
        subscriptions.forEach((id, subscription) -> send(
                id,
                subscription.emitter(),
                SseEmitter.event().comment("keep-alive")));
    }

    int subscriptionCount() {
        return subscriptions.size();
    }

    boolean canReceive(AuthenticatedUser user, RealtimeChange change) {
        return switch (change.audience()) {
            case AUTHENTICATED -> true;
            case ADMIN -> user.role() == UserRole.ADMIN || user.role() == UserRole.SUPERADMIN;
            case USER -> user.id().equals(change.targetUserId());
        };
    }

    private void send(long id, SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException exception) {
            subscriptions.remove(id);
            emitter.complete();
        }
    }

    private record Subscription(AuthenticatedUser user, SseEmitter emitter) {
    }
}
