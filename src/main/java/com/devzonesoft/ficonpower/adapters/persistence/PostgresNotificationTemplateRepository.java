package com.devzonesoft.ficonpower.adapters.persistence;

import com.devzonesoft.ficonpower.domain.NotificationEventType;
import com.devzonesoft.ficonpower.domain.NotificationTemplate;
import com.devzonesoft.ficonpower.ports.output.NotificationTemplateRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresNotificationTemplateRepository implements NotificationTemplateRepository {

    private final JdbcTemplate jdbcTemplate;

    public PostgresNotificationTemplateRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<NotificationTemplate> findAll() {
        return jdbcTemplate.query("""
                SELECT event_type, body
                FROM notification_templates
                ORDER BY event_type
                """, this::mapTemplate);
    }

    @Override
    public Optional<NotificationTemplate> findByEventType(NotificationEventType eventType) {
        List<NotificationTemplate> templates = jdbcTemplate.query("""
                SELECT event_type, body
                FROM notification_templates
                WHERE event_type = ?
                """, this::mapTemplate, eventType.code());
        return templates.stream().findFirst();
    }

    private NotificationTemplate mapTemplate(ResultSet rs, int rowNum) throws SQLException {
        return new NotificationTemplate(
                NotificationEventType.fromCode(rs.getString("event_type")),
                rs.getString("body"));
    }
}
