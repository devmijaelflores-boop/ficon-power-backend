package com.devzonesoft.ficonpower.usecase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devzonesoft.ficonpower.domain.ConnectivityNotice;
import com.devzonesoft.ficonpower.domain.DeviceStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class HandleDeviceConnectivityServiceTest {

    @Test
    void normalizesEventPrecisionAndDispatchesAProducedNotice() throws Exception {
        DeviceConnectivityTransaction transaction = mock(DeviceConnectivityTransaction.class);
        ConnectivityNotificationDispatcher notifications =
                mock(ConnectivityNotificationDispatcher.class);
        HandleDeviceConnectivityService service =
                new HandleDeviceConnectivityService(transaction, notifications);
        Instant received = Instant.parse("2026-07-22T12:00:00.123456Z");
        Instant normalized = Instant.parse("2026-07-22T12:00:00.123Z");
        var payload = new ObjectMapper().readTree("{}");
        ConnectivityNotice notice = new ConnectivityNotice.Outage(
                UUID.randomUUID(), "Medidor", normalized);
        when(transaction.apply("device-1", DeviceStatus.OFFLINE, normalized, payload))
                .thenReturn(Optional.of(notice));

        service.handle("device-1", DeviceStatus.OFFLINE, received, payload);

        verify(notifications).dispatch(notice);
    }
}
