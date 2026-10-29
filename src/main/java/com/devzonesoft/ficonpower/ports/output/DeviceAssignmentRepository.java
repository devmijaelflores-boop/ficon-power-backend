package com.devzonesoft.ficonpower.ports.output;

import com.devzonesoft.ficonpower.domain.DevicePlacement;

public interface DeviceAssignmentRepository {

    void assign(String externalDeviceId, DevicePlacement placement, String tuyaContainerId);

    boolean closeActive(String externalDeviceId, String tuyaContainerId);
}
