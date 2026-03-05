package gov.irs.directfile.models.encryption;

import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KmsEncryptionService implements EncryptionService {
    private final DataEncryptDecrypt dataEncryptDecrypt;

    public KmsEncryptionService(DataEncryptDecrypt dataEncryptDecrypt) {
        this.dataEncryptDecrypt = dataEncryptDecrypt;
    }

    @Override
    public byte[] encrypt(byte[] plaintext, Map<String, String> context) {
        return dataEncryptDecrypt.encrypt(plaintext, context);
    }

    @Override
    public byte[] decrypt(byte[] ciphertext) {
        return dataEncryptDecrypt.decrypt(ciphertext);
    }
}
