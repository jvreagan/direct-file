package gov.irs.mef.inputcomposition;

/**
 * Stub for MeF SDK BinaryAttachment.
 * Represents a binary attachment included with an IRS submission.
 */
public class BinaryAttachment {
    private String name;
    private byte[] content;

    public BinaryAttachment() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }
}
