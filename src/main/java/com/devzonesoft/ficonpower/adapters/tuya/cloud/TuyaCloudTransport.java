package com.devzonesoft.ficonpower.adapters.tuya.cloud;

import com.devzonesoft.ficonpower.config.TuyaCloudProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

@Component
public class TuyaCloudTransport {

    private final RestClient restClient;
    private final TuyaRequestSigner signer;

    public TuyaCloudTransport(
            TuyaCloudProperties properties,
            RestClient.Builder restClientBuilder,
            TuyaRequestSigner signer
    ) {
        this.restClient = restClientBuilder.baseUrl(properties.getApiUrl()).build();
        this.signer = signer;
    }

    public JsonNode get(String path, String accessToken, String operation) {
        JsonNode response = restClient.get()
                .uri(path)
                .headers(headers -> headers.addAll(signer.headersForGet(path, accessToken)))
                .retrieve()
                .body(JsonNode.class);
        ensureSuccess(response, operation);
        return response;
    }

    private void ensureSuccess(JsonNode response, String operation) {
        if (response == null || !response.path("success").asBoolean(false)) {
            String code = response == null ? "no-response" : response.path("code").asString();
            String message = response == null ? "empty response" : response.path("msg").asString();
            throw new IllegalStateException("Tuya Cloud %s failed: %s %s"
                    .formatted(operation, code, message));
        }
    }
}
