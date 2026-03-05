package gov.irs.mef.inputcomposition;

import java.io.File;

/**
 * Stub for MeF SDK SubmissionManifest.
 * Wraps a submission manifest XML file.
 */
public class SubmissionManifest {
    private final File manifestFile;

    public SubmissionManifest(File manifestFile) {
        this.manifestFile = manifestFile;
    }

    public File getManifestFile() {
        return manifestFile;
    }
}
