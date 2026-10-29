package com.devzonesoft.ficonpower.adapters.tuya.pulsar;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.apache.pulsar.client.api.AuthenticationDataProvider;

final class TuyaPulsarAuthenticationData implements AuthenticationDataProvider {

    private final String commandData;

    TuyaPulsarAuthenticationData(String accessId, String accessKey) {
        String password = md5(accessId + md5(accessKey)).substring(8, 24);
        this.commandData = "{\"username\":\"%s\",\"password\":\"%s\"}"
                .formatted(accessId, password);
    }

    @Override
    public String getCommandData() {
        return commandData;
    }

    @Override
    public boolean hasDataForHttp() {
        return false;
    }

    @Override
    public boolean hasDataFromCommand() {
        return true;
    }

    private static String md5(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("MD5 is not available", exception);
        }
    }
}
