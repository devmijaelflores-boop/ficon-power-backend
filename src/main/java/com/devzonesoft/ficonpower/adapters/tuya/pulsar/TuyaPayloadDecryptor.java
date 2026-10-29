package com.devzonesoft.ficonpower.adapters.tuya.pulsar;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class TuyaPayloadDecryptor {

    static final String AES_ECB = "aes_ecb";
    static final String AES_GCM = "aes_gcm";
    private static final int GCM_NONCE_LENGTH = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    public String decrypt(String encryptedData, String accessKey, String encryptionMode) {
        if (accessKey == null || accessKey.length() < 24) {
            throw new IllegalArgumentException("TUYA_ACCESS_KEY must contain at least 24 characters");
        }

        byte[] key = accessKey.substring(8, 24).getBytes(StandardCharsets.UTF_8);
        byte[] message = Base64.getDecoder().decode(encryptedData);

        try {
            byte[] clearText = AES_GCM.equalsIgnoreCase(encryptionMode)
                    ? decryptGcm(message, key)
                    : decryptEcb(message, key);
            return new String(clearText, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException exception) {
            throw new IllegalArgumentException("Unable to decrypt Tuya Pulsar payload", exception);
        }
    }

    private byte[] decryptEcb(byte[] message, byte[] key) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"));
        return cipher.doFinal(message);
    }

    private byte[] decryptGcm(byte[] message, byte[] key) throws GeneralSecurityException {
        if (message.length <= GCM_NONCE_LENGTH + GCM_TAG_LENGTH_BITS / 8) {
            throw new IllegalArgumentException("Invalid AES-GCM payload");
        }

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec parameters = new GCMParameterSpec(
                GCM_TAG_LENGTH_BITS, message, 0, GCM_NONCE_LENGTH);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), parameters);
        return cipher.doFinal(message, GCM_NONCE_LENGTH, message.length - GCM_NONCE_LENGTH);
    }
}
