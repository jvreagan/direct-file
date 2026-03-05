package gov.irs.mef;

import java.util.ArrayList;
import java.util.Collection;

import gov.irs.efile.ValidationErrorListType;

/**
 * Stub for MeF SDK AcknowledgementList.
 * Contains acknowledgements returned when polling for submission status.
 */
public class AcknowledgementList {

    private Collection<Acknowledgement> acknowledgements = new ArrayList<>();

    public Collection<Acknowledgement> getAcknowledgements() {
        return acknowledgements;
    }

    public void setAcknowledgements(Collection<Acknowledgement> acknowledgements) {
        this.acknowledgements = acknowledgements;
    }

    public static class Acknowledgement {
        private String submissionId;
        private String receiptId;
        private String acceptanceStatusTxt;
        private ValidationErrorListType validationErrorList;

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

        public String getAcceptanceStatusTxt() {
            return acceptanceStatusTxt;
        }

        public void setAcceptanceStatusTxt(String acceptanceStatusTxt) {
            this.acceptanceStatusTxt = acceptanceStatusTxt;
        }

        public ValidationErrorListType getValidationErrorList() {
            return validationErrorList;
        }

        public void setValidationErrorList(ValidationErrorListType validationErrorList) {
            this.validationErrorList = validationErrorList;
        }
    }
}
