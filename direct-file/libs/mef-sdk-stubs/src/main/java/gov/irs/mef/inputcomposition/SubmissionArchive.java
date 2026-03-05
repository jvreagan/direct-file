package gov.irs.mef.inputcomposition;

/**
 * Stub for MeF SDK SubmissionArchive.
 * Represents an IRS submission archive (zip) before postmarking.
 */
public class SubmissionArchive {
    private String submissionId;
    private String archivePath;

    public String getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(String submissionId) {
        this.submissionId = submissionId;
    }

    public String getArchivePath() {
        return archivePath;
    }

    public void setArchivePath(String archivePath) {
        this.archivePath = archivePath;
    }
}
