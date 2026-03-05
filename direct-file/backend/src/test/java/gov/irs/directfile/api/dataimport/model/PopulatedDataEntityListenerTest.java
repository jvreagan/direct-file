package gov.irs.directfile.api.dataimport.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import gov.irs.directfile.models.encryption.DataEncryptDecrypt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PopulatedDataEntityListenerTest {

    @Mock
    private DataEncryptDecrypt dataEncryptDecrypt;

    private PopulatedDataEntityListener listener;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        listener = new PopulatedDataEntityListener();
        objectMapper = new ObjectMapper();
        listener.configure(dataEncryptDecrypt, objectMapper);
    }

    @Test
    void givenValidCipherText_whenDecryptColumn_thenSetsJsonData() throws Exception {
        // given
        String plainJson = "{\"key\":\"value\"}";
        byte[] plainBytes = plainJson.getBytes();
        String cipherText = java.util.Base64.getEncoder().encodeToString(plainBytes);

        PopulatedData populatedData = new PopulatedData();
        populatedData.setDataCipherText(cipherText);

        when(dataEncryptDecrypt.decrypt(any(byte[].class))).thenReturn(plainBytes);

        // when
        listener.decryptColumn(populatedData);

        // then
        JsonNode expectedNode = objectMapper.readTree(plainJson);
        assertThat(populatedData.getData()).isNotNull();
        assertThat(populatedData.getData()).isEqualTo(expectedNode);
    }

    @Test
    void givenNullCipherText_whenDecryptColumn_thenHandlesGracefully() {
        // given
        PopulatedData populatedData = new PopulatedData();
        populatedData.setDataCipherText(null);

        // GenericStringEncryptor.convertToEntityAttribute returns null for null input
        // objectMapper.readTree(null) throws, which is caught by the catch block

        // when - should not throw
        listener.decryptColumn(populatedData);

        // then - data remains null since the exception is caught
        assertThat(populatedData.getData()).isNull();
    }

    @Test
    void givenInvalidJson_whenDecryptColumn_thenLogsError() {
        // given
        String invalidJson = "not valid json {{{";
        byte[] plainBytes = invalidJson.getBytes();
        String cipherText = java.util.Base64.getEncoder().encodeToString(plainBytes);

        PopulatedData populatedData = new PopulatedData();
        populatedData.setDataCipherText(cipherText);

        when(dataEncryptDecrypt.decrypt(any(byte[].class))).thenReturn(plainBytes);

        // when - should not throw; error is caught and logged
        listener.decryptColumn(populatedData);

        // then - data remains null because readTree throws on invalid JSON
        assertThat(populatedData.getData()).isNull();
    }
}
