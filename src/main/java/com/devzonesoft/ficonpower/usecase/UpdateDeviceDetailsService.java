package com.devzonesoft.ficonpower.usecase;

import com.devzonesoft.ficonpower.domain.Device;
import com.devzonesoft.ficonpower.domain.DeviceNotFoundException;
import com.devzonesoft.ficonpower.ports.input.UpdateDeviceDetails;
import com.devzonesoft.ficonpower.ports.output.DeviceRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class UpdateDeviceDetailsService implements UpdateDeviceDetails {

    static final int MAX_NAME_LENGTH = 150;

    private final DeviceRepository deviceRepository;

    public UpdateDeviceDetailsService(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    @Override
    public Device update(UUID id, String name, UUID siteId) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede estar vacio");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "El nombre no puede superar los " + MAX_NAME_LENGTH + " caracteres");
        }
        if (siteId == null) {
            throw new IllegalArgumentException("La sede es obligatoria");
        }
        return deviceRepository.updateDetails(id, name.trim(), siteId)
                .orElseThrow(() -> new DeviceNotFoundException(id.toString()));
    }
}
