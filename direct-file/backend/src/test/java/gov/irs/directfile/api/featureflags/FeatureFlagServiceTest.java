package gov.irs.directfile.api.featureflags;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import gov.irs.directfile.api.cache.CacheService;
import gov.irs.directfile.api.config.RedisConfiguration;
import gov.irs.directfile.api.errors.FeatureFlagException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeatureFlagServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private CacheService cacheService;

    private FeatureFlagConfigurationProperties featureFlagConfigurationProperties;
    private FeatureFlagService featureFlagService;

    private static final String ENVIRONMENT_PREFIX = "test/";
    private static final String FEATURE_FLAGS_BUCKET = "my-feature-flags-bucket";
    private static final String FEATURE_FLAGS_OBJECT = "feature-flags.json";
    private static final Duration FEATURE_FLAGS_EXPIRATION = Duration.ofMinutes(5);

    private static final String FEATURE_FLAGS_JSON =
            """
            {
                "open-enrollment": {
                    "new-users-allowed": true,
                    "max-users": 200000000
                },
                "esignature-enabled": false
            }
            """;

    @BeforeEach
    void setUp() {
        featureFlagConfigurationProperties = new FeatureFlagConfigurationProperties(
                ENVIRONMENT_PREFIX, FEATURE_FLAGS_BUCKET, FEATURE_FLAGS_OBJECT, FEATURE_FLAGS_EXPIRATION);
        featureFlagService = new FeatureFlagService(s3Client, featureFlagConfigurationProperties, cacheService);
    }

    @Test
    void givenCacheHit_whenGetFeatureFlags_thenReturnsCachedFlags() {
        FeatureFlags cachedFlags = new FeatureFlags(new OpenEnrollment(true, 100), false);
        when(cacheService.get(
                        eq(RedisConfiguration.FEATURE_FLAG_CACHE_NAME),
                        eq(FEATURE_FLAGS_OBJECT),
                        eq(FeatureFlags.class)))
                .thenReturn(cachedFlags);

        FeatureFlags result = featureFlagService.getFeatureFlags();

        assertThat(result).isSameAs(cachedFlags);
        verify(s3Client, never()).getObjectAsBytes(any(GetObjectRequest.class));
    }

    @Test
    void givenCacheMiss_whenGetFeatureFlags_thenFetchesFromS3() {
        when(cacheService.get(
                        eq(RedisConfiguration.FEATURE_FLAG_CACHE_NAME),
                        eq(FEATURE_FLAGS_OBJECT),
                        eq(FeatureFlags.class)))
                .thenReturn(null);

        ResponseBytes<GetObjectResponse> responseBytes =
                ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), FEATURE_FLAGS_JSON.getBytes());
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);

        FeatureFlags result = featureFlagService.getFeatureFlags();

        assertThat(result).isNotNull();
        assertThat(result.getOpenEnrollment()).isNotNull();
        assertThat(result.getOpenEnrollment().getNewUsersAllowed()).isTrue();
        assertThat(result.isEsignatureEnabled()).isFalse();
        verify(s3Client).getObjectAsBytes(any(GetObjectRequest.class));
        verify(cacheService)
                .set(
                        eq(RedisConfiguration.FEATURE_FLAG_CACHE_NAME),
                        eq(FEATURE_FLAGS_OBJECT),
                        any(FeatureFlags.class),
                        eq(FEATURE_FLAGS_EXPIRATION));
    }

    @Test
    void givenS3Error_whenGetFeatureFlags_thenThrowsFeatureFlagException() {
        when(cacheService.get(
                        eq(RedisConfiguration.FEATURE_FLAG_CACHE_NAME),
                        eq(FEATURE_FLAGS_OBJECT),
                        eq(FeatureFlags.class)))
                .thenReturn(null);
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenThrow(S3Exception.builder().message("Access denied").build());

        assertThatThrownBy(() -> featureFlagService.getFeatureFlags())
                .isInstanceOf(FeatureFlagException.class)
                .hasMessageContaining("Error retrieving feature flags");
    }

    @Test
    void givenCacheHit_whenGetFeatureObjectAsString_thenReturnsCachedString() {
        String objectKey = "email-allowlist.txt";
        String cachedContent = "user@example.com\nother@example.com";
        when(cacheService.get(eq(RedisConfiguration.FEATURE_FLAG_CACHE_NAME), eq(objectKey), eq(String.class)))
                .thenReturn(cachedContent);

        String result = featureFlagService.getFeatureObjectAsString(objectKey);

        assertThat(result).isEqualTo(cachedContent);
        verify(s3Client, never()).getObjectAsBytes(any(GetObjectRequest.class));
    }

    @Test
    void givenCacheMiss_whenGetFeatureObjectAsString_thenFetchesFromS3() {
        String objectKey = "email-allowlist.txt";
        String s3Content = "user@example.com\nother@example.com";
        when(cacheService.get(eq(RedisConfiguration.FEATURE_FLAG_CACHE_NAME), eq(objectKey), eq(String.class)))
                .thenReturn(null);

        ResponseBytes<GetObjectResponse> responseBytes =
                ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), s3Content.getBytes());
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);

        String result = featureFlagService.getFeatureObjectAsString(objectKey);

        assertThat(result).isEqualTo(s3Content);
        verify(s3Client).getObjectAsBytes(any(GetObjectRequest.class));
        verify(cacheService)
                .set(
                        eq(RedisConfiguration.FEATURE_FLAG_CACHE_NAME),
                        eq(objectKey),
                        eq(s3Content),
                        eq(FEATURE_FLAGS_EXPIRATION));
    }
}
