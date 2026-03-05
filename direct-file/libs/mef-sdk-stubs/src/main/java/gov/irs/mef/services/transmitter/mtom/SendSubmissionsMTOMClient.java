package gov.irs.mef.services.transmitter.mtom;

import gov.irs.mef.exception.ServiceException;
import gov.irs.mef.exception.ToolkitException;
import gov.irs.mef.inputcomposition.SubmissionContainer;
import gov.irs.mef.services.ServiceContext;

/**
 * Stub for MeF SDK SendSubmissionsMTOMClient.
 * In the real SDK, sends submission containers to the IRS via MTOM web service.
 */
public class SendSubmissionsMTOMClient {
    public SendSubmissionsMTOMClient() {
        // Stub: no-op
    }

    public SendSubmissionsResult invoke(ServiceContext serviceContext, SubmissionContainer submissionContainer)
            throws ServiceException, ToolkitException {
        throw new ToolkitException("MeF SDK stub: real SDK not available");
    }
}
