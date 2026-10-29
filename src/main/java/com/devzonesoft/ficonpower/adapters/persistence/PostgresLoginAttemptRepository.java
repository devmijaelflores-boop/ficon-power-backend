package com.devzonesoft.ficonpower.adapters.persistence;

import com.devzonesoft.ficonpower.ports.output.LoginAttemptRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresLoginAttemptRepository implements LoginAttemptRepository {

    private final JdbcTemplate jdbc;

    public PostgresLoginAttemptRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean hasActiveBlock(String username, String ip) {
        Integer active = jdbc.queryForObject("""
                        SELECT count(*)
                        FROM blocked_login_users
                        WHERE lower(username) = lower(?)
                          AND ip_address = ?
                          AND unblocked_at IS NULL
                          AND (expires_at IS NULL OR expires_at > now())
                        """, Integer.class, username, ip);
        return active != null && active > 0;
    }

    @Override
    public int countFailedAttemptsSinceLastSuccess(
            String username,
            String ip,
            Instant windowStart
    ) {
        Timestamp start = Timestamp.from(windowStart);
        Integer failed = jdbc.queryForObject("""
                        SELECT count(*)
                        FROM login_attempts
                        WHERE lower(coalesce(username, '')) = lower(?)
                          AND ip_address = ?
                          AND success = false
                          AND created_at >= ?
                          AND created_at > coalesce((
                              SELECT max(created_at)
                              FROM login_attempts
                              WHERE lower(coalesce(username, '')) = lower(?)
                                AND ip_address = ?
                                AND success = true
                          ), ?)
                        """, Integer.class, username, ip, start, username, ip, start);
        return failed == null ? 0 : failed;
    }

    @Override
    public void upsertBlock(
            String username,
            UUID userId,
            String ip,
            String userAgent,
            int failedAttempts,
            Instant expiresAt
    ) {
        jdbc.update("""
                        INSERT INTO blocked_login_users (
                            username, user_id, ip_address, user_agent,
                            failed_attempts, reason, blocked_at, expires_at,
                            unblocked_at, unblocked_by, unblock_reason
                        )
                        VALUES (?, ?, ?, ?, ?, 'too_many_failed_attempts', now(), ?, null, null, null)
                        ON CONFLICT (username, ip_address) DO UPDATE
                        SET user_id = excluded.user_id,
                            user_agent = excluded.user_agent,
                            failed_attempts = excluded.failed_attempts,
                            reason = excluded.reason,
                            blocked_at = now(),
                            expires_at = excluded.expires_at,
                            unblocked_at = null,
                            unblocked_by = null,
                            unblock_reason = null
                        """,
                username,
                userId,
                ip,
                userAgent,
                failedAttempts,
                Timestamp.from(expiresAt));
    }

    @Override
    public void clearExpiredOrManualBlock(String username, String ip) {
        jdbc.update("""
                        UPDATE blocked_login_users
                        SET unblocked_at = coalesce(unblocked_at, now()),
                            unblock_reason = coalesce(unblock_reason, 'successful_login')
                        WHERE lower(username) = lower(?)
                          AND ip_address = ?
                          AND (unblocked_at IS NULL OR expires_at <= now())
                        """, username, ip);
    }

    @Override
    public void record(
            String username,
            UUID userId,
            String ip,
            String userAgent,
            boolean success,
            String reason
    ) {
        jdbc.update("""
                        INSERT INTO login_attempts (username, user_id, ip_address, user_agent, success, reason)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """, username, userId, ip, userAgent, success, reason);
    }
}
