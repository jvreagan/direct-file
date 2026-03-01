package gov.irs.directfile.models.storage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.encryption.s3.S3EncryptionClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3DocumentStorageServiceTest {
    private S3DocumentStorageService storageService;

    @Mock
    private S3EncryptionClient s3Client;

    private final String bucketName = "test-bucket";
    private final String environmentPrefix = "dev/";

    @BeforeEach
    void setUp() {
        storageService = new S3DocumentStorageService(s3Client, bucketName, environmentPrefix);
    }

    @Test
    void givenKey_whenStore_thenPrependsEnvironmentPrefix() throws IOException {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        storageService.store("test.txt", new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8)));

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
        assertEquals("dev/test.txt", captor.getValue().key());
        assertEquals(bucketName, captor.getValue().bucket());
    }

    @Test
    void givenMetadata_whenStore_thenMetadataIsIncluded() throws IOException {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        Map<String, String> metadata = Map.of("type", "document");
        storageService.store("test.txt", new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8)), metadata);

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
        assertEquals("document", captor.getValue().metadata().get("type"));
    }

    @Test
    void givenNoSuchBucket_whenStore_thenThrowsIOException() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(NoSuchBucketException.builder().build());

        assertThrows(
                IOException.class,
                () -> storageService.store(
                        "test.txt", new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    @SuppressWarnings("unchecked")
    void givenExistingKey_whenRetrieve_thenReturnsInputStream() throws IOException {
        ResponseBytes<GetObjectResponse> responseBytes = mock(ResponseBytes.class);
        when(responseBytes.asByteArray()).thenReturn("hello".getBytes(StandardCharsets.UTF_8));
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);

        InputStream result = storageService.retrieve("test.txt");

        assertNotNull(result);
        assertEquals("hello", new String(result.readAllBytes(), StandardCharsets.UTF_8));

        ArgumentCaptor<GetObjectRequest> captor = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3Client).getObjectAsBytes(captor.capture());
        assertEquals("dev/test.txt", captor.getValue().key());
    }

    @Test
    void givenNoSuchKey_whenRetrieve_thenThrowsStorageException() {
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().build());

        assertThrows(StorageException.class, () -> storageService.retrieve("nonexistent.txt"));
    }

    @Test
    void givenKey_whenDelete_thenCallsDeleteObjectWithPrefixedKey() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(DeleteObjectResponse.builder().build());

        storageService.delete("test.txt");

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());
        assertEquals("dev/test.txt", captor.getValue().key());
        assertEquals(bucketName, captor.getValue().bucket());
    }

    @Test
    void givenExistingKey_whenExists_thenReturnsTrue() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().build());

        assertTrue(storageService.exists("test.txt"));

        ArgumentCaptor<HeadObjectRequest> captor = ArgumentCaptor.forClass(HeadObjectRequest.class);
        verify(s3Client).headObject(captor.capture());
        assertEquals("dev/test.txt", captor.getValue().key());
    }

    @Test
    void givenNonexistentKey_whenExists_thenReturnsFalse() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().build());

        assertFalse(storageService.exists("nonexistent.txt"));
    }
}
