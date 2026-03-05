package gov.irs.mef.services.msi;

import gov.irs.mef.exception.ServiceException;
import gov.irs.mef.exception.ToolkitException;
import gov.irs.mef.services.ServiceContext;

/**
 * Stub for MeF SDK LoginClient.
 * In the real SDK, handles authentication with the MeF system.
 */
public class LoginClient {
    public LoginClient() {
        // Stub: no-op
    }

    public ServiceContext login(ServiceContext serviceContext) throws ServiceException, ToolkitException {
        // Stub: returns the same context
        return serviceContext;
    }
}
