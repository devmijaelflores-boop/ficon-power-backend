package com.devzonesoft.ficonpower.usecase;

import com.devzonesoft.ficonpower.domain.AppUser;
import com.devzonesoft.ficonpower.domain.UserRole;
import com.devzonesoft.ficonpower.ports.output.UserRepository;
import com.devzonesoft.ficonpower.ports.output.WhatsAppRecipientRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserManagementService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final WhatsAppRecipientRepository whatsappRecipients;

    public UserManagementService(
            UserRepository users,
            PasswordEncoder passwordEncoder,
            WhatsAppRecipientRepository whatsappRecipients
    ) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.whatsappRecipients = whatsappRecipients;
    }

    @Transactional
    public AppUser create(String username, String password, UserRole role, String displayName,
            String email, String whatsappPhone, boolean whatsappEnabled) {
        if (role == UserRole.SUPERADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "SUPERADMIN no es un rol asignable desde el dashboard");
        }
        AppUser user = users.create(username, passwordEncoder.encode(password), role,
                displayName, normalizeEmail(email));
        updateWhatsApp(user, whatsappPhone, whatsappEnabled);
        return user;
    }

    @Transactional
    public AppUser update(
            UUID id,
            UserRole role,
            String displayName,
            String email,
            boolean active,
            String password,
            String whatsappPhone,
            boolean whatsappEnabled
    ) {
        AppUser current = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuario no encontrado"));
        if (current.role() == UserRole.SUPERADMIN
                && (!active || role != UserRole.SUPERADMIN)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Los superadmins no se reasignan ni revocan desde el dashboard");
        }
        if (current.role() != UserRole.SUPERADMIN && role == UserRole.SUPERADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "SUPERADMIN no es un rol asignable desde el dashboard");
        }
        if (current.role() == UserRole.SUPERADMIN
                && (!active || role != UserRole.SUPERADMIN)
                && users.countActiveByRole(UserRole.SUPERADMIN) <= 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe existir al menos un superadmin activo");
        }

        AppUser updated = users.update(id, role, displayName, normalizeEmail(email), active);
        if (password != null && !password.isBlank()) {
            updated = users.updatePassword(id, passwordEncoder.encode(password));
        }
        updateWhatsApp(updated, whatsappPhone, whatsappEnabled);
        return updated;
    }

    private void updateWhatsApp(AppUser user, String phone, boolean enabled) {
        if (phone == null || phone.isBlank()) {
            whatsappRecipients.findByUserId(user.id())
                    .ifPresent(recipient -> whatsappRecipients.delete(recipient.id()));
            return;
        }
        whatsappRecipients.upsertForUser(
                user.id(),
                user.displayName(),
                WhatsAppRecipientService.requirePeruMobile(phone),
                enabled);
    }

    private String normalizeEmail(String email) {
        return email == null || email.isBlank() ? null : email.trim().toLowerCase();
    }
}
