package gov.irs.mef.services.transmitter.mtom;

import gov.irs.mef.AcknowledgementList;

/**
 * Stub for MeF SDK GetAcksResult.
 * Contains the result of polling for submission acknowledgements.
 */
public class GetAcksResult {
    private AcknowledgementList acknowledgementList;

    public AcknowledgementList getAcknowledgementList() {
        return acknowledgementList;
    }

    public void setAcknowledgementList(AcknowledgementList acknowledgementList) {
        this.acknowledgementList = acknowledgementList;
    }
}
