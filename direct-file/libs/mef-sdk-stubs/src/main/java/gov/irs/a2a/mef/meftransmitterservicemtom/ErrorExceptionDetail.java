package gov.irs.a2a.mef.meftransmitterservicemtom;

import gov.irs.a2a.mef.mefheader.ErrorExceptionDetailType;

/**
 * Stub for MeF SDK ErrorExceptionDetail.
 * Wraps error exception details from MeF transmitter service operations.
 */
public class ErrorExceptionDetail {
    private final String message;
    private final ErrorExceptionDetailType detailType;

    public ErrorExceptionDetail(String message, ErrorExceptionDetailType detailType) {
        this.message = message;
        this.detailType = detailType;
    }

    public String getMessage() {
        return message;
    }

    public ErrorExceptionDetailType getDetailType() {
        return detailType;
    }
}
