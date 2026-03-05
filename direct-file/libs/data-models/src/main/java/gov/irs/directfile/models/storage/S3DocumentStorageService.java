package gov.irs.directfile.models.storage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.encryption.s3.S3EncryptionClient;

@Slf4j
public class S3DocumentStorageService implements DocumentStorageService {
    private final S3EncryptionClient s3Client;
    private final String bucketName;
    private final String environmentPrefix;

    public S3DocumentStorageService(S3EncryptionClient s3Client, String bucketName, String environmentPrefix) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.environmentPrefix = environmentPrefix;
    }

    private String prefixKey(String key) {
        return StringUtils.prependIfMissing(key, environmentPrefix);
    }

    @Override
    public void store(String key, InputStream data, Map<String, String> metadata) throws IOException {
        String prefixedKey = prefixKey(key);
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(prefixedKey)
                .metadata(metadata.isEmpty() ? null : metadata)
                .build();
        byte[] bytes = IOUtils.toByteArray(data);
        try {
            s3Client.putObject(request, RequestBody.fromBytes(bytes));
            log.info("Stored object {} in bucket {}", prefixedKey, bucketName);
        } catch (NoSuchBucketException e) {
            throw new IOException("Bucket " + bucketName + " does not exist", e);
        } catch (S3Exception e) {
            throw new IOException("Failed to store object in bucket " + bucketName, e);
        }
    }

    @Override
    public InputStream retrieve(String key) throws IOException {
        String prefixedKey = prefixKey(key);
        GetObjectRequest request =
                GetObjectRequest.builder().bucket(bucketName).key(prefixedKey).build();
        try {
            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(request);
            if (objectBytes == null) throw new IOException("Empty object: " + prefixedKey);
            return new ByteArrayInputStream(objectBytes.asByteArray());
        } catch (NoSuchKeyException e) {
            throw new StorageException("Object not found: " + prefixedKey, e);
        }
    }

    @Override
    public void delete(String key) {
        String prefixedKey = prefixKey(key);
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(prefixedKey)
                .build();
        s3Client.deleteObject(request);
        log.info("Deleted object {} from bucket {}", prefixedKey, bucketName);
    }

    @Override
    public List<StorageResource> list(String prefix) {
        String prefixedKey = prefixKey(prefix);
        ListObjectsRequest request = ListObjectsRequest.builder()
                .bucket(bucketName)
                .prefix(prefixedKey)
                .build();
        try {
            ListObjectsResponse response = s3Client.listObjects(request);
            List<StorageResource> resources = new ArrayList<>();
            for (S3Object obj : response.contents()) {
                String resourceId = obj.key().replace(prefixedKey, "");
                int dotIndex = resourceId.lastIndexOf(".");
                if (dotIndex > 0) {
                    resourceId = resourceId.substring(0, dotIndex);
                }
                resources.add(new StorageResource(obj.key(), resourceId, obj.lastModified()));
            }
            return resources;
        } catch (S3Exception e) {
            throw new StorageException("Failed to list objects with prefix: " + prefixedKey, e);
        }
    }

    @Override
    public boolean exists(String key) {
        String prefixedKey = prefixKey(key);
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(prefixedKey)
                    .build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }
}
