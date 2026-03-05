package gov.irs.directfile.api.authorization;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import gov.irs.directfile.api.featureflags.FeatureFlagService;
import gov.irs.directfile.api.user.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenEnrollmentFeatureServiceTest {

    @Mock
    private UserRepository userRepo;

    @Mock
    private FeatureFlagService featureFlagService;

    private OpenEnrollmentFeatureService service;

    @BeforeEach
    void setUp() {
        service = new OpenEnrollmentFeatureService(userRepo, featureFlagService);
    }

    @Test
    void givenInitialState_whenNewUsersAllowed_thenReturnsFalse() {
        // openEnrollmentFeatureEnabled is true (hardcoded in constructor),
        // so newUsersAllowed() returns newUsersAllowed && !maxUserCountReached().
        // newUsersAllowed field starts as false, so result is false.
        boolean result = service.newUsersAllowed();

        assertThat(result).isFalse();
    }

    @Test
    void givenLoadConfigCalled_whenNewUsersAllowed_thenReturnsTrue() {
        // loadOpenEnrollmentConfig sets newUsersAllowed=true via startOpenEnrollment,
        // and maxUsersTarget=200000000. currentUserCount starts at 0, so !maxUserCountReached() is true.
        when(userRepo.countByAccessGranted(true)).thenReturn(0);

        service.loadOpenEnrollmentConfig();

        boolean result = service.newUsersAllowed();
        assertThat(result).isTrue();
    }

    @Test
    void givenMaxUsersReached_whenNewUsersAllowed_thenReturnsFalse() throws Exception {
        // First call loadOpenEnrollmentConfig to set newUsersAllowed=true and maxUsersTarget=200000000
        when(userRepo.countByAccessGranted(true)).thenReturn(0);
        service.loadOpenEnrollmentConfig();

        // Now use reflection to set currentUserCount >= maxUsersTarget
        Field currentUserCountField = OpenEnrollmentFeatureService.class.getDeclaredField("currentUserCount");
        currentUserCountField.setAccessible(true);
        currentUserCountField.set(service, 200000000);

        boolean result = service.newUsersAllowed();
        assertThat(result).isFalse();
    }

    @Test
    void givenLoadConfigCalledTwiceWithSameState_whenNewUsersAllowed_thenStaysTrue() {
        // Calling loadOpenEnrollmentConfig twice should be idempotent.
        // First call: newUsersAllowed changes from false to true -> startOpenEnrollment
        // Second call: newUsersAllowed == newUsersAllowedFeatureFlag (both true) -> checkCurrentUserCount
        when(userRepo.countByAccessGranted(true)).thenReturn(0);

        service.loadOpenEnrollmentConfig();
        service.loadOpenEnrollmentConfig();

        boolean result = service.newUsersAllowed();
        assertThat(result).isTrue();
    }

    @Test
    void givenLoadConfigThrows_whenNewUsersAllowed_thenReturnsFalse() {
        // First set up a valid state where newUsersAllowed=true
        when(userRepo.countByAccessGranted(true)).thenReturn(0).thenThrow(new RuntimeException("DB connection failed"));

        service.loadOpenEnrollmentConfig(); // sets newUsersAllowed=true
        assertThat(service.newUsersAllowed()).isTrue();

        // Second call: loadOpenEnrollmentConfig enters the "else if" branch
        // (this.newUsersAllowed == newUsersAllowedFeatureFlag, both true),
        // calls checkCurrentUserCount -> updateCurrentUserCount -> userRepo.countByAccessGranted throws.
        // The catch block sets newUsersAllowed=false.
        service.loadOpenEnrollmentConfig();

        boolean result = service.newUsersAllowed();
        assertThat(result).isFalse();
    }
}
