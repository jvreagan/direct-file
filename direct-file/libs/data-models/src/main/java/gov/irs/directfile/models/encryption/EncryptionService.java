package gov.irs.directfile.models.encryption;

import java.util.Map;

public interface EncryptionService {
    byte[] encrypt(byte[] plaintext, Map<String, String> context);

    default byte[] encrypt(byte[] plaintext) {
        return encrypt(plaintext, Map.of());
    }

    byte[] decrypt(byte[] ciphertext);
}
