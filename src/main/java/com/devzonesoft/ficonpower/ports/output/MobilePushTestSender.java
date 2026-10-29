package com.devzonesoft.ficonpower.ports.output;

import com.devzonesoft.ficonpower.domain.MobilePushTestResult;
import java.util.UUID;

public interface MobilePushTestSender {

    MobilePushTestResult sendTest(UUID userId, String token);
}
