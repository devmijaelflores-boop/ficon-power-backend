package com.devzonesoft.ficonpower.adapters.persistence;

import com.devzonesoft.ficonpower.domain.AppUser;
import com.devzonesoft.ficonpower.domain.UserRole;
import com.devzonesoft.ficonpower.ports.output.UserRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresUserRepository implements UserRepository {

    private final JdbcTemplate jdbc;

    public PostgresUserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<AppUser> findByUsername(String username) {
        return jdbc.query("""
                        SELECT id, username, password_hash, role, display_name, email, is_active,
                               created_at, updated_at
                        FROM app_users
                        WHERE lower(username) = lower(?)
                        """, this::mapUser, username)
                .stream()
                .findFirst();
    }

    @Override
    public Optional<AppUser> findById(UUID id) {
        return jdbc.query("""
                        SELECT id, username, password_hash, role, display_name, email, is_active,
                               created_at, updated_at
                        FROM app_users
                        WHERE id = ?
                        """, this::mapUser, id)
                .stream()
                .findFirst();
    }

    @Override
    public List<AppUser> findAll() {
        return jdbc.query("""
                        SELECT id, username, password_hash, role, display_name, email, is_active,
                               created_at, updated_at
                        FROM app_users
                        ORDER BY username
                        """, this::mapUser);
    }

    @Override
    public int countActiveByRole(UserRole role) {
        Integer count = jdbc.queryForObject("""
                        SELECT count(*)
                        FROM app_users
                        WHERE role = ? AND is_active = true
                        """, Integer.class, role.name());
        return count == null ? 0 : count;
    }

    @Override
    public AppUser create(String username, String passwordHash, UserRole role, String displayName,
            String email) {
        return jdbc.queryForObject("""
                        INSERT INTO app_users (username, password_hash, role, display_name, email)
                        VALUES (?, ?, ?, ?, ?)
                        RETURNING id, username, password_hash, role, display_name, email, is_active,
                                  created_at, updated_at
                        """, this::mapUser, username, passwordHash, role.name(), displayName, email);
    }

    @Override
    public AppUser update(UUID id, UserRole role, String displayName, String email, boolean active) {
        return jdbc.queryForObject("""
                        UPDATE app_users
                        SET role = ?, display_name = ?, email = ?, is_active = ?
                        WHERE id = ?
                        RETURNING id, username, password_hash, role, display_name, email, is_active,
                                  created_at, updated_at
                        """, this::mapUser, role.name(), displayName, email, active, id);
    }

    @Override
    public AppUser updatePassword(UUID id, String passwordHash) {
        return jdbc.queryForObject("""
                        UPDATE app_users
                        SET password_hash = ?
                        WHERE id = ?
                        RETURNING id, username, password_hash, role, display_name, email, is_active,
                                  created_at, updated_at
                        """, this::mapUser, passwordHash, id);
    }

    AppUser mapUser(ResultSet rs, int rowNum) throws SQLException {
        return new AppUser(
                rs.getObject("id", UUID.class),
                rs.getString("username"),
                rs.getString("password_hash"),
                UserRole.valueOf(rs.getString("role")),
                rs.getString("display_name"),
                rs.getString("email"),
                rs.getBoolean("is_active"),
                toInstant(rs.getTimestamp("created_at")),
                toInstant(rs.getTimestamp("updated_at"))
        );
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
