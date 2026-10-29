package com.devzonesoft.ficonpower.adapters.tuya.pulsar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

class TuyaMessageDecoderTest {

    private static final String ACCESS_KEY = "12345678abcdefghijklmnop12345678";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TuyaMessageSignatureVerifier signatureVerifier = new TuyaMessageSignatureVerifier();
    private final TuyaMessageDecoder decoder = new TuyaMessageDecoder(
            objectMapper, signatureVerifier, new TuyaPayloadDecryptor());

    @Test
    void verifiesAndDecryptsAnEcbMessage() throws Exception {
        String clearText = """
                {"bizCode":"deviceOffline","bizData":{"devId":"tuya-device-1"}}
                """.trim();
        String encrypted = encryptEcb(clearText);
        ObjectNode envelope = signedEnvelope(encrypted);

        JsonNode decoded = decoder.decode(
                objectMapper.writeValueAsBytes(envelope), ACCESS_KEY, TuyaPayloadDecryptor.AES_ECB);

        assertThat(decoded.path("bizCode").asString()).isEqualTo("deviceOffline");
        assertThat(decoded.path("bizData").path("devId").asString()).isEqualTo("tuya-device-1");
    }

    @Test
    void rejectsMessagesWithAnInvalidSignature() throws Exception {
        ObjectNode envelope = signedEnvelope(encryptEcb("{}"));
        envelope.put("sign", "00000000000000000000000000000000");

        assertThatThrownBy(() -> decoder.decode(
                objectMapper.writeValueAsBytes(envelope), ACCESS_KEY, TuyaPayloadDecryptor.AES_ECB))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void verifiesAndDecryptsAGcmMessage() throws Exception {
        String clearText = "{\"bizCode\":\"offline\",\"devId\":\"gcm-device\"}";
        ObjectNode envelope = signedEnvelope(encryptGcm(clearText));

        JsonNode decoded = decoder.decode(
                objectMapper.writeValueAsBytes(envelope), ACCESS_KEY, TuyaPayloadDecryptor.AES_GCM);

        assertThat(decoded.path("devId").asString()).isEqualTo("gcm-device");
    }

    private ObjectNode signedEnvelope(String encrypted) throws Exception {
        ObjectNode envelope = objectMapper.createObjectNode();
        envelope.put("protocol", 1001);
        envelope.put("pv", "2.0");
        envelope.put("encryptVersion", "v2");
        envelope.put("t", 1721653200000L);
        envelope.put("data", encrypted);

        String material = "data=%s||protocol=1001||pv=2.0||t=1721653200000||%s"
                .formatted(encrypted, ACCESS_KEY);
        String signature = HexFormat.of().formatHex(
                MessageDigest.getInstance("MD5").digest(material.getBytes(StandardCharsets.UTF_8)));
        envelope.put("sign", signature);
        return envelope;
    }

    private String encryptEcb(String clearText) throws Exception {
        byte[] key = ACCESS_KEY.substring(8, 24).getBytes(StandardCharsets.UTF_8);
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"));
        return Base64.getEncoder().encodeToString(
                cipher.doFinal(clearText.getBytes(StandardCharsets.UTF_8)));
    }

    private String encryptGcm(String clearText) throws Exception {
        byte[] key = ACCESS_KEY.substring(8, 24).getBytes(StandardCharsets.UTF_8);
        byte[] nonce = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"),
                new GCMParameterSpec(128, nonce));
        byte[] encrypted = cipher.doFinal(clearText.getBytes(StandardCharsets.UTF_8));
        byte[] message = new byte[nonce.length + encrypted.length];
        System.arraycopy(nonce, 0, message, 0, nonce.length);
        System.arraycopy(encrypted, 0, message, nonce.length, encrypted.length);
        return Base64.getEncoder().encodeToString(message);
    }
}
