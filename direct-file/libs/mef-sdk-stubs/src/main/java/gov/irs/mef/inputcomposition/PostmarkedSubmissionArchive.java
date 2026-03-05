package gov.irs.mef.inputcomposition;

/**
 * Stub for MeF SDK PostmarkedSubmissionArchive.
 * Represents a postmarked (timestamped) submission archive ready for bundling.
 */
public class PostmarkedSubmissionArchive {
    private SubmissionArchive submissionArchive;
    private String postmarkTimestamp;

    public SubmissionArchive getSubmissionArchive() {
        return submissionArchive;
    }

    public void setSubmissionArchive(SubmissionArchive submissionArchive) {
        this.submissionArchive = submissionArchive;
    }

    public String getPostmarkTimestamp() {
        return postmarkTimestamp;
    }

    public void setPostmarkTimestamp(String postmarkTimestamp) {
        this.postmarkTimestamp = postmarkTimestamp;
    }
}
