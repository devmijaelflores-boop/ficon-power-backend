package com.devzonesoft.ficonpower.adapters.persistence;

import com.devzonesoft.ficonpower.domain.NotificationEventType;
import com.devzonesoft.ficonpower.domain.NotificationDeliveryStatus;
import com.devzonesoft.ficonpower.domain.UserWhatsAppDelivery;
import com.devzonesoft.ficonpower.domain.WhatsAppRecipient;
import com.devzonesoft.ficonpower.ports.output.WhatsAppRecipientRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresWhatsAppRecipientRepository implements WhatsAppRecipientRepository {

    private static final String SELECT = """
            SELECT r.id, r.user_id, r.label, r.phone, r.site_id, s.name AS site_name,
                   r.notify_outage, r.notify_restored, r.is_active,
                   r.last_test_status, r.last_test_message, r.last_test_at,
                   r.created_at, r.updated_at
            FROM whatsapp_recipients r
            LEFT JOIN sites s ON s.id = r.site_id
            """;

    private final JdbcTemplate jdbc;

    public PostgresWhatsAppRecipientRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<WhatsAppRecipient> findAll() {
        return jdbc.query(SELECT
                + " ORDER BY r.is_active DESC, r.user_id NULLS FIRST, r.label, r.phone",
                this::map);
    }

    @Override
    public boolean hasAny() {
        Boolean found = jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM whatsapp_recipients)", Boolean.class);
        return Boolean.TRUE.equals(found);
    }

    @Override
    public Optional<WhatsAppRecipient> findById(UUID id) {
        return jdbc.query(SELECT + " WHERE r.id = ?", this::map, id).stream().findFirst();
    }

    @Override
    public Optional<WhatsAppRecipient> findByUserId(UUID userId) {
        return jdbc.query(SELECT + " WHERE r.user_id = ?", this::map, userId)
                .stream().findFirst();
    }

    @Override
    public List<WhatsAppRecipient> findActiveForIncident(
            UUID incidentId,
            NotificationEventType eventType
    ) {
        if (incidentId == null) return List.of();
        String eventColumn = eventType == NotificationEventType.POWER_OUTAGE
                ? "r.notify_outage" : "r.notify_restored";
        String sql = SELECT
                + " JOIN incidents i ON i.id = ?"
                + " JOIN devices d ON d.id = i.device_id"
                + " WHERE r.is_active = true"
                + " AND (r.user_id IS NULL OR EXISTS ("
                + "     SELECT 1 FROM app_users u WHERE u.id = r.user_id AND u.is_active = true))"
                + " AND " + eventColumn + " = true"
                + " AND (r.site_id IS NULL OR r.site_id = d.site_id)"
                + " ORDER BY r.site_id NULLS LAST, r.label";
        return jdbc.query(sql, this::map, incidentId);
    }

    @Override
    public WhatsAppRecipient create(String label, String phone, UUID siteId,
            boolean notifyOutage, boolean notifyRestored, boolean active) {
        UUID id = jdbc.queryForObject("""
                INSERT INTO whatsapp_recipients
                    (label, phone, site_id, notify_outage, notify_restored, is_active)
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING id
                """, UUID.class, label, phone, siteId, notifyOutage, notifyRestored, active);
        return findById(id).orElseThrow();
    }

    @Override
    public Optional<WhatsAppRecipient> update(UUID id, String label, String phone, UUID siteId,
            boolean notifyOutage, boolean notifyRestored, boolean active) {
        int updated = jdbc.update("""
                UPDATE whatsapp_recipients
                SET label = ?, phone = ?, site_id = ?, notify_outage = ?,
                    notify_restored = ?, is_active = ?
                WHERE id = ?
                """, label, phone, siteId, notifyOutage, notifyRestored, active, id);
        return updated == 0 ? Optional.empty() : findById(id);
    }

    @Override
    public WhatsAppRecipient upsertForUser(
            UUID userId,
            String label,
            String phone,
            boolean active
    ) {
        UUID id = jdbc.queryForObject("""
                INSERT INTO whatsapp_recipients
                    (user_id, label, phone, site_id, notify_outage, notify_restored, is_active)
                VALUES (?, ?, ?, NULL, true, true, ?)
                ON CONFLICT (user_id) WHERE user_id IS NOT NULL DO UPDATE
                SET label = EXCLUDED.label,
                    phone = EXCLUDED.phone,
                    site_id = NULL,
                    notify_outage = true,
                    notify_restored = true,
                    is_active = EXCLUDED.is_active
                RETURNING id
                """, UUID.class, userId, label, phone, active);
        return findById(id).orElseThrow();
    }

    @Override
    public List<UserWhatsAppDelivery> findDeliveriesByUserId(UUID userId, int limit) {
        return jdbc.query("""
                SELECT id, event_type, template_name, status, error_message, sent_at, created_at
                FROM notification_deliveries
                WHERE user_id = ? AND channel = 'WHATSAPP'
                ORDER BY created_at DESC
                LIMIT ?
                """, (rs, rowNum) -> new UserWhatsAppDelivery(
                        rs.getLong("id"),
                        rs.getString("event_type"),
                        rs.getString("template_name"),
                        NotificationDeliveryStatus.valueOf(rs.getString("status")),
                        rs.getString("error_message"),
                        instant(rs.getTimestamp("sent_at")),
                        instant(rs.getTimestamp("created_at"))), userId, limit);
    }

    @Override
    public boolean delete(UUID id) {
        return jdbc.update("DELETE FROM whatsapp_recipients WHERE id = ?", id) > 0;
    }

    @Override
    public void recordTest(UUID id, boolean accepted, String message) {
        jdbc.update("""
                UPDATE whatsapp_recipients
                SET last_test_status = ?, last_test_message = ?, last_test_at = now()
                WHERE id = ?
                """, accepted ? "SENT" : "FAILED", message, id);
    }

    private WhatsAppRecipient map(ResultSet rs, int rowNum) throws SQLException {
        return new WhatsAppRecipient(
                rs.getObject("id", UUID.class), rs.getObject("user_id", UUID.class),
                rs.getString("label"), rs.getString("phone"),
                rs.getObject("site_id", UUID.class), rs.getString("site_name"),
                rs.getBoolean("notify_outage"), rs.getBoolean("notify_restored"),
                rs.getBoolean("is_active"), rs.getString("last_test_status"),
                rs.getString("last_test_message"),
                rs.getTimestamp("last_test_at") == null ? null : rs.getTimestamp("last_test_at").toInstant(),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }

    private java.time.Instant instant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
