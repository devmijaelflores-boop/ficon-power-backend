package com.devzonesoft.ficonpower.ports.output;

import com.devzonesoft.ficonpower.domain.MonitoringViews.DeviceView;
import com.devzonesoft.ficonpower.domain.MonitoringViews.DeviceDetailView;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceQueryRepository {

    List<DeviceView> findDevices();

    Optional<DeviceDetailView> findDeviceDetail(UUID id);
}
