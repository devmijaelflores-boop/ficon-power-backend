package com.devzonesoft.ficonpower.ports.output;

import com.devzonesoft.ficonpower.domain.MobileDeviceToken;
import java.util.List;
import java.util.UUID;

public interface MobileDeviceTokenRepository {

    MobileDeviceToken upsert(
            UUID userId,
            String token,
            String platform,
            String deviceName,
            String appVersion
    );

    void revoke(UUID userId, String token);

    List<MobileDeviceToken> findActive();
}
