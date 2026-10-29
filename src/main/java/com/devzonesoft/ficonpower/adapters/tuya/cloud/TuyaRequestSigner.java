package com.devzonesoft.ficonpower.adapters.tuya.cloud;

import com.devzonesoft.ficonpower.config.TuyaCloudProperties;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
public class TuyaRequestSigner {

    private static final String EMPTY_BODY_SHA256 =
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    private final TuyaCloudProperties properties;
    private final Clock clock;

    public TuyaRequestSigner(TuyaCloudProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public HttpHeaders headersForGet(String path, String accessToken) {
        long timestamp = Instant.now(clock).toEpochMilli();
        String stringToSign = "GET\n%s\n\n%s".formatted(EMPTY_BODY_SHA256, path);
        String material = properties.getAccessId() + accessToken + timestamp + stringToSign;
        HttpHeaders headers = new HttpHeaders();
        headers.set("client_id", properties.getAccessId());
        headers.set("t", Long.toString(timestamp));
        headers.set("sign_method", "HMAC-SHA256");
        headers.set("sign", hmacSha256(material, properties.getAccessKey()));
        if (!accessToken.isBlank()) {
            headers.set("access_token", accessToken);
        }
        return headers;
    }

    private String hmacSha256(String value, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().withUpperCase().formatHex(
                    mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to sign Tuya Cloud request", exception);
        }
    }
}
