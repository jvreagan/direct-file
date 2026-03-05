package gov.irs.directfile.api.authentication;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;

class LocalUserDetailsCacheServiceTest {

    private LocalUserDetailsCacheService cacheService;

    @BeforeEach
    void setUp() {
        UserDetailsCacheProperties props = new UserDetailsCacheProperties(100L, Duration.ofMinutes(5));
        cacheService = new LocalUserDetailsCacheService(props);
    }

    @Test
    void givenEmptyCache_whenGet_thenReturnsEmpty() {
        // given
        UUID userId = UUID.randomUUID();

        // when
        Optional<UserDetails> result = cacheService.get(userId);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void givenPutEntry_whenGet_thenReturnsEntry() {
        // given
        UUID externalId = UUID.randomUUID();
        SMUserDetailsPrincipal principal =
                new SMUserDetailsPrincipal(UUID.randomUUID(), externalId, "test@test.com", "123456789");
        cacheService.put(externalId, principal);

        // when
        Optional<UserDetails> result = cacheService.get(externalId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(principal);
        SMUserDetailsPrincipal retrieved = (SMUserDetailsPrincipal) result.get();
        assertThat(retrieved.email()).isEqualTo("test@test.com");
        assertThat(retrieved.tin()).isEqualTo("123456789");
    }

    @Test
    void givenEntries_whenClear_thenCacheEmpty() {
        // given
        UUID externalId1 = UUID.randomUUID();
        UUID externalId2 = UUID.randomUUID();
        cacheService.put(
                externalId1, new SMUserDetailsPrincipal(UUID.randomUUID(), externalId1, "user1@test.com", "111111111"));
        cacheService.put(
                externalId2, new SMUserDetailsPrincipal(UUID.randomUUID(), externalId2, "user2@test.com", "222222222"));

        // verify entries exist before clear
        assertThat(cacheService.get(externalId1)).isPresent();
        assertThat(cacheService.get(externalId2)).isPresent();

        // when
        cacheService.clear();

        // then
        assertThat(cacheService.get(externalId1)).isEmpty();
        assertThat(cacheService.get(externalId2)).isEmpty();
    }
}
