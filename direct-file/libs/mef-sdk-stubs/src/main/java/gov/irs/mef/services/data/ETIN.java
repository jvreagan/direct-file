package gov.irs.mef.services.data;

/**
 * Stub for MeF SDK ETIN (Electronic Transmitter Identification Number).
 */
public class ETIN {
    private final String value;

    public ETIN(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
