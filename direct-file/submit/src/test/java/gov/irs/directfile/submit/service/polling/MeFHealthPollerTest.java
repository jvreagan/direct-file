package gov.irs.directfile.submit.service.polling;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import gov.irs.directfile.submit.exception.LoginFailureException;
import gov.irs.directfile.submit.exception.LogoutFailureException;
import gov.irs.directfile.submit.service.interfaces.IBundleSubmissionActionHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeFHealthPollerTest {

    @Mock
    private IBundleSubmissionActionHandler bundleSubmissionActionHandler;

    private MeFHealthPoller meFHealthPoller;

    @BeforeEach
    void setUp() {
        meFHealthPoller = new MeFHealthPoller(bundleSubmissionActionHandler);
    }

    @Test
    void givenSuccessfulLogin_whenPerformCheck_thenReturnsTrueAndLogsOut() throws Exception {
        // given
        when(bundleSubmissionActionHandler.login()).thenReturn(true);

        // when
        boolean result = meFHealthPoller.performMefConnectivityCheck();

        // then
        assertThat(result).isTrue();
        verify(bundleSubmissionActionHandler).login();
        verify(bundleSubmissionActionHandler).logout();
    }

    @Test
    void givenLoginFailure_whenPerformCheck_thenReturnsFalse() throws Exception {
        // given
        when(bundleSubmissionActionHandler.login()).thenThrow(new LoginFailureException("Login failed"));

        // when
        boolean result = meFHealthPoller.performMefConnectivityCheck();

        // then
        assertThat(result).isFalse();
        verify(bundleSubmissionActionHandler).login();
        verify(bundleSubmissionActionHandler, never()).logout();
    }

    @Test
    void givenLogoutFailure_whenPerformCheck_thenReturnsFalse() throws Exception {
        // given
        when(bundleSubmissionActionHandler.login()).thenReturn(true);
        doThrow(new LogoutFailureException("Logout failed"))
                .when(bundleSubmissionActionHandler)
                .logout();

        // when
        boolean result = meFHealthPoller.performMefConnectivityCheck();

        // then
        assertThat(result).isFalse();
        verify(bundleSubmissionActionHandler).login();
        verify(bundleSubmissionActionHandler).logout();
    }
}
