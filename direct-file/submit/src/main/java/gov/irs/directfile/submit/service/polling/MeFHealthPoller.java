package gov.irs.directfile.submit.service.polling;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import gov.irs.directfile.submit.service.interfaces.IBundleSubmissionActionHandler;

@Component
@Slf4j
public class MeFHealthPoller {
    private final IBundleSubmissionActionHandler bundleSubmissionActionHandler;

    public MeFHealthPoller(IBundleSubmissionActionHandler bundleSubmissionActionHandler) {
        this.bundleSubmissionActionHandler = bundleSubmissionActionHandler;
    }

    public boolean performMefConnectivityCheck() {
        try {
            boolean loginSuccess = bundleSubmissionActionHandler.login();
            if (loginSuccess) {
                bundleSubmissionActionHandler.logout();
            }
            return loginSuccess;
        } catch (Exception e) {
            log.warn("MeF connectivity check failed: {}", e.getMessage());
            return false;
        }
    }
}
