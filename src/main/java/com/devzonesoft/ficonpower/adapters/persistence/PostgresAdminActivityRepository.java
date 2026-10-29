package com.devzonesoft.ficonpower.adapters.persistence;

import com.devzonesoft.ficonpower.domain.AdminActivityViews.AuditLogView;
import com.devzonesoft.ficonpower.domain.AdminActivityViews.AuthSessionView;
import com.devzonesoft.ficonpower.domain.AdminActivityViews.BlockedLoginUserView;
import com.devzonesoft.ficonpower.domain.AdminActivityViews.LoginAttemptView;
import com.devzonesoft.ficonpower.domain.PagedResult;
import com.devzonesoft.ficonpower.ports.output.AdminActivityRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresAdminActivityRepository implements AdminActivityRepository {

    private static final String BLOCKED_USER_COLUMNS = """
            b.id, b.username, b.user_id, u.display_name,
            b.ip_address, b.user_agent, b.failed_attempts,
            b.reason, b.blocked_at, b.expires_at,
            b.unblocked_at, b.unblocked_by, ub.username AS unblocked_by_username,
            b.unblock_reason,
            (b.unblocked_at IS NULL AND (b.expires_at IS NULL OR b.expires_at > now())) AS active
            """;

    private static final String BLOCKED_USER_FROM = """
            FROM blocked_login_users b
            LEFT JOIN app_users u ON u.id = b.user_id
            LEFT JOIN app_users ub ON ub.id = b.unblocked_by
            """;

    private static final String BLOCKED_USER_SELECT = "SELECT " + BLOCKED_USER_COLUMNS + BLOCKED_USER_FROM;

    private final JdbcTemplate jdbc;

    public PostgresAdminActivityRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<AuditLogView> findAuditLogs(int limit) {
        return jdbc.query("""
                        SELECT id, actor_user_id, actor_username, actor_role, action,
                               entity_type, entity_id, ip_address, user_agent,
                               metadata, created_at
                        FROM audit_logs
                        ORDER BY created_at DESC
                        LIMIT ?
                        """, this::mapAuditLog, limit);
    }

    @Override
    public PagedResult<AuditLogView> findAuditLogsPage(
            int page,
            int size,
            String search,
            String actor,
            String action,
            String entityType,
            String ipAddress
    ) {
        AdminFilters filters = new AdminFilters();
        filters.likeAny(search, "actor_username", "actor_role", "action", "entity_type", "entity_id", "ip_address", "user_agent", "metadata");
        filters.equalsIgnoreCase(actor, "actor_username");
        filters.equalsIgnoreCase(action, "action");
        filters.equalsIgnoreCase(entityType, "entity_type");
        filters.like(ipAddress, "ip_address");
        long total = count("FROM audit_logs", filters);
        List<Object> params = pagingParams(filters, page, size);
        List<AuditLogView> items = jdbc.query("""
                        SELECT id, actor_user_id, actor_username, actor_role, action,
                               entity_type, entity_id, ip_address, user_agent,
                               metadata, created_at
                        FROM audit_logs
                        """ + filters.where() + """
                        ORDER BY created_at DESC
                        LIMIT ? OFFSET ?
                        """, this::mapAuditLog, params.toArray());
        return PagedResult.of(items, page, size, total);
    }

    @Override
    public List<LoginAttemptView> findLoginAttempts(int limit) {
        return jdbc.query("""
                        SELECT id, username, user_id, ip_address, user_agent,
                               success, reason, created_at
                        FROM login_attempts
                        ORDER BY created_at DESC
                        LIMIT ?
                        """, this::mapLoginAttempt, limit);
    }

    @Override
    public PagedResult<LoginAttemptView> findLoginAttemptsPage(
            int page,
            int size,
            String search,
            String username,
            String success,
            String ipAddress
    ) {
        AdminFilters filters = new AdminFilters();
        filters.likeAny(search, "username", "ip_address", "user_agent", "reason");
        filters.equalsIgnoreCase(username, "username");
        filters.booleanValue(success, "success");
        filters.like(ipAddress, "ip_address");
        long total = count("FROM login_attempts", filters);
        List<Object> params = pagingParams(filters, page, size);
        List<LoginAttemptView> items = jdbc.query("""
                        SELECT id, username, user_id, ip_address, user_agent,
                               success, reason, created_at
                        FROM login_attempts
                        """ + filters.where() + """
                        ORDER BY created_at DESC
                        LIMIT ? OFFSET ?
                        """, this::mapLoginAttempt, params.toArray());
        return PagedResult.of(items, page, size, total);
    }

    @Override
    public List<AuthSessionView> findSessions(int limit) {
        return jdbc.query("""
                        SELECT s.id, s.user_id, u.username, u.role, s.ip_address,
                               s.user_agent, s.created_at, s.expires_at, s.revoked_at
                        FROM auth_sessions s
                        JOIN app_users u ON u.id = s.user_id
                        ORDER BY s.created_at DESC
                        LIMIT ?
                        """, this::mapAuthSession, limit);
    }

    @Override
    public PagedResult<AuthSessionView> findSessionsPage(
            int page,
            int size,
            String search,
            String username,
            String role,
            String ipAddress
    ) {
        AdminFilters filters = new AdminFilters();
        filters.likeAny(search, "u.username", "u.role", "s.ip_address", "s.user_agent");
        filters.equalsIgnoreCase(username, "u.username");
        filters.equalsIgnoreCase(role, "u.role");
        filters.like(ipAddress, "s.ip_address");
        String from = """
                FROM auth_sessions s
                JOIN app_users u ON u.id = s.user_id
                """;
        long total = count(from, filters);
        List<Object> params = pagingParams(filters, page, size);
        List<AuthSessionView> items = jdbc.query("""
                        SELECT s.id, s.user_id, u.username, u.role, s.ip_address,
                               s.user_agent, s.created_at, s.expires_at, s.revoked_at
                        FROM auth_sessions s
                        JOIN app_users u ON u.id = s.user_id
                        """ + filters.where() + """
                        ORDER BY s.created_at DESC
                        LIMIT ? OFFSET ?
                        """, this::mapAuthSession, params.toArray());
        return PagedResult.of(items, page, size, total);
    }

    @Override
    public List<BlockedLoginUserView> findBlockedUsers(int limit) {
        return jdbc.query(BLOCKED_USER_SELECT + """
                        ORDER BY active DESC, b.blocked_at DESC
                        LIMIT ?
                        """, this::mapBlockedUser, limit);
    }

    @Override
    public PagedResult<BlockedLoginUserView> findBlockedUsersPage(
            int page,
            int size,
            String search,
            String username,
            String active,
            String ipAddress
    ) {
        AdminFilters filters = new AdminFilters();
        filters.likeAny(search, "b.username", "u.display_name", "b.ip_address", "b.user_agent", "b.reason");
        filters.equalsIgnoreCase(username, "b.username");
        filters.like(ipAddress, "b.ip_address");
        filters.activeBlocked(active);
        long total = count(BLOCKED_USER_FROM, filters);
        List<Object> params = pagingParams(filters, page, size);
        List<BlockedLoginUserView> items = jdbc.query(BLOCKED_USER_SELECT + filters.where() + """
                        ORDER BY active DESC, b.blocked_at DESC
                        LIMIT ? OFFSET ?
                        """, this::mapBlockedUser, params.toArray());
        return PagedResult.of(items, page, size, total);
    }

    @Override
    public int unblock(UUID id, UUID actorId, String reason) {
        return jdbc.update("""
                        UPDATE blocked_login_users
                        SET unblocked_at = now(),
                            unblocked_by = ?,
                            unblock_reason = ?
                        WHERE id = ?
                        """, actorId, reason, id);
    }

    @Override
    public BlockedLoginUserView findBlockedUser(UUID id) {
        return jdbc.queryForObject(
                BLOCKED_USER_SELECT + "WHERE b.id = ?",
                this::mapBlockedUser,
                id);
    }

    AuditLogView mapAuditLog(ResultSet rs, int rowNum) throws SQLException {
        return new AuditLogView(
                rs.getString("id"),
                nullableUuidString(rs, "actor_user_id"),
                rs.getString("actor_username"),
                rs.getString("actor_role"),
                rs.getString("action"),
                rs.getString("entity_type"),
                rs.getString("entity_id"),
                rs.getString("ip_address"),
                rs.getString("user_agent"),
                rs.getString("metadata"),
                instant(rs, "created_at"));
    }

    LoginAttemptView mapLoginAttempt(ResultSet rs, int rowNum) throws SQLException {
        return new LoginAttemptView(
                rs.getString("id"),
                rs.getString("username"),
                nullableUuidString(rs, "user_id"),
                rs.getString("ip_address"),
                rs.getString("user_agent"),
                rs.getBoolean("success"),
                rs.getString("reason"),
                instant(rs, "created_at"));
    }

    AuthSessionView mapAuthSession(ResultSet rs, int rowNum) throws SQLException {
        return new AuthSessionView(
                uuidString(rs, "id"),
                uuidString(rs, "user_id"),
                rs.getString("username"),
                rs.getString("role"),
                rs.getString("ip_address"),
                rs.getString("user_agent"),
                instant(rs, "created_at"),
                instant(rs, "expires_at"),
                nullableInstant(rs, "revoked_at"));
    }

    BlockedLoginUserView mapBlockedUser(ResultSet rs, int rowNum) throws SQLException {
        return new BlockedLoginUserView(
                uuidString(rs, "id"),
                rs.getString("username"),
                nullableUuidString(rs, "user_id"),
                rs.getString("display_name"),
                rs.getString("ip_address"),
                rs.getString("user_agent"),
                rs.getInt("failed_attempts"),
                rs.getString("reason"),
                instant(rs, "blocked_at"),
                nullableInstant(rs, "expires_at"),
                nullableInstant(rs, "unblocked_at"),
                nullableUuidString(rs, "unblocked_by"),
                rs.getString("unblocked_by_username"),
                rs.getString("unblock_reason"),
                rs.getBoolean("active"));
    }

    private String uuidString(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, UUID.class).toString();
    }

    private String nullableUuidString(ResultSet rs, String column) throws SQLException {
        UUID value = rs.getObject(column, UUID.class);
        return value == null ? null : value.toString();
    }

    private java.time.Instant instant(ResultSet rs, String column) throws SQLException {
        return rs.getTimestamp(column).toInstant();
    }

    private java.time.Instant nullableInstant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private long count(String fromClause, AdminFilters filters) {
        Long total = jdbc.queryForObject(
                "SELECT count(*) " + fromClause + filters.where(),
                Long.class,
                filters.params().toArray());
        return total == null ? 0 : total;
    }

    private List<Object> pagingParams(AdminFilters filters, int page, int size) {
        List<Object> params = new ArrayList<>(filters.params());
        params.add(size);
        params.add((long) page * size);
        return params;
    }

    private static final class AdminFilters {
        private final List<String> clauses = new ArrayList<>();
        private final List<Object> params = new ArrayList<>();

        void likeAny(String value, String... columns) {
            if (isBlank(value) || columns.length == 0) {
                return;
            }
            StringBuilder clause = new StringBuilder("(");
            for (int index = 0; index < columns.length; index++) {
                if (index > 0) {
                    clause.append(" OR ");
                }
                clause.append("lower(coalesce(").append(columns[index]).append("::text, '')) LIKE ?");
                params.add("%" + value.trim().toLowerCase() + "%");
            }
            clause.append(")");
            clauses.add(clause.toString());
        }

        void like(String value, String column) {
            if (isBlank(value)) {
                return;
            }
            clauses.add("lower(coalesce(" + column + "::text, '')) LIKE ?");
            params.add("%" + value.trim().toLowerCase() + "%");
        }

        void equalsIgnoreCase(String value, String column) {
            if (isBlank(value)) {
                return;
            }
            clauses.add("lower(coalesce(" + column + "::text, '')) = ?");
            params.add(value.trim().toLowerCase());
        }

        void booleanValue(String value, String column) {
            if (isBlank(value)) {
                return;
            }
            if ("true".equalsIgnoreCase(value) || "success".equalsIgnoreCase(value)) {
                clauses.add(column + " = true");
            } else if ("false".equalsIgnoreCase(value) || "failed".equalsIgnoreCase(value)) {
                clauses.add(column + " = false");
            }
        }

        void activeBlocked(String value) {
            if (isBlank(value)) {
                return;
            }
            if ("true".equalsIgnoreCase(value) || "active".equalsIgnoreCase(value)) {
                clauses.add("(b.unblocked_at IS NULL AND (b.expires_at IS NULL OR b.expires_at > now()))");
            } else if ("false".equalsIgnoreCase(value) || "resolved".equalsIgnoreCase(value)) {
                clauses.add("(b.unblocked_at IS NOT NULL OR b.expires_at <= now())");
            }
        }

        String where() {
            return clauses.isEmpty() ? "" : " WHERE " + String.join(" AND ", clauses) + "\n";
        }

        List<Object> params() {
            return params;
        }

        private static boolean isBlank(String value) {
            return value == null || value.trim().isEmpty();
        }
    }
}
