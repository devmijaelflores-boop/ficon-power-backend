package com.devzonesoft.ficonpower.usecase;

import com.devzonesoft.ficonpower.domain.AuthenticatedUser;
import com.devzonesoft.ficonpower.domain.MobileDeviceToken;
import com.devzonesoft.ficonpower.ports.output.MobileDeviceTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MobileDeviceTokenService {

    private final MobileDeviceTokenRepository tokens;

    public MobileDeviceTokenService(MobileDeviceTokenRepository tokens) {
        this.tokens = tokens;
    }

    public MobileDeviceToken register(
            AuthenticatedUser user,
            String token,
            String platform,
            String deviceName,
            String appVersion
    ) {
        return tokens.upsert(
                user.id(),
                token,
                normalizePlatform(platform),
                trimToNull(deviceName),
                trimToNull(appVersion));
    }

    public void revoke(AuthenticatedUser user, String token) {
        tokens.revoke(user.id(), token);
    }

    private String normalizePlatform(String platform) {
        if (!StringUtils.hasText(platform)) {
            return "android";
        }
        return platform.trim().toLowerCase();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
