package gov.irs.efile;

import java.util.ArrayList;
import java.util.List;

/**
 * Stub for MeF SDK ValidationErrorListType.
 * Contains validation errors from IRS submission processing.
 */
public class ValidationErrorListType {
    private List<ValidationErrorGrp> validationErrorGrp = new ArrayList<>();

    public List<ValidationErrorGrp> getValidationErrorGrp() {
        return validationErrorGrp;
    }

    public void setValidationErrorGrp(List<ValidationErrorGrp> validationErrorGrp) {
        this.validationErrorGrp = validationErrorGrp;
    }

    public static class ValidationErrorGrp {
        private String ruleNum;
        private String severityCd;
        private String errorMessageTxt;

        public String getRuleNum() {
            return ruleNum;
        }

        public void setRuleNum(String ruleNum) {
            this.ruleNum = ruleNum;
        }

        public String getSeverityCd() {
            return severityCd;
        }

        public void setSeverityCd(String severityCd) {
            this.severityCd = severityCd;
        }

        public String getErrorMessageTxt() {
            return errorMessageTxt;
        }

        public void setErrorMessageTxt(String errorMessageTxt) {
            this.errorMessageTxt = errorMessageTxt;
        }
    }
}
