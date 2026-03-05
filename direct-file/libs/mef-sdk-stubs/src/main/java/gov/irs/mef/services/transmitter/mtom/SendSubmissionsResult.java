package gov.irs.mef.services.transmitter.mtom;

import gov.irs.mef.SubmissionReceiptList;

/**
 * Stub for MeF SDK SendSubmissionsResult.
 * Contains the result of sending submissions to the IRS via MeF.
 */
public class SendSubmissionsResult {
    private SubmissionReceiptList submissionReceiptList;

    public SubmissionReceiptList getSubmissionReceiptList() {
        return submissionReceiptList;
    }

    public void setSubmissionReceiptList(SubmissionReceiptList submissionReceiptList) {
        this.submissionReceiptList = submissionReceiptList;
    }
}
