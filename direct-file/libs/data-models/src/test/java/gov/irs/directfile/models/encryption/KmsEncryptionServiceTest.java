package gov.irs.directfile.models.encryption;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KmsEncryptionServiceTest {

    @Mock
    private DataEncryptDecrypt dataEncryptDecrypt;

    @InjectMocks
    private KmsEncryptionService kmsEncryptionService;

    @Test
    void givenPlaintext_whenEncrypt_thenDelegates() {
        // given
        byte[] plaintext = "sensitive data".getBytes(StandardCharsets.UTF_8);
        Map<String, String> context = Map.of("purpose", "tax-return");
        byte[] expectedCiphertext = "encrypted-bytes".getBytes(StandardCharsets.UTF_8);
        when(dataEncryptDecrypt.encrypt(plaintext, context)).thenReturn(expectedCiphertext);

        // when
        byte[] result = kmsEncryptionService.encrypt(plaintext, context);

        // then
        assertThat(result).isEqualTo(expectedCiphertext);
        verify(dataEncryptDecrypt).encrypt(plaintext, context);
    }

    @Test
    void givenCiphertext_whenDecrypt_thenDelegates() {
        // given
        byte[] ciphertext = "encrypted-bytes".getBytes(StandardCharsets.UTF_8);
        byte[] expectedPlaintext = "sensitive data".getBytes(StandardCharsets.UTF_8);
        when(dataEncryptDecrypt.decrypt(ciphertext)).thenReturn(expectedPlaintext);

        // when
        byte[] result = kmsEncryptionService.decrypt(ciphertext);

        // then
        assertThat(result).isEqualTo(expectedPlaintext);
        verify(dataEncryptDecrypt).decrypt(ciphertext);
    }

    @Test
    void givenNullPlaintext_whenEncrypt_thenDelegates() {
        // given
        Map<String, String> context = Map.of("purpose", "test");
        when(dataEncryptDecrypt.encrypt(null, context)).thenReturn(null);

        // when
        byte[] result = kmsEncryptionService.encrypt(null, context);

        // then
        assertThat(result).isNull();
        verify(dataEncryptDecrypt).encrypt(null, context);
    }
}
