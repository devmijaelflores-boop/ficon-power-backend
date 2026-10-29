package com.devzonesoft.ficonpower.usecase;

import com.devzonesoft.ficonpower.domain.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class LogoutService {

    private final AuthSessionService authSessions;
    private final MobileDeviceTokenService mobileTokens;

    public LogoutService(
            AuthSessionService authSessions,
            MobileDeviceTokenService mobileTokens
    ) {
        this.authSessions = authSessions;
        this.mobileTokens = mobileTokens;
    }

    @Transactional
    public void logout(
            AuthenticatedUser user,
            String accessToken,
            String refreshToken,
            String deviceToken
    ) {
        if (user != null && StringUtils.hasText(deviceToken)) {
            mobileTokens.revoke(user, deviceToken);
        }
        if (StringUtils.hasText(accessToken)) {
            authSessions.revoke(accessToken);
        }
        if (StringUtils.hasText(refreshToken)) {
            authSessions.revokeRefreshToken(refreshToken);
        }
    }
}
