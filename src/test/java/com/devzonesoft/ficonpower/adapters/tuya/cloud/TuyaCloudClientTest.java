package com.devzonesoft.ficonpower.adapters.tuya.cloud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.devzonesoft.ficonpower.config.TuyaCloudProperties;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class TuyaCloudClientTest {

    @Test
    void returnsNoDevicesWhenCloudSynchronizationIsDisabled() {
        TuyaCloudProperties properties = new TuyaCloudProperties();
        TuyaCloudTransport transport = mock(TuyaCloudTransport.class);
        TuyaAccessTokenProvider tokens = mock(TuyaAccessTokenProvider.class);
        TuyaCloudDeviceMapper mapper = mock(TuyaCloudDeviceMapper.class);
        TuyaCloudClient client = new TuyaCloudClient(
                properties, transport, tokens, mapper);

        assertEquals(List.of(), client.listDevices());
        verifyNoInteractions(transport, tokens, mapper);
    }

    @Test
    void delegatesEnabledDeviceListingToTransportAndMapper() {
        TuyaCloudProperties properties = new TuyaCloudProperties();
        properties.setEnabled(true);
        TuyaCloudTransport transport = mock(TuyaCloudTransport.class);
        TuyaAccessTokenProvider tokens = mock(TuyaAccessTokenProvider.class);
        TuyaCloudDeviceMapper mapper = mock(TuyaCloudDeviceMapper.class);
        JsonNode response = mock(JsonNode.class);
        List<TuyaCloudDevice> expected = List.of(
                new TuyaCloudDevice("device-1", "Medidor", true, null, null, null));
        when(tokens.accessToken()).thenReturn("access-token");
        when(transport.get(
                "/v2.0/cloud/thing/device?page_size=20",
                "access-token",
                "device list")).thenReturn(response);
        when(mapper.mapDevices(response)).thenReturn(expected);
        TuyaCloudClient client = new TuyaCloudClient(
                properties, transport, tokens, mapper);

        assertEquals(expected, client.listDevices());

        verify(tokens).accessToken();
        verify(mapper).mapDevices(response);
    }

    @Test
    void resolvesTheNameOfAnIndustrialAsset() {
        TuyaCloudProperties properties = new TuyaCloudProperties();
        properties.setEnabled(true);
        TuyaCloudTransport transport = mock(TuyaCloudTransport.class);
        TuyaAccessTokenProvider tokens = mock(TuyaAccessTokenProvider.class);
        when(tokens.accessToken()).thenReturn("access-token");
        when(transport.get(
                "/v1.0/iot-03/assets/asset-1",
                "access-token",
                "asset details")).thenReturn(new ObjectMapper().readTree("""
                        {"success":true,"result":{"asset_name":"TIMAX_puno"}}
                        """));
        TuyaCloudClient client = new TuyaCloudClient(
                properties, transport, tokens, mock(TuyaCloudDeviceMapper.class));

        assertEquals(Optional.of("TIMAX_puno"), client.assetName("asset-1"));
    }

    @Test
    void returnsEmptyWhenTheSpaceResponseHasNoName() {
        TuyaCloudProperties properties = new TuyaCloudProperties();
        properties.setEnabled(true);
        TuyaCloudTransport transport = mock(TuyaCloudTransport.class);
        TuyaAccessTokenProvider tokens = mock(TuyaAccessTokenProvider.class);
        when(tokens.accessToken()).thenReturn("access-token");
        when(transport.get(
                "/v2.0/cloud/space/space-1",
                "access-token",
                "space details")).thenReturn(new ObjectMapper().readTree("""
                        {"success":true,"result":{}}
                        """));
        TuyaCloudClient client = new TuyaCloudClient(
                properties, transport, tokens, mock(TuyaCloudDeviceMapper.class));

        assertTrue(client.spaceName("space-1").isEmpty());
    }
}
