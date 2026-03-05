package gov.irs.directfile.submit.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OfflineModeServiceTest {

    private OfflineModeService offlineModeService;

    @BeforeEach
    void setUp() {
        offlineModeService = new OfflineModeService();
    }

    @Test
    void givenInitialState_whenIsOfflineModeEnabled_thenReturnsFalse() {
        // given: a freshly constructed OfflineModeService

        // when
        boolean result = offlineModeService.isOfflineModeEnabled();

        // then
        assertThat(result).isFalse();
    }

    @Test
    void givenDisabled_whenEnableOfflineMode_thenIsEnabled() {
        // given: offline mode is disabled (initial state)
        assertThat(offlineModeService.isOfflineModeEnabled()).isFalse();

        // when
        offlineModeService.enableOfflineMode();

        // then
        assertThat(offlineModeService.isOfflineModeEnabled()).isTrue();
    }

    @Test
    void givenEnabled_whenDisableOfflineMode_thenIsDisabled() {
        // given: offline mode is enabled
        offlineModeService.enableOfflineMode();
        assertThat(offlineModeService.isOfflineModeEnabled()).isTrue();

        // when
        offlineModeService.disableOfflineMode();

        // then
        assertThat(offlineModeService.isOfflineModeEnabled()).isFalse();
    }

    @Test
    void givenEnabled_whenEnableOfflineModeTwice_thenStaysEnabled() {
        // given: offline mode is already enabled
        offlineModeService.enableOfflineMode();
        assertThat(offlineModeService.isOfflineModeEnabled()).isTrue();

        // when: enabling it again
        offlineModeService.enableOfflineMode();

        // then: still enabled (idempotent)
        assertThat(offlineModeService.isOfflineModeEnabled()).isTrue();
    }
}
