package gov.irs.directfile.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import gov.irs.directfile.models.encryption.*;

@Configuration
public class EncryptionConfiguration {

    @Bean
    @Profile(BeanProfiles.AWS)
    public EncryptionService kmsEncryptionService(DataEncryptDecrypt dataEncryptDecrypt) {
        return new KmsEncryptionService(dataEncryptDecrypt);
    }

    @Bean
    @Profile(BeanProfiles.LOCAL)
    public EncryptionService noOpEncryptionService() {
        return new NoOpEncryptionService();
    }
}
