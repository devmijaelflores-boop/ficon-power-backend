package com.devzonesoft.ficonpower.adapters.tuya.cloud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class TuyaCloudDeviceMapperTest {

    private final ObjectMapper json = new ObjectMapper();
    private final TuyaCloudDeviceMapper mapper = new TuyaCloudDeviceMapper();

    @Test
    void mapsCamelCaseFieldsReturnedByTuyaCloud() throws Exception {
        var response = json.readTree("""
                {
                  "result": [
                    {
                      "id": "6596da38f03423bf9092af",
                      "customName": "WIFI Energy Meter Test",
                      "isOnline": true,
                      "category": "dlq",
                      "productId": "zblabrkwxcxezteq",
                      "model": "RPE1"
                    }
                  ]
                }
                """);

        List<TuyaCloudDevice> devices = mapper.mapDevices(response);

        assertEquals(1, devices.size());
        assertTrue(devices.get(0).online());
        assertEquals("WIFI Energy Meter Test", devices.get(0).name());
        assertEquals("zblabrkwxcxezteq", devices.get(0).productId());
    }

    @Test
    void supportsNestedDeviceListsAndSkipsEntriesWithoutId() throws Exception {
        var response = json.readTree("""
                {
                  "result": {
                    "devices": [
                      {"name": "Sin id"},
                      {"device_id": "device-2", "device_name": "Medidor 2"}
                    ]
                  }
                }
                """);

        List<TuyaCloudDevice> devices = mapper.mapDevices(response);

        assertEquals(1, devices.size());
        assertEquals("device-2", devices.get(0).deviceId());
    }
}
