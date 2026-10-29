package com.devzonesoft.ficonpower.web;

import com.devzonesoft.ficonpower.domain.AuthenticatedUser;
import com.devzonesoft.ficonpower.usecase.AuditLogService;
import com.devzonesoft.ficonpower.usecase.AuthSessionService;
import com.devzonesoft.ficonpower.usecase.AuthService;
import com.devzonesoft.ficonpower.usecase.LogoutService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthSessionService authSessions;
    private final LogoutService logoutService;
    private final AuditLogService audit;

    public AuthController(
            AuthService authService,
            AuthSessionService authSessions,
            LogoutService logoutService,
            AuditLogService audit
    ) {
        this.authService = authService;
        this.authSessions = authSessions;
        this.logoutService = logoutService;
        this.audit = audit;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest
    ) {
        AuthService.LoginResult result = authService.login(
                request.username(),
                request.password(),
                HttpRequestMetadata.from(servletRequest));
        return ResponseEntity.ok(LoginResponse.from(result));
    }

    @GetMapping("/me")
    public UserView me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return new UserView("", "", "ANONYMOUS", "Sin sesión", null, null);
        }
        if (!(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            return new UserView(
                    "", authentication.getName(), "API_CLIENT", "Integración API", null, null);
        }
        return UserView.from(user);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @Valid @RequestBody RefreshRequest request,
            HttpServletRequest servletRequest
    ) {
        try {
            AuthSessionService.RefreshedSession result = authSessions.refresh(
                    request.refreshToken(),
                    HttpRequestMetadata.from(servletRequest).ipAddress(),
                    HttpRequestMetadata.from(servletRequest).userAgent());
            return ResponseEntity.ok(LoginResponse.from(result));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED,
                    e.getMessage(),
                    e);
        }
    }

    @PostMapping("/logout")
    public void logout(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody(required = false) LogoutRequest request,
            Authentication authentication,
            HttpServletRequest servletRequest
    ) {
        audit.record(
                "auth.logout",
                "auth_session",
                null,
                null,
                HttpRequestMetadata.from(servletRequest));
        AuthenticatedUser user = authentication != null
                && authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser
                ? authenticatedUser
                : null;
        String accessToken = authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring(7)
                : null;
        logoutService.logout(
                user,
                accessToken,
                request == null ? null : request.refreshToken(),
                request == null ? null : request.deviceToken());
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record LogoutRequest(
            @Size(max = 4096) String refreshToken,
            @Size(max = 4096) String deviceToken
    ) {
    }

    public record LoginResponse(
            String token,
            Instant expiresAt,
            String refreshToken,
            Instant refreshExpiresAt,
            UserView user
    ) {
        static LoginResponse from(AuthService.LoginResult result) {
            return new LoginResponse(
                    result.token(),
                    result.expiresAt(),
                    result.refreshToken(),
                    result.refreshExpiresAt(),
                    UserView.from(result.user()));
        }

        static LoginResponse from(AuthSessionService.RefreshedSession result) {
            return new LoginResponse(
                    result.token(),
                    result.expiresAt(),
                    result.refreshToken(),
                    result.refreshExpiresAt(),
                    UserView.from(result.user()));
        }
    }

    public record UserView(
            String id,
            String username,
            String role,
            String displayName,
            String email,
            String whatsappPhone
    ) {
        static UserView from(AuthenticatedUser user) {
            return new UserView(
                    user.id().toString(),
                    user.username(),
                    user.role().name(),
                    user.displayName(),
                    user.email(),
                    user.whatsappPhone());
        }
    }
}
