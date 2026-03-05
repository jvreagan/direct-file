package gov.irs.directfile.models.encryption;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NoOpEncryptionServiceTest {

    private NoOpEncryptionService noOpEncryptionService;

    @BeforeEach
    void setUp() {
        noOpEncryptionService = new NoOpEncryptionService();
    }

    @Test
    void givenPlaintext_whenEncrypt_thenReturnsSameBytes() {
        byte[] plaintext = "sensitive data".getBytes(StandardCharsets.UTF_8);

        byte[] result = noOpEncryptionService.encrypt(plaintext, Map.of("key", "value"));

        assertThat(result).isSameAs(plaintext);
    }

    @Test
    void givenCiphertext_whenDecrypt_thenReturnsSameBytes() {
        byte[] ciphertext = "not actually encrypted".getBytes(StandardCharsets.UTF_8);

        byte[] result = noOpEncryptionService.decrypt(ciphertext);

        assertThat(result).isSameAs(ciphertext);
    }
}
