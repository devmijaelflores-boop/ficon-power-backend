package com.devzonesoft.ficonpower.ports.output;

import com.devzonesoft.ficonpower.domain.AppUser;
import com.devzonesoft.ficonpower.domain.UserRole;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    Optional<AppUser> findByUsername(String username);

    Optional<AppUser> findById(UUID id);

    List<AppUser> findAll();

    int countActiveByRole(UserRole role);

    AppUser create(String username, String passwordHash, UserRole role, String displayName,
            String email);

    AppUser update(UUID id, UserRole role, String displayName, String email, boolean active);

    AppUser updatePassword(UUID id, String passwordHash);
}
