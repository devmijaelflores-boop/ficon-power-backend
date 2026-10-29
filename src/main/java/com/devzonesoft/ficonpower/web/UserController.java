package com.devzonesoft.ficonpower.web;

import com.devzonesoft.ficonpower.domain.AppUser;
import com.devzonesoft.ficonpower.domain.UserWhatsAppDelivery;
import com.devzonesoft.ficonpower.domain.UserRole;
import com.devzonesoft.ficonpower.domain.WhatsAppRecipient;
import com.devzonesoft.ficonpower.domain.WhatsAppTestResult;
import com.devzonesoft.ficonpower.ports.output.UserRepository;
import com.devzonesoft.ficonpower.ports.output.WhatsAppRecipientRepository;
import com.devzonesoft.ficonpower.usecase.AuditLogService;
import com.devzonesoft.ficonpower.usecase.UserManagementService;
import com.devzonesoft.ficonpower.usecase.WhatsAppRecipientService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository users;
    private final UserManagementService userManagement;
    private final AuditLogService audit;
    private final WhatsAppRecipientRepository whatsappRecipients;
    private final WhatsAppRecipientService whatsappService;

    public UserController(
            UserRepository users,
            UserManagementService userManagement,
            AuditLogService audit,
            WhatsAppRecipientRepository whatsappRecipients,
            WhatsAppRecipientService whatsappService
    ) {
        this.users = users;
        this.userManagement = userManagement;
        this.audit = audit;
        this.whatsappRecipients = whatsappRecipients;
        this.whatsappService = whatsappService;
    }

    @GetMapping
    public List<UserView> users() {
        return users.findAll().stream().map(this::view).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserView create(@Valid @RequestBody CreateUserRequest request, HttpServletRequest servletRequest) {
        AppUser user = userManagement.create(
                request.username(),
                request.password(),
                parseRole(request.role()),
                request.displayName(),
                request.email(),
                request.whatsappPhone(),
                request.whatsappEnabled());
        audit.record("user.create", "app_user", user.id().toString(),
                "{\"username\":\"" + user.username() + "\"}",
                HttpRequestMetadata.from(servletRequest));
        return view(user);
    }

    @PutMapping("/{id}")
    public UserView update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request,
            HttpServletRequest servletRequest
    ) {
        AppUser user = userManagement.update(
                id,
                parseRole(request.role()),
                request.displayName(),
                request.email(),
                request.active(),
                request.password(),
                request.whatsappPhone(),
                request.whatsappEnabled());
        audit.record("user.update", "app_user", user.id().toString(),
                "{\"username\":\"" + user.username() + "\"}",
                HttpRequestMetadata.from(servletRequest));
        return view(user);
    }

    @GetMapping("/{id}/whatsapp-deliveries")
    public List<UserWhatsAppDelivery> whatsappDeliveries(@PathVariable UUID id) {
        if (users.findById(id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
        }
        return whatsappRecipients.findDeliveriesByUserId(id, 50);
    }

    @PostMapping("/{id}/whatsapp-test")
    public WhatsAppTestResult whatsappTest(
            @PathVariable UUID id,
            HttpServletRequest servletRequest
    ) {
        WhatsAppTestResult result = whatsappService.testUser(id);
        audit.record("user.whatsapp_test", "app_user", id.toString(),
                "{\"accepted\":" + result.accepted() + "}",
                HttpRequestMetadata.from(servletRequest));
        return result;
    }

    private UserView view(AppUser user) {
        WhatsAppRecipient recipient = whatsappRecipients.findByUserId(user.id()).orElse(null);
        return UserView.from(user, recipient);
    }

    private UserRole parseRole(String role) {
        try {
            return UserRole.valueOf(role.toUpperCase());
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rol invalido: " + role);
        }
    }

    public record UserView(
            String id,
            String username,
            String role,
            String displayName,
            String email,
            String whatsappPhone,
            boolean whatsappEnabled,
            boolean active
    ) {
        static UserView from(AppUser user, WhatsAppRecipient recipient) {
            return new UserView(
                    user.id().toString(),
                    user.username(),
                    user.role().name(),
                    user.displayName(),
                    user.email(),
                    recipient == null ? null : recipient.phone(),
                    recipient != null && recipient.active(),
                    user.active());
        }
    }

    public record CreateUserRequest(
            @NotBlank @Size(max = 60) String username,
            @NotBlank @Size(min = 8, max = 120) String password,
            @NotBlank String role,
            @NotBlank @Size(max = 120) String displayName,
            @Email @Size(max = 254) String email,
            @Size(max = 30) String whatsappPhone,
            boolean whatsappEnabled
    ) {
    }

    public record UpdateUserRequest(
            @NotBlank String role,
            @NotBlank @Size(max = 120) String displayName,
            @Email @Size(max = 254) String email,
            boolean active,
            @Size(min = 8, max = 120) String password,
            @Size(max = 30) String whatsappPhone,
            boolean whatsappEnabled
    ) {
    }
}
