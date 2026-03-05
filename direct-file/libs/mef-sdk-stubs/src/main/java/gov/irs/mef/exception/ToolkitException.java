package gov.irs.mef.exception;

/**
 * Stub for MeF SDK ToolkitException.
 * Thrown when MeF toolkit operations fail.
 */
public class ToolkitException extends Exception {
    public ToolkitException(String message) {
        super(message);
    }

    public ToolkitException(String message, Throwable cause) {
        super(message, cause);
    }
}
