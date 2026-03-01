package gov.irs.directfile.status.mef.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import gov.irs.mef.services.ServiceContext;
import gov.irs.mef.services.msi.LogoutClient;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MeFLogoutClientServiceTest {
    private MeFLogoutClientService logoutClientService;

    @Mock
    private LogoutClient logoutClient;

    @Mock
    private ServiceContext serviceContext;

    @BeforeEach
    void setUp() {
        logoutClientService = new MeFLogoutClientService(logoutClient);
    }

    @Test
    void givenServiceContext_whenLogout_thenDelegatesToLogoutClient() throws Exception {
        logoutClientService.logout(serviceContext);

        verify(logoutClient).logout(serviceContext);
    }
}
