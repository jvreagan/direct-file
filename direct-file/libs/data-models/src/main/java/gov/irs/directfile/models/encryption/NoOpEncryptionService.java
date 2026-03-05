package gov.irs.directfile.models.encryption;

import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NoOpEncryptionService implements EncryptionService {
    @Override
    public byte[] encrypt(byte[] plaintext, Map<String, String> context) {
        log.debug("NoOp: encrypt called (returning plaintext)");
        return plaintext;
    }

    @Override
    public byte[] decrypt(byte[] ciphertext) {
        log.debug("NoOp: decrypt called (returning ciphertext as-is)");
        return ciphertext;
    }
}
