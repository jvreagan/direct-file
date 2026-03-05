package gov.irs.mef.services.transmitter.mtom;

import java.util.Set;

import gov.irs.mef.exception.ServiceException;
import gov.irs.mef.exception.ToolkitException;
import gov.irs.mef.services.ServiceContext;

/**
 * Stub for MeF SDK GetAcksMTOMClient.
 * In the real SDK, polls the IRS for submission acknowledgements via MTOM web service.
 */
public class GetAcksMTOMClient {
    public GetAcksMTOMClient() {
        // Stub: no-op
    }

    public GetAcksResult invoke(ServiceContext serviceContext, Set<String> submissionIds)
            throws ServiceException, ToolkitException {
        throw new ToolkitException("MeF SDK stub: real SDK not available");
    }
}
