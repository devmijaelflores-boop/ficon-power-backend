package com.devzonesoft.ficonpower.adapters.tuya.pulsar;

import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class TuyaMessageDecoder {

    private final ObjectMapper objectMapper;
    private final TuyaMessageSignatureVerifier signatureVerifier;
    private final TuyaPayloadDecryptor decryptor;

    public TuyaMessageDecoder(
            ObjectMapper objectMapper,
            TuyaMessageSignatureVerifier signatureVerifier,
            TuyaPayloadDecryptor decryptor
    ) {
        this.objectMapper = objectMapper;
        this.signatureVerifier = signatureVerifier;
        this.decryptor = decryptor;
    }

    public JsonNode decode(byte[] payload, String accessKey, String encryptionMode) {
        JsonNode envelope = objectMapper.readTree(new String(payload, StandardCharsets.UTF_8));
        if (!signatureVerifier.verify(envelope, accessKey)) {
            throw new SecurityException("Invalid Tuya Pulsar message signature");
        }

        JsonNode data = envelope.get("data");
        if (data == null || data.asString().isBlank()) {
            throw new IllegalArgumentException("Tuya Pulsar message does not contain encrypted data");
        }

        String decrypted = decryptor.decrypt(data.asString(), accessKey, encryptionMode);
        return objectMapper.readTree(decrypted);
    }
}
