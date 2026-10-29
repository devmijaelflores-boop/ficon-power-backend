package com.devzonesoft.ficonpower.web;

import com.devzonesoft.ficonpower.domain.AuthenticatedUser;
import com.devzonesoft.ficonpower.domain.MobileDeviceToken;
import com.devzonesoft.ficonpower.domain.MobilePushTestResult;
import com.devzonesoft.ficonpower.ports.output.MobilePushTestSender;
import com.devzonesoft.ficonpower.usecase.MobileDeviceTokenService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/mobile/device-tokens")
public class MobileDeviceTokenController {

    private final MobileDeviceTokenService tokenService;
    private final MobilePushTestSender pushTestSender;

    public MobileDeviceTokenController(
            MobileDeviceTokenService tokenService,
            MobilePushTestSender pushTestSender
    ) {
        this.tokenService = tokenService;
        this.pushTestSender = pushTestSender;
    }

    @PostMapping
    public MobileDeviceTokenView register(
            @Valid @RequestBody RegisterMobileTokenRequest request,
            Authentication authentication
    ) {
        MobileDeviceToken token = tokenService.register(
                authenticatedUser(authentication),
                request.token(),
                request.platform(),
                request.deviceName(),
                request.appVersion());
        return MobileDeviceTokenView.from(token);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(
            @Valid @RequestBody RevokeMobileTokenRequest request,
            Authentication authentication
    ) {
        tokenService.revoke(authenticatedUser(authentication), request.token());
    }

    @PostMapping("/test")
    public MobilePushTestResult test(
            @Valid @RequestBody RevokeMobileTokenRequest request,
            Authentication authentication
    ) {
        AuthenticatedUser user = authenticatedUser(authentication);
        return pushTestSender.sendTest(user.id(), request.token());
    }

    private AuthenticatedUser authenticatedUser(Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión requerida");
        }
        return user;
    }

    public record RegisterMobileTokenRequest(
            @NotBlank @Size(max = 4096) String token,
            @Size(max = 30) String platform,
            @Size(max = 120) String deviceName,
            @Size(max = 40) String appVersion
    ) {
    }

    public record RevokeMobileTokenRequest(@NotBlank @Size(max = 4096) String token) {
    }

    public record MobileDeviceTokenView(
            UUID id,
            UUID userId,
            String platform,
            String deviceName,
            String appVersion,
            Instant lastSeenAt
    ) {
        static MobileDeviceTokenView from(MobileDeviceToken token) {
            return new MobileDeviceTokenView(
                    token.id(),
                    token.userId(),
                    token.platform(),
                    token.deviceName(),
                    token.appVersion(),
                    token.lastSeenAt());
        }
    }
}
