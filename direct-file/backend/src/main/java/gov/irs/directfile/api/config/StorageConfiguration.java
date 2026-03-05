package gov.irs.directfile.api.config;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.encryption.s3.S3EncryptionClient;

import gov.irs.directfile.models.storage.*;

@Configuration
public class StorageConfiguration {

    @Bean
    @Profile(BeanProfiles.AWS)
    public DocumentStorageService s3DocumentStorageService(
            S3EncryptionClient s3EncryptionClient, S3ConfigurationProperties s3ConfigurationProperties) {
        return new S3DocumentStorageService(
                s3EncryptionClient,
                s3ConfigurationProperties.getS3().getBucket(),
                s3ConfigurationProperties.getS3().getEnvironmentPrefix());
    }

    @Bean
    @Profile(BeanProfiles.LOCAL)
    public DocumentStorageService localFileDocumentStorageService(
            @Value("${direct-file.storage.local.base-directory:./local-storage}") String baseDirectory) {
        return new LocalFileDocumentStorageService(Path.of(baseDirectory));
    }
}
