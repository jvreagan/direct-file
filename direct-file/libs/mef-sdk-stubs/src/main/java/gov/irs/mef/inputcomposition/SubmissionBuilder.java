package gov.irs.mef.inputcomposition;

import java.util.GregorianCalendar;

import gov.irs.mef.exception.ToolkitException;

/**
 * Stub for MeF SDK SubmissionBuilder.
 * In the real SDK, builds submission archives, postmarked archives, and containers.
 */
public class SubmissionBuilder {

    public static SubmissionArchive createIRSSubmissionArchive(
            String submissionId,
            SubmissionManifest manifest,
            SubmissionXML xml,
            BinaryAttachment[] attachments,
            String outputPath)
            throws ToolkitException {
        SubmissionArchive archive = new SubmissionArchive();
        archive.setSubmissionId(submissionId);
        archive.setArchivePath(outputPath);
        return archive;
    }

    public static PostmarkedSubmissionArchive createPostmarkedSubmissionArchive(
            SubmissionArchive archive, GregorianCalendar calendar) throws ToolkitException {
        PostmarkedSubmissionArchive postmarked = new PostmarkedSubmissionArchive();
        postmarked.setSubmissionArchive(archive);
        return postmarked;
    }

    public static SubmissionContainer createSubmissionContainer(
            PostmarkedSubmissionArchive[] archives, String outputPath) throws ToolkitException {
        SubmissionContainer container = new SubmissionContainer();
        container.setContainerPath(outputPath);
        return container;
    }
}
