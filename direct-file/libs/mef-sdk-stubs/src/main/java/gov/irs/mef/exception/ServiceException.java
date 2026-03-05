package gov.irs.mef.exception;

import java.util.logging.Level;

import gov.irs.a2a.mef.meftransmitterservicemtom.ErrorExceptionDetail;

/**
 * Stub for MeF SDK ServiceException.
 * Thrown when MeF service operations fail.
 */
public class ServiceException extends Exception {
    private final ErrorExceptionDetail detail;
    private final Level level;

    public ServiceException(String message, ErrorExceptionDetail detail, Level level) {
        super(message);
        this.detail = detail;
        this.level = level;
    }

    public ErrorExceptionDetail getDetail() {
        return detail;
    }

    public Level getLevel() {
        return level;
    }
}
