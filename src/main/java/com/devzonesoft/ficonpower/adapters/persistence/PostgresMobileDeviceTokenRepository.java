package com.devzonesoft.ficonpower.adapters.persistence;

import com.devzonesoft.ficonpower.domain.MobileDeviceToken;
import com.devzonesoft.ficonpower.ports.output.MobileDeviceTokenRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresMobileDeviceTokenRepository implements MobileDeviceTokenRepository {

    private final JdbcTemplate jdbc;

    public PostgresMobileDeviceTokenRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public MobileDeviceToken upsert(
            UUID userId,
            String token,
            String platform,
            String deviceName,
            String appVersion
    ) {
        return jdbc.query("""
                        INSERT INTO mobile_device_tokens (
                            user_id, token, platform, device_name, app_version, last_seen_at, revoked_at
                        )
                        VALUES (?, ?, ?, ?, ?, now(), null)
                        ON CONFLICT (token) DO UPDATE SET
                            user_id = EXCLUDED.user_id,
                            platform = EXCLUDED.platform,
                            device_name = EXCLUDED.device_name,
                            app_version = EXCLUDED.app_version,
                            last_seen_at = now(),
                            revoked_at = null
                        RETURNING id, user_id, token, platform, device_name, app_version, last_seen_at
                        """,
                this::map,
                userId,
                token,
                platform,
                deviceName,
                appVersion).getFirst();
    }

    @Override
    public void revoke(UUID userId, String token) {
        jdbc.update("""
                        UPDATE mobile_device_tokens
                        SET revoked_at = now()
                        WHERE user_id = ? AND token = ? AND revoked_at IS NULL
                        """,
                userId,
                token);
    }

    @Override
    public List<MobileDeviceToken> findActive() {
        return jdbc.query("""
                        SELECT id, user_id, token, platform, device_name, app_version, last_seen_at
                        FROM mobile_device_tokens
                        WHERE revoked_at IS NULL
                        ORDER BY last_seen_at DESC
                        """,
                this::map);
    }

    private MobileDeviceToken map(ResultSet rs, int rowNum) throws SQLException {
        return new MobileDeviceToken(
                rs.getObject("id", UUID.class),
                rs.getObject("user_id", UUID.class),
                rs.getString("token"),
                rs.getString("platform"),
                rs.getString("device_name"),
                rs.getString("app_version"),
                rs.getTimestamp("last_seen_at").toInstant());
    }
}
