package gov.irs.mef;

import java.util.ArrayList;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Stub for MeF SDK SubmissionReceiptList.
 * Contains a list of submission receipt groups returned after sending submissions.
 */
public class SubmissionReceiptList {

    private List<SubmissionReceiptGrp> receipts = new ArrayList<>();

    public List<SubmissionReceiptGrp> getReceipts() {
        return receipts;
    }

    public void setReceipts(List<SubmissionReceiptGrp> receipts) {
        this.receipts = receipts;
    }

    public static class SubmissionReceiptGrp {
        private String submissionId;
        private String receiptId;
        private XMLGregorianCalendar submissionReceivedTs;

        public String getSubmissionId() {
            return submissionId;
        }

        public void setSubmissionId(String submissionId) {
            this.submissionId = submissionId;
        }

        public String getReceiptId() {
            return receiptId;
        }

        public void setReceiptId(String receiptId) {
            this.receiptId = receiptId;
        }

        public XMLGregorianCalendar getSubmissionReceivedTs() {
            return submissionReceivedTs;
        }

        public void setSubmissionReceivedTs(XMLGregorianCalendar submissionReceivedTs) {
            this.submissionReceivedTs = submissionReceivedTs;
        }
    }
}
