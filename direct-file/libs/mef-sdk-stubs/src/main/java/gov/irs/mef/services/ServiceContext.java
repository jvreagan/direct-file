package gov.irs.mef.services;

import gov.irs.a2a.mef.mefheader.TestCdType;
import gov.irs.mef.services.data.ETIN;

/**
 * Stub for MeF SDK ServiceContext.
 * Holds the context for MeF service operations (ETIN, ASID, test/prod mode).
 */
public class ServiceContext {
    private final ETIN etin;
    private String appSysID;
    private final TestCdType testCdType;

    public ServiceContext(ETIN etin, String appSysID, TestCdType testCdType) {
        this.etin = etin;
        this.appSysID = appSysID;
        this.testCdType = testCdType;
    }

    public ETIN getEtin() {
        return etin;
    }

    public String getAppSysID() {
        return appSysID;
    }

    public void setAppSysID(String appSysID) {
        this.appSysID = appSysID;
    }

    public TestCdType getTestCdType() {
        return testCdType;
    }
}
