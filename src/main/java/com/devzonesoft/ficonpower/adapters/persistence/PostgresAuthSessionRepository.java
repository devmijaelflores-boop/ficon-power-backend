package com.devzonesoft.ficonpower.adapters.persistence;

import com.devzonesoft.ficonpower.domain.AuthenticatedUser;
import com.devzonesoft.ficonpower.domain.RefreshSession;
import com.devzonesoft.ficonpower.domain.UserRole;
import com.devzonesoft.ficonpower.ports.output.AuthSessionRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresAuthSessionRepository implements AuthSessionRepository {

    private final JdbcTemplate jdbc;

    public PostgresAuthSessionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void create(
            UUID userId,
            String tokenHash,
            String refreshTokenHash,
            String ip,
            String userAgent,
            Instant expiresAt,
            Instant refreshExpiresAt
    ) {
        jdbc.update("""
                        INSERT INTO auth_sessions (
                            user_id, token_hash, refresh_token_hash,
                            ip_address, user_agent, expires_at, refresh_expires_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                userId,
                tokenHash,
                refreshTokenHash,
                ip,
                userAgent,
                Timestamp.from(expiresAt),
                Timestamp.from(refreshExpiresAt));
    }

    @Override
    public Optional<RefreshSession> findRefreshSession(String refreshTokenHash) {
        return jdbc.query("""
                        SELECT s.id AS session_id,
                               u.id, u.username, u.role, u.display_name, u.email,
                               r.phone AS whatsapp_phone
                        FROM auth_sessions s
                        JOIN app_users u ON u.id = s.user_id
                        LEFT JOIN whatsapp_recipients r ON r.user_id = u.id
                        WHERE s.refresh_token_hash = ?
                          AND s.revoked_at IS NULL
                          AND s.refresh_expires_at > now()
                          AND u.is_active = true
                        """, (rs, rowNum) -> new RefreshSession(
                                rs.getObject("session_id", UUID.class),
                                mapAuthenticatedUser(rs, rowNum)), refreshTokenHash)
                .stream()
                .findFirst();
    }

    @Override
    public boolean rotate(
            UUID sessionId,
            String previousRefreshTokenHash,
            String tokenHash,
            String refreshTokenHash,
            String ip,
            String userAgent,
            Instant expiresAt,
            Instant refreshExpiresAt
    ) {
        return jdbc.update("""
                        UPDATE auth_sessions
                        SET token_hash = ?,
                            refresh_token_hash = ?,
                            ip_address = ?,
                            user_agent = ?,
                            expires_at = ?,
                            refresh_expires_at = ?,
                            last_refreshed_at = now()
                        WHERE id = ?
                          AND refresh_token_hash = ?
                          AND revoked_at IS NULL
                          AND refresh_expires_at > now()
                        """,
                tokenHash,
                refreshTokenHash,
                ip,
                userAgent,
                Timestamp.from(expiresAt),
                Timestamp.from(refreshExpiresAt),
                sessionId,
                previousRefreshTokenHash) == 1;
    }

    @Override
    public Optional<AuthenticatedUser> findAuthenticatedUser(String tokenHash) {
        return jdbc.query("""
                        SELECT u.id, u.username, u.role, u.display_name, u.email,
                               r.phone AS whatsapp_phone
                        FROM auth_sessions s
                        JOIN app_users u ON u.id = s.user_id
                        LEFT JOIN whatsapp_recipients r ON r.user_id = u.id
                        WHERE s.token_hash = ?
                          AND s.revoked_at IS NULL
                          AND s.expires_at > now()
                          AND u.is_active = true
                        """, this::mapAuthenticatedUser, tokenHash)
                .stream()
                .findFirst();
    }

    @Override
    public void revoke(String tokenHash) {
        jdbc.update("""
                        UPDATE auth_sessions
                        SET revoked_at = now()
                        WHERE token_hash = ? AND revoked_at IS NULL
                """, tokenHash);
    }

    @Override
    public void revokeByRefreshToken(String refreshTokenHash) {
        jdbc.update("""
                        UPDATE auth_sessions
                        SET revoked_at = now()
                        WHERE refresh_token_hash = ? AND revoked_at IS NULL
                        """, refreshTokenHash);
    }

    @Override
    public boolean revokeById(UUID sessionId) {
        return jdbc.update("""
                        UPDATE auth_sessions
                        SET revoked_at = now()
                        WHERE id = ? AND revoked_at IS NULL
                        """, sessionId) == 1;
    }

    AuthenticatedUser mapAuthenticatedUser(ResultSet rs, int rowNum) throws SQLException {
        return new AuthenticatedUser(
                rs.getObject("id", UUID.class),
                rs.getString("username"),
                UserRole.valueOf(rs.getString("role")),
                rs.getString("display_name"),
                rs.getString("email"),
                rs.getString("whatsapp_phone"));
    }
}
