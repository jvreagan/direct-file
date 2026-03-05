package gov.irs.directfile.submit.service;

import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import gov.irs.mef.exception.ServiceException;
import gov.irs.mef.exception.ToolkitException;
import gov.irs.mef.services.ServiceContext;
import gov.irs.mef.services.msi.LoginClient;
import gov.irs.mef.services.msi.LogoutClient;
import gov.irs.mef.services.transmitter.mtom.SendSubmissionsMTOMClient;
import gov.irs.mef.services.transmitter.mtom.SendSubmissionsResult;

import gov.irs.directfile.audit.AuditEventData;
import gov.irs.directfile.audit.AuditLogElement;
import gov.irs.directfile.audit.AuditService;
import gov.irs.directfile.audit.events.Event;
import gov.irs.directfile.audit.events.EventId;
import gov.irs.directfile.audit.events.EventStatus;
import gov.irs.directfile.audit.events.SystemEventPrincipal;
import gov.irs.directfile.submit.actions.ActionContext;
import gov.irs.directfile.submit.actions.exception.SubmissionFailureException;
import gov.irs.directfile.submit.command.SubmitBundleAction;
import gov.irs.directfile.submit.config.Config;
import gov.irs.directfile.submit.domain.BundledArchives;
import gov.irs.directfile.submit.domain.SendSubmissionsResultWrapper;
import gov.irs.directfile.submit.domain.SubmissionBatch;
import gov.irs.directfile.submit.domain.SubmittedDataContainer;
import gov.irs.directfile.submit.domain.UserContextData;
import gov.irs.directfile.submit.exception.LoginFailureException;
import gov.irs.directfile.submit.exception.LogoutFailureException;
import gov.irs.directfile.submit.service.interfaces.IBundleSubmissionActionHandler;

@Service
@Slf4j
@SuppressFBWarnings(
        value = {"NM_METHOD_NAMING_CONVENTION"},
        justification = "Initial SpotBugs Setup")
public class MefBundleSubmissionActionHandler implements IBundleSubmissionActionHandler {
    private final Config config;
    private final ActionContext actionContext;

    private static final AuditService auditService = new AuditService();

    public MefBundleSubmissionActionHandler(Config config, ActionContext actionContext) {
        this.config = config;
        this.actionContext = actionContext;
    }

    @Override
    public boolean login() throws LoginFailureException {
        try {
            LoginClient loginClient = new LoginClient();
            loginClient.login(actionContext.getServiceContext());
            return true;
        } catch (ServiceException | ToolkitException e) {
            throw new LoginFailureException("Failed to login to MeF", e);
        }
    }

    @Override
    public boolean logout() throws LogoutFailureException {
        try {
            LogoutClient logoutClient = new LogoutClient();
            logoutClient.logout(actionContext.getServiceContext());
            return true;
        } catch (ServiceException | ToolkitException e) {
            throw new LogoutFailureException("Failed to logout from MeF", e);
        }
    }

    @Override
    public SubmittedDataContainer submitBundles(BundledArchives bundledArchives, SubmissionBatch submissionBatch)
            throws SubmissionFailureException {
        ServiceContext serviceContext = actionContext.getServiceContext();
        List<UserContextData> userContexts = bundledArchives.UserContexts;
        String submissionIds = userContexts.stream()
                .map(UserContextData::getSubmissionId)
                .reduce((a, b) -> a + "," + b)
                .orElse("");

        try {
            SendSubmissionsMTOMClient client = new SendSubmissionsMTOMClient();
            SendSubmissionsResult result = client.invoke(serviceContext, bundledArchives.submissionContainer);
            SendSubmissionsResultWrapper resultWrapper = new SendSubmissionsResultWrapper(result);

            Event successEvent = Event.builder()
                    .eventId(EventId.SUBMIT_BATCH)
                    .eventStatus(EventStatus.SUCCESS)
                    .eventPrincipal(new SystemEventPrincipal())
                    .detail(Map.of("mefSubmissionIds", submissionIds).toString())
                    .build();
            AuditEventData successEventData = new AuditEventData();
            successEventData.put(AuditLogElement.cyberOnly, true);
            successEventData.put(AuditLogElement.responseStatusCode, "200");
            successEventData.put(
                    AuditLogElement.remoteAddress, userContexts.get(0).getRemoteAddress());
            auditService.performLogFromEvent(successEvent, successEventData);

            return new SubmittedDataContainer(userContexts, resultWrapper, submissionBatch);
        } catch (ServiceException | ToolkitException e) {
            Event failureEvent = Event.builder()
                    .eventId(EventId.SUBMIT_BATCH)
                    .eventStatus(EventStatus.FAILURE)
                    .eventPrincipal(new SystemEventPrincipal())
                    .eventErrorMessage(e.getClass().getName())
                    .detail(Map.of("mefSubmissionIds", submissionIds, "errorMessage", e.toString())
                            .toString())
                    .build();
            AuditEventData failureEventData = new AuditEventData();
            failureEventData.put(AuditLogElement.cyberOnly, true);
            failureEventData.put(AuditLogElement.responseStatusCode, "400");
            failureEventData.put(AuditLogElement.eventErrorMessage, e.getClass().getName());
            failureEventData.put(
                    AuditLogElement.remoteAddress, userContexts.get(0).getRemoteAddress());
            auditService.performLogFromEvent(failureEvent, failureEventData);

            throw new SubmissionFailureException(submissionBatch, bundledArchives, e);
        }
    }

    @Override
    public SubmittedDataContainer handleCommand(SubmitBundleAction command) throws SubmissionFailureException {
        return this.submitBundles(
                command.getBundleArchivesActionResult().getBundledArchives(),
                command.getBundleArchivesActionResult().getBatch());
    }

    @Override
    public void Setup(Config config) throws Throwable {
        // Setup handled by ActionContext initialization
    }
}
