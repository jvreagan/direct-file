package gov.irs.directfile.status.mef.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import gov.irs.mef.services.msi.LoginClient;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MeFLoginClientServiceTest {
    private MeFLoginClientService loginClientService;

    @Mock
    private LoginClient loginClient;

    @BeforeEach
    void setUp() {
        loginClientService = new MeFLoginClientService(loginClient);
    }

    @Test
    void givenLoginClient_whenLoginWithDefaultAsid_thenReturnsServiceContextWrapper() throws Exception {
        ServiceContextWrapper result = loginClientService.loginWithDefaultAsid();

        assertNotNull(result);
    }
}
