package com.devzonesoft.ficonpower.usecase;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.devzonesoft.ficonpower.domain.AuthenticatedUser;
import com.devzonesoft.ficonpower.domain.UserRole;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class LogoutServiceTest {

    private final AuthSessionService authSessions = mock(AuthSessionService.class);
    private final MobileDeviceTokenService mobileTokens = mock(MobileDeviceTokenService.class);
    private final LogoutService service = new LogoutService(authSessions, mobileTokens);

    @Test
    void revokesDeviceBeforeInvalidatingSessionCredentials() {
        AuthenticatedUser user = new AuthenticatedUser(
                UUID.randomUUID(), "technician", UserRole.USER, "Técnico");

        service.logout(user, "access-token", "refresh-token", "fcm-token");

        InOrder order = inOrder(mobileTokens, authSessions);
        order.verify(mobileTokens).revoke(user, "fcm-token");
        order.verify(authSessions).revoke("access-token");
        order.verify(authSessions).revokeRefreshToken("refresh-token");
    }

    @Test
    void acceptsLogoutWithoutMobileOrRefreshTokens() {
        service.logout(null, "access-token", null, null);

        org.mockito.Mockito.verify(authSessions).revoke("access-token");
        verifyNoInteractions(mobileTokens);
    }
}
