package com.devzonesoft.ficonpower.web;

import com.devzonesoft.ficonpower.domain.AuthenticatedUser;
import com.devzonesoft.ficonpower.domain.MobileNotification;
import com.devzonesoft.ficonpower.usecase.MobileNotificationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/mobile/notifications")
public class MobileNotificationController {

    private final MobileNotificationService notifications;

    public MobileNotificationController(MobileNotificationService notifications) {
        this.notifications = notifications;
    }

    @GetMapping
    public List<MobileNotificationView> history(
            @RequestParam(defaultValue = "100") @Min(1) @Max(500) int limit,
            Authentication authentication
    ) {
        return notifications.findHistory(authenticatedUser(authentication), limit)
                .stream()
                .map(MobileNotificationView::from)
                .toList();
    }

    @GetMapping("/unread-count")
    public UnreadCountView unreadCount(Authentication authentication) {
        return new UnreadCountView(
                notifications.countUnread(authenticatedUser(authentication)));
    }

    @PatchMapping("/{id}/read")
    public MobileNotificationMutationView markRead(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        boolean updated = notifications.markRead(authenticatedUser(authentication), id);
        if (!updated) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Notificación no encontrada o ya leída");
        }
        return new MobileNotificationMutationView(1);
    }

    @PatchMapping("/read-all")
    public MobileNotificationMutationView markAllRead(Authentication authentication) {
        return new MobileNotificationMutationView(
                notifications.markAllRead(authenticatedUser(authentication)));
    }

    private AuthenticatedUser authenticatedUser(Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión requerida");
        }
        return user;
    }

    public record MobileNotificationView(
            UUID id,
            String title,
            String body,
            Map<String, String> data,
            String eventType,
            String status,
            Instant createdAt,
            Instant readAt,
            boolean unread
    ) {
        static MobileNotificationView from(MobileNotification notification) {
            return new MobileNotificationView(
                    notification.id(),
                    notification.title(),
                    notification.body(),
                    notification.data(),
                    notification.eventType(),
                    notification.status().name(),
                    notification.createdAt(),
                    notification.readAt(),
                    notification.unread());
        }
    }

    public record UnreadCountView(long unreadCount) {
    }

    public record MobileNotificationMutationView(int updated) {
    }
}
