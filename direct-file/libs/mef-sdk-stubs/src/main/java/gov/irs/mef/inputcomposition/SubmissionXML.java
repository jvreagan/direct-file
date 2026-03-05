package gov.irs.mef.inputcomposition;

import java.io.File;

/**
 * Stub for MeF SDK SubmissionXML.
 * Wraps a submission XML file.
 */
public class SubmissionXML {
    private final File submissionFile;

    public SubmissionXML(File submissionFile) {
        this.submissionFile = submissionFile;
    }

    public File getSubmissionFile() {
        return submissionFile;
    }

    public String getXmlData() {
        return null;
    }
}
