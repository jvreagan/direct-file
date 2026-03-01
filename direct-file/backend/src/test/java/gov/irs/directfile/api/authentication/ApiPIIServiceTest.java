package gov.irs.directfile.api.authentication;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import gov.irs.directfile.models.encryption.EncryptionService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiPIIServiceTest {
    private ApiPIIService piiService;

    @Mock
    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        piiService = new ApiPIIService(encryptionService);
    }

    @Nested
    class StorePIITests {
        @Test
        void givenValidValue_whenStorePII_thenEncryptsAndStores() {
            UUID userId = UUID.randomUUID();
            when(encryptionService.encrypt(any(byte[].class), anyMap()))
                    .thenReturn("encrypted".getBytes(StandardCharsets.UTF_8));

            piiService.storePII(userId, PIIAttribute.GIVENNAME, "John");

            verify(encryptionService).encrypt(eq("John".getBytes(StandardCharsets.UTF_8)), anyMap());
        }

        @Test
        void givenStoredPII_whenFetchAttributes_thenReturnsStoredValues() {
            UUID userId = UUID.randomUUID();
            when(encryptionService.encrypt(any(byte[].class), anyMap()))
                    .thenReturn("encrypted".getBytes(StandardCharsets.UTF_8));

            piiService.storePII(userId, PIIAttribute.GIVENNAME, "John");
            piiService.storePII(userId, PIIAttribute.SURNAME, "Doe");

            Map<PIIAttribute, String> result =
                    piiService.fetchAttributes(userId, Set.of(PIIAttribute.GIVENNAME, PIIAttribute.SURNAME));

            assertEquals("John", result.get(PIIAttribute.GIVENNAME));
            assertEquals("Doe", result.get(PIIAttribute.SURNAME));
        }

        @Test
        void givenNoStoredPII_whenFetchAttributes_thenReturnsEmptyMap() {
            UUID userId = UUID.randomUUID();

            Map<PIIAttribute, String> result = piiService.fetchAttributes(userId, Set.of(PIIAttribute.GIVENNAME));

            assertTrue(result.isEmpty());
        }

        @Test
        void givenMapOfAttributes_whenStorePIIBulk_thenStoresAll() {
            UUID userId = UUID.randomUUID();
            when(encryptionService.encrypt(any(byte[].class), anyMap()))
                    .thenReturn("encrypted".getBytes(StandardCharsets.UTF_8));

            Map<PIIAttribute, String> attrs = Map.of(
                    PIIAttribute.GIVENNAME, "Jane",
                    PIIAttribute.SURNAME, "Smith");
            piiService.storePII(userId, attrs);

            verify(encryptionService, times(2)).encrypt(any(byte[].class), anyMap());
            Map<PIIAttribute, String> result =
                    piiService.fetchAttributes(userId, Set.of(PIIAttribute.GIVENNAME, PIIAttribute.SURNAME));
            assertEquals(2, result.size());
        }
    }

    @Nested
    class ValidateTinTests {
        @Test
        void givenValidSSN_whenValidateTin_thenNoException() {
            assertDoesNotThrow(() -> ApiPIIService.validateTin("123456789"));
        }

        @Test
        void givenValidSSNWithDashes_whenValidateTin_thenNoException() {
            assertDoesNotThrow(() -> ApiPIIService.validateTin("123-45-6789"));
        }

        @Test
        void givenValidITIN_whenValidateTin_thenNoException() {
            assertDoesNotThrow(() -> ApiPIIService.validateTin("912345678"));
        }

        @Test
        void givenInvalidTin_whenValidateTin_thenThrows() {
            assertThrows(ApiPIIService.PIIValidationException.class, () -> ApiPIIService.validateTin("12345"));
        }

        @Test
        void givenNullTin_whenValidateTin_thenThrows() {
            assertThrows(ApiPIIService.PIIValidationException.class, () -> ApiPIIService.validateTin(null));
        }

        @Test
        void givenBlankTin_whenValidateTin_thenThrows() {
            assertThrows(ApiPIIService.PIIValidationException.class, () -> ApiPIIService.validateTin("   "));
        }
    }

    @Nested
    class ValidateEinTests {
        @Test
        void givenValidEin_whenValidateEin_thenNoException() {
            assertDoesNotThrow(() -> ApiPIIService.validateEin("123456789"));
        }

        @Test
        void givenInvalidEin_whenValidateEin_thenThrows() {
            assertThrows(ApiPIIService.PIIValidationException.class, () -> ApiPIIService.validateEin("12345"));
        }

        @Test
        void givenNullEin_whenValidateEin_thenThrows() {
            assertThrows(ApiPIIService.PIIValidationException.class, () -> ApiPIIService.validateEin(null));
        }
    }

    @Nested
    class MaskTinTests {
        @Test
        void givenFullSSN_whenMaskTin_thenMasksCorrectly() {
            assertEquals("***-**-6789", ApiPIIService.maskTin("123456789"));
        }

        @Test
        void givenNull_whenMaskTin_thenReturnsMaskPlaceholder() {
            assertEquals("***", ApiPIIService.maskTin(null));
        }

        @Test
        void givenShortValue_whenMaskTin_thenReturnsMaskPlaceholder() {
            assertEquals("***", ApiPIIService.maskTin("12"));
        }
    }

    @Nested
    class MaskEmailTests {
        @Test
        void givenNormalEmail_whenMaskEmail_thenMasksCorrectly() {
            assertEquals("j***n@example.com", ApiPIIService.maskEmail("john@example.com"));
        }

        @Test
        void givenShortLocalPart_whenMaskEmail_thenMasksCorrectly() {
            assertEquals("**@example.com", ApiPIIService.maskEmail("ab@example.com"));
        }

        @Test
        void givenNull_whenMaskEmail_thenReturnsMaskPlaceholder() {
            assertEquals("***", ApiPIIService.maskEmail(null));
        }

        @Test
        void givenNoAtSign_whenMaskEmail_thenReturnsMaskPlaceholder() {
            assertEquals("***", ApiPIIService.maskEmail("notanemail"));
        }
    }
}
