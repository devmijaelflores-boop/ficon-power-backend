package com.devzonesoft.ficonpower.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.devzonesoft.ficonpower.domain.Device;
import com.devzonesoft.ficonpower.domain.DeviceNotFoundException;
import com.devzonesoft.ficonpower.domain.DeviceStatus;
import com.devzonesoft.ficonpower.ports.output.DeviceRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UpdateDeviceDetailsServiceTest {

    private static final UUID DEVICE_ID = UUID.randomUUID();
    private static final UUID SITE_ID = UUID.randomUUID();

    private final DeviceRepository deviceRepository = new DeviceRepository() {
        @Override
        public Optional<Device> findByDeviceId(String deviceId) {
            return Optional.empty();
        }

        @Override
        public Optional<Device> findByDeviceIdForUpdate(String deviceId) {
            return Optional.empty();
        }

        @Override
        public Device upsert(
                String deviceId,
                String name,
                DeviceStatus status,
                String category,
                String productId,
                String model
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateStatus(UUID id, DeviceStatus status, Instant occurredAt) {
        }

        @Override
        public boolean deactivateByDeviceId(String deviceId) {
            return false;
        }

        @Override
        public Optional<Device> updateDetails(UUID id, String name, UUID siteId) {
            if (!id.equals(DEVICE_ID)) {
                return Optional.empty();
            }
            return Optional.of(new Device(
                    id, "tuya-123", name, siteId, "Lampa", "tuya", null, null, null,
                    true, DeviceStatus.ONLINE, Instant.now(), Instant.now(), Instant.now()));
        }
    };

    private final UpdateDeviceDetailsService service =
            new UpdateDeviceDetailsService(deviceRepository);

    @Test
    void updatesNameAndSite() {
        Device updated = service.update(DEVICE_ID, "Cuchilla Lampa", SITE_ID);

        assertEquals("Cuchilla Lampa", updated.name());
        assertEquals(SITE_ID, updated.siteId());
    }

    @Test
    void rejectsBlankName() {
        assertThrows(IllegalArgumentException.class,
                () -> service.update(DEVICE_ID, "  ", SITE_ID));
    }

    @Test
    void rejectsTooLongName() {
        String longName = "x".repeat(UpdateDeviceDetailsService.MAX_NAME_LENGTH + 1);

        assertThrows(IllegalArgumentException.class,
                () -> service.update(DEVICE_ID, longName, SITE_ID));
    }

    @Test
    void failsWhenDeviceDoesNotExist() {
        assertThrows(DeviceNotFoundException.class,
                () -> service.update(UUID.randomUUID(), "Nombre", SITE_ID));
    }
}
