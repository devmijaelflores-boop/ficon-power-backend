package com.devzonesoft.ficonpower.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.devzonesoft.ficonpower.domain.AppUser;
import com.devzonesoft.ficonpower.domain.UserRole;
import com.devzonesoft.ficonpower.ports.output.UserRepository;
import com.devzonesoft.ficonpower.ports.output.WhatsAppRecipientRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

class UserManagementServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final WhatsAppRecipientRepository whatsappRecipients =
            mock(WhatsAppRecipientRepository.class);
    private final UserManagementService service =
            new UserManagementService(users, passwordEncoder, whatsappRecipients);

    @Test
    void createsAUserWithAnEncodedPassword() {
        AppUser created = user(UserRole.ADMIN, true);
        when(passwordEncoder.encode("StrongPassword!2026")).thenReturn("encoded-password");
        when(users.create("admin", "encoded-password", UserRole.ADMIN, "Administrador",
                "admin@ficon.pe"))
                .thenReturn(created);

        AppUser result = service.create(
                "admin",
                "StrongPassword!2026",
                UserRole.ADMIN,
                "Administrador", "  ADMIN@FICON.PE ", "+51 987 654 321", true);

        assertSame(created, result);
        verify(whatsappRecipients).upsertForUser(
                created.id(), "User", "51987654321", true);
    }

    @Test
    void rejectsCreatingASuperadminFromTheDashboard() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.create(
                        "root",
                        "StrongPassword!2026",
                        UserRole.SUPERADMIN,
                        "Superadmin", null, null, false));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(users, passwordEncoder);
    }

    @Test
    void rejectsUpdatingAnUnknownUser() {
        UUID id = UUID.randomUUID();
        when(users.findById(id)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.update(id, UserRole.USER, "Usuario", null, true, null, null, false));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void preventsPromotingARegularUserToSuperadmin() {
        AppUser current = user(UserRole.ADMIN, true);
        when(users.findById(current.id())).thenReturn(Optional.of(current));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.update(
                        current.id(), UserRole.SUPERADMIN, "Administrador", null,
                        true, null, null, false));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(users, never()).update(
                current.id(), UserRole.SUPERADMIN, "Administrador", null, true);
    }

    @Test
    void preventsDeactivatingASuperadmin() {
        AppUser current = user(UserRole.SUPERADMIN, true);
        when(users.findById(current.id())).thenReturn(Optional.of(current));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.update(
                        current.id(), UserRole.SUPERADMIN, "Superadmin", null,
                        false, null, null, false));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(users, never()).update(
                current.id(), UserRole.SUPERADMIN, "Superadmin", null, false);
    }

    @Test
    void updatesThePasswordOnlyWhenANewOneIsProvided() {
        AppUser current = user(UserRole.USER, true);
        AppUser updated = user(UserRole.ADMIN, true);
        AppUser withPassword = user(UserRole.ADMIN, true);
        when(users.findById(current.id())).thenReturn(Optional.of(current));
        when(users.update(current.id(), UserRole.ADMIN, "Nuevo nombre", null, true))
                .thenReturn(updated);
        when(passwordEncoder.encode("NewPassword!2026")).thenReturn("new-password-hash");
        when(users.updatePassword(current.id(), "new-password-hash")).thenReturn(withPassword);

        AppUser result = service.update(
                current.id(), UserRole.ADMIN, "Nuevo nombre", null, true,
                "NewPassword!2026", null, false);

        assertSame(withPassword, result);
        verify(users).updatePassword(current.id(), "new-password-hash");
    }

    @Test
    void keepsThePasswordWhenTheProvidedValueIsBlank() {
        AppUser current = user(UserRole.USER, true);
        AppUser updated = user(UserRole.USER, true);
        when(users.findById(current.id())).thenReturn(Optional.of(current));
        when(users.update(current.id(), UserRole.USER, "Usuario", null, true)).thenReturn(updated);

        AppUser result = service.update(current.id(), UserRole.USER, "Usuario", null,
                true, "   ", null, false);

        assertSame(updated, result);
        verify(users, never()).updatePassword(eq(current.id()), anyString());
        verifyNoInteractions(passwordEncoder);
    }

    private AppUser user(UserRole role, boolean active) {
        Instant now = Instant.now();
        return new AppUser(
                UUID.randomUUID(),
                "user",
                "password-hash",
                role,
                "User",
                active,
                now,
                now);
    }
}
