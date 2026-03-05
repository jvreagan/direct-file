package gov.irs.directfile.status.mef.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import gov.irs.mef.exception.ServiceException;
import gov.irs.mef.exception.ToolkitException;
import gov.irs.mef.services.ServiceContext;
import gov.irs.mef.services.msi.LogoutClient;

@Slf4j
@Service
public class MeFLogoutClientService {
    private final LogoutClient logoutClient;

    public MeFLogoutClientService(LogoutClient logoutClient) {
        this.logoutClient = logoutClient;
    }

    public void logout(ServiceContext serviceContext) throws ServiceException, ToolkitException {
        log.info("Logging out from MeF");
        logoutClient.logout(serviceContext);
    }
}
