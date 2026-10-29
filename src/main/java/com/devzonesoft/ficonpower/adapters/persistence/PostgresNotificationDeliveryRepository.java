package com.devzonesoft.ficonpower.adapters.persistence;

import com.devzonesoft.ficonpower.domain.NotificationDelivery;
import com.devzonesoft.ficonpower.domain.MobileNotification;
import com.devzonesoft.ficonpower.domain.NotificationDeliveryStatus;
import com.devzonesoft.ficonpower.ports.output.NotificationDeliveryRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Repository
public class PostgresNotificationDeliveryRepository implements NotificationDeliveryRepository {

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public PostgresNotificationDeliveryRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(NotificationDelivery delivery) {
        if (isPushUpsert(delivery)) {
            upsertPushDelivery(delivery);
            return;
        }
        insertDelivery(delivery);
    }

    private boolean isPushUpsert(NotificationDelivery delivery) {
        return delivery.channel() == com.devzonesoft.ficonpower.domain.NotificationChannelType.PUSH
                && delivery.userId() != null
                && delivery.notificationKey() != null;
    }

    private void upsertPushDelivery(NotificationDelivery delivery) {
        int updated = updatePushDelivery(delivery);
        if (updated > 0) {
            return;
        }
        try {
            insertDelivery(delivery);
        } catch (DuplicateKeyException ignored) {
            updatePushDelivery(delivery);
        }
    }

    private void insertDelivery(NotificationDelivery delivery) {
        jdbc.update("""
                        INSERT INTO notification_deliveries (
                            notification_key, incident_id, event_id, user_id,
                            channel, provider, recipient, event_type,
                            template_name, status, provider_message_id, error_code, error_message,
                            attempt_count, sent_at, title, body, data
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb))
                        """,
                delivery.notificationKey(),
                delivery.incidentId(),
                delivery.eventId(),
                delivery.userId(),
                delivery.channel().name(),
                delivery.provider(),
                delivery.recipient(),
                delivery.eventType() == null ? null : delivery.eventType().code(),
                delivery.templateName(),
                delivery.status().name(),
                delivery.providerMessageId(),
                delivery.errorCode(),
                delivery.errorMessage(),
                delivery.attemptCount(),
                delivery.sentAt(),
                delivery.title(),
                delivery.body(),
                objectMapper.writeValueAsString(delivery.data()));
    }

    private int updatePushDelivery(NotificationDelivery delivery) {
        return jdbc.update("""
                        UPDATE notification_deliveries
                        SET incident_id = COALESCE(?, incident_id),
                            event_id = COALESCE(?, event_id),
                            provider = ?,
                            recipient = ?,
                            event_type = ?,
                            status = ?,
                            provider_message_id = ?,
                            error_code = ?,
                            error_message = ?,
                            attempt_count = attempt_count + 1,
                            sent_at = ?,
                            title = ?,
                            body = ?,
                            data = CAST(? AS jsonb),
                            read_at = NULL,
                            created_at = now()
                        WHERE user_id = ?
                          AND notification_key = ?
                          AND channel = 'PUSH'
                          AND notification_key IS NOT NULL
                        """,
                delivery.incidentId(),
                delivery.eventId(),
                delivery.provider(),
                delivery.recipient(),
                delivery.eventType() == null ? null : delivery.eventType().code(),
                delivery.status().name(),
                delivery.providerMessageId(),
                delivery.errorCode(),
                delivery.errorMessage(),
                delivery.sentAt(),
                delivery.title(),
                delivery.body(),
                objectMapper.writeValueAsString(delivery.data()),
                delivery.userId(),
                delivery.notificationKey());
    }

    @Override
    public List<MobileNotification> findByUser(UUID userId, int limit) {
        return jdbc.query("""
                        SELECT *
                        FROM (
                            SELECT DISTINCT ON (notification_key)
                                   notification_key, title, body, data::text AS data,
                                   event_type, status, created_at, read_at
                            FROM notification_deliveries
                            WHERE user_id = ?
                              AND channel = 'PUSH'
                              AND notification_key IS NOT NULL
                            ORDER BY notification_key, created_at DESC
                        ) notifications
                        ORDER BY created_at DESC
                        LIMIT ?
                        """, this::mapMobileNotification, userId, limit);
    }

    @Override
    public long countUnread(UUID userId) {
        Long count = jdbc.queryForObject("""
                        SELECT count(DISTINCT notification_key)
                        FROM notification_deliveries
                        WHERE user_id = ?
                          AND channel = 'PUSH'
                          AND notification_key IS NOT NULL
                          AND read_at IS NULL
                        """, Long.class, userId);
        return count == null ? 0 : count;
    }

    @Override
    public boolean markRead(UUID userId, UUID notificationId) {
        return jdbc.update("""
                        UPDATE notification_deliveries
                        SET read_at = COALESCE(read_at, now()),
                            status = CASE WHEN status = 'SENT' THEN 'READ' ELSE status END
                        WHERE user_id = ? AND notification_key = ? AND read_at IS NULL
                        """, userId, notificationId) > 0;
    }

    @Override
    public int markAllRead(UUID userId) {
        return jdbc.update("""
                        UPDATE notification_deliveries
                        SET read_at = now(),
                            status = CASE WHEN status = 'SENT' THEN 'READ' ELSE status END
                        WHERE user_id = ? AND channel = 'PUSH' AND read_at IS NULL
                        """, userId);
    }

    @Override
    public List<UUID> findIncidentIdsByNotificationKey(UUID userId, UUID notificationId) {
        return jdbc.queryForList("""
                        SELECT DISTINCT incident_id
                        FROM notification_deliveries
                        WHERE user_id = ?
                          AND notification_key = ?
                          AND event_type = 'power_outage'
                          AND incident_id IS NOT NULL
                        """, UUID.class, userId, notificationId);
    }

    @Override
    public List<UUID> findIncidentIdsByUser(UUID userId) {
        return jdbc.queryForList("""
                        SELECT DISTINCT incident_id
                        FROM notification_deliveries
                        WHERE user_id = ?
                          AND incident_id IS NOT NULL
                        """, UUID.class, userId);
    }

    private MobileNotification mapMobileNotification(ResultSet rs, int rowNum) throws SQLException {
        return new MobileNotification(
                rs.getObject("notification_key", UUID.class),
                rs.getString("title"),
                rs.getString("body"),
                readData(rs.getString("data")),
                rs.getString("event_type"),
                NotificationDeliveryStatus.valueOf(rs.getString("status")),
                instant(rs, "created_at"),
                instant(rs, "read_at"));
    }

    private Map<String, String> readData(String json) throws SQLException {
        try {
            return objectMapper.readValue(
                    json == null ? "{}" : json,
                    new TypeReference<Map<String, String>>() { });
        } catch (RuntimeException exception) {
            throw new SQLException("No se pudo decodificar data de notificación", exception);
        }
    }

    private Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }
}
