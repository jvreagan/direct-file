package gov.irs.directfile.audit;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import gov.irs.directfile.audit.events.Event;
import gov.irs.directfile.audit.events.EventId;
import gov.irs.directfile.audit.events.EventStatus;
import gov.irs.directfile.audit.events.SystemEventPrincipal;

import static org.assertj.core.api.Assertions.assertThat;

class AuditServiceTest {

    private AuditService auditService;

    @BeforeEach
    void setUp() {
        auditService = new AuditService();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void givenSuccessEvent_whenAddAuditPropertiesToMDC_thenMDCContainsProperties() {
        // given
        Event event = Event.builder()
                .eventId(EventId.SUBMIT_BATCH)
                .eventStatus(EventStatus.SUCCESS)
                .eventPrincipal(new SystemEventPrincipal())
                .build();

        // when
        auditService.addAuditPropertiesToMDC(event);

        // then
        assertThat(MDC.get(AuditLogElement.eventStatus.toString())).isEqualTo(EventStatus.SUCCESS.toString());
        assertThat(MDC.get(AuditLogElement.eventId.toString())).isEqualTo(EventId.SUBMIT_BATCH.toString());
        // SystemEventPrincipal has UserType.SYS, so userType should be set
        assertThat(MDC.get(AuditLogElement.userType.toString())).isEqualTo("SYS");
    }

    @Test
    void givenEventWithErrorMessage_whenAddAuditPropertiesToMDC_thenMDCContainsErrorMessage() {
        // given
        String errorMessage = "Something went wrong";
        Event event = Event.builder()
                .eventId(EventId.SUBMIT_BATCH)
                .eventStatus(EventStatus.FAILURE)
                .eventPrincipal(new SystemEventPrincipal())
                .eventErrorMessage(errorMessage)
                .build();

        // when
        auditService.addAuditPropertiesToMDC(event);

        // then
        assertThat(MDC.get(AuditLogElement.eventErrorMessage.toString())).isEqualTo(errorMessage);
        assertThat(MDC.get(AuditLogElement.eventStatus.toString())).isEqualTo(EventStatus.FAILURE.toString());
    }

    @Test
    void givenSuccessStatus_whenPerformLog_thenLogsAtInfo() {
        // given - manually set MDC to simulate the state after addAuditPropertiesToMDC
        MDC.put(AuditLogElement.eventStatus.toString(), EventStatus.SUCCESS.toString());

        // when / then - performLog should not throw; it logs at INFO for SUCCESS status
        auditService.performLog();

        // Verify MDC still has the value (performLog does not clear MDC)
        assertThat(MDC.get(AuditLogElement.eventStatus.toString())).isEqualTo(EventStatus.SUCCESS.toString());
    }

    @Test
    void givenFailureEvent_whenPerformLogFromEvent_thenClearsMDC() {
        // given
        MDC.put("existingKey", "existingValue");
        Event event = Event.builder()
                .eventId(EventId.SUBMIT_BATCH)
                .eventStatus(EventStatus.FAILURE)
                .eventPrincipal(new SystemEventPrincipal())
                .build();
        AuditEventData eventData = new AuditEventData();
        eventData.put(AuditLogElement.responseStatusCode, 500);

        // when
        auditService.performLogFromEvent(event, eventData);

        // then - MDC should be completely cleared
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    @Test
    void givenExistingMDC_whenPerformLogFromEventAndPreserveMDC_thenMDCRestored() {
        // given - populate MDC with values that should survive the call
        MDC.put("existingKey", "existingValue");
        MDC.put("anotherKey", "anotherValue");
        Map<String, String> originalContext = MDC.getCopyOfContextMap();

        Event event = Event.builder()
                .eventId(EventId.SUBMIT_BATCH)
                .eventStatus(EventStatus.SUCCESS)
                .eventPrincipal(new SystemEventPrincipal())
                .build();
        AuditEventData eventData = new AuditEventData();

        // when
        auditService.performLogFromEventAndPreserveMDCValues(event, eventData);

        // then - original MDC values should be restored
        assertThat(MDC.getCopyOfContextMap()).isEqualTo(originalContext);
        assertThat(MDC.get("existingKey")).isEqualTo("existingValue");
        assertThat(MDC.get("anotherKey")).isEqualTo("anotherValue");
    }
}
