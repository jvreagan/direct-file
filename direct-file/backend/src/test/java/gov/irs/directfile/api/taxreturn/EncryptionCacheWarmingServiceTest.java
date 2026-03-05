package gov.irs.directfile.api.taxreturn;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import com.amazonaws.encryptionsdk.AwsCrypto;
import com.amazonaws.encryptionsdk.CryptoMaterialsManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EncryptionCacheWarmingServiceTest {

    @Mock
    private CryptoMaterialsManager cryptoMaterialsManager;

    @Mock
    private AwsCrypto awsCrypto;

    @Captor
    private ArgumentCaptor<Map<String, String>> contextCaptor;

    private EncryptionCacheWarmingService encryptionCacheWarmingService;

    private static final byte[] WARMING_DATA = "data-for-cache-warming".getBytes(StandardCharsets.UTF_8);

    @BeforeEach
    void setUp() {
        encryptionCacheWarmingService = new EncryptionCacheWarmingService(cryptoMaterialsManager, awsCrypto);
    }

    @Test
    void givenValidUserId_whenWarmCache_thenEncryptDataCalled() {
        UUID userId = UUID.randomUUID();

        encryptionCacheWarmingService.warmCacheForUserExternalId(userId);

        verify(awsCrypto).encryptData(eq(cryptoMaterialsManager), eq(WARMING_DATA), any(Map.class));
    }

    @Test
    void givenUserId_whenWarmCache_thenContextContainsId() {
        UUID userId = UUID.randomUUID();

        encryptionCacheWarmingService.warmCacheForUserExternalId(userId);

        verify(awsCrypto).encryptData(eq(cryptoMaterialsManager), eq(WARMING_DATA), contextCaptor.capture());
        Map<String, String> capturedContext = contextCaptor.getValue();
        assertThat(capturedContext).containsEntry("id", userId.toString());
        assertThat(capturedContext).hasSize(1);
    }

    @Test
    void givenEncryptionFailure_whenWarmCache_thenExceptionPropagates() {
        UUID userId = UUID.randomUUID();
        when(awsCrypto.encryptData(any(CryptoMaterialsManager.class), any(byte[].class), any(Map.class)))
                .thenThrow(new RuntimeException("KMS failure"));

        assertThatThrownBy(() -> encryptionCacheWarmingService.warmCacheForUserExternalId(userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("KMS failure");
    }
}
