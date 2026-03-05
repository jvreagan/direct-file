package gov.irs.directfile.api.authentication;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import gov.irs.directfile.api.cache.CacheService;
import gov.irs.directfile.api.config.RedisConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RemoteUserDetailsCacheServiceTest {

    @Mock
    private CacheService cacheService;

    @Mock
    private UserDetailsCacheProperties userDetailsCacheProperties;

    @InjectMocks
    private RemoteUserDetailsCacheService remoteUserDetailsCacheService;

    @Test
    void givenCacheMiss_whenGet_thenReturnsEmpty() {
        // given
        UUID externalId = UUID.randomUUID();
        when(cacheService.get(
                        eq(RedisConfiguration.USERS_CACHE_NAME),
                        eq(externalId.toString()),
                        eq(SMUserDetailsProperties.class)))
                .thenReturn(null);

        // when
        Optional<UserDetails> result = remoteUserDetailsCacheService.get(externalId);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void givenCacheHit_whenGet_thenReturnsPrincipal() {
        // given
        UUID id = UUID.randomUUID();
        UUID externalId = UUID.randomUUID();
        String email = "cached@test.com";
        String tin = "999888777";
        SMUserDetailsProperties properties = new SMUserDetailsProperties(id, externalId, email, tin);

        when(cacheService.get(
                        eq(RedisConfiguration.USERS_CACHE_NAME),
                        eq(externalId.toString()),
                        eq(SMUserDetailsProperties.class)))
                .thenReturn(properties);

        // when
        Optional<UserDetails> result = remoteUserDetailsCacheService.get(externalId);

        // then
        assertThat(result).isPresent();
        SMUserDetailsPrincipal principal = (SMUserDetailsPrincipal) result.get();
        assertThat(principal.id()).isEqualTo(id);
        assertThat(principal.externalId()).isEqualTo(externalId);
        assertThat(principal.email()).isEqualTo(email);
        assertThat(principal.tin()).isEqualTo(tin);
    }

    @Test
    void givenExpireAfterWrite_whenPut_thenSetsWithDuration() {
        // given
        UUID externalId = UUID.randomUUID();
        SMUserDetailsPrincipal principal =
                new SMUserDetailsPrincipal(UUID.randomUUID(), externalId, "put@test.com", "111222333");
        Duration expiration = Duration.ofMinutes(10);
        when(userDetailsCacheProperties.expireAfterWrite()).thenReturn(expiration);

        // when
        remoteUserDetailsCacheService.put(externalId, principal);

        // then
        verify(cacheService, times(1))
                .set(
                        eq(RedisConfiguration.USERS_CACHE_NAME),
                        eq(externalId.toString()),
                        any(SMUserDetailsProperties.class),
                        eq(expiration));
    }

    @Test
    void whenClear_thenClearsCacheByName() {
        // when
        remoteUserDetailsCacheService.clear();

        // then
        verify(cacheService, times(1)).clearCache(RedisConfiguration.USERS_CACHE_NAME);
    }
}
