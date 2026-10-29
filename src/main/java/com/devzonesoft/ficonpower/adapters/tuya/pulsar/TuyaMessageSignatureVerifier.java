package com.devzonesoft.ficonpower.adapters.tuya.pulsar;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
public class TuyaMessageSignatureVerifier {

    public boolean verify(JsonNode envelope, String accessKey) {
        JsonNode signatureNode = envelope.get("sign");
        if (signatureNode == null || signatureNode.asString().isBlank()) {
            return false;
        }

        String canonicalParameters = envelope.properties().stream()
                .filter(entry -> !"sign".equals(entry.getKey()))
                // Tuya sends encryptVersion=v2 as transport metadata but does not sign it.
                .filter(entry -> !"encryptVersion".equals(entry.getKey()))
                .filter(entry -> hasValue(entry.getValue()))
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(entry -> entry.getKey() + "=" + canonicalValue(entry.getValue()))
                .collect(Collectors.joining("||"));

        String material = canonicalParameters + "||" + accessKey;
        byte[] expected = md5(material);
        try {
            byte[] supplied = HexFormat.of().parseHex(signatureNode.asString());
            return MessageDigest.isEqual(expected, supplied);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private boolean hasValue(JsonNode value) {
        if (value == null || value.isNull()) {
            return false;
        }
        if (value.isString()) {
            return !value.asString().isBlank();
        }
        if (value.isArray() || value.isObject()) {
            return !value.isEmpty();
        }
        return true;
    }

    private String canonicalValue(JsonNode value) {
        return value.isValueNode() ? value.asString() : value.toString();
    }

    private byte[] md5(String value) {
        try {
            return MessageDigest.getInstance("MD5").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("MD5 is not available", exception);
        }
    }
}
