package com.devzonesoft.ficonpower.ports.input;

import com.devzonesoft.ficonpower.domain.Device;
import java.util.UUID;

public interface UpdateDeviceDetails {

    Device update(UUID id, String name, UUID siteId);
}
