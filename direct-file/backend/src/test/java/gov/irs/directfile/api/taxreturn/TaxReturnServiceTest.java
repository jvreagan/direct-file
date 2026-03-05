package gov.irs.directfile.api.taxreturn;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import gov.irs.directfile.api.audit.AuditService;
import gov.irs.directfile.api.config.StatusEndpointProperties;
import gov.irs.directfile.api.dataimport.gating.DataImportBehavior;
import gov.irs.directfile.api.dataimport.gating.DataImportGatingService;
import gov.irs.directfile.api.dispatch.DispatchService;
import gov.irs.directfile.api.errors.*;
import gov.irs.directfile.api.loaders.service.FactGraphService;
import gov.irs.directfile.api.taxreturn.dto.Status;
import gov.irs.directfile.api.taxreturn.dto.StatusResponseBody;
import gov.irs.directfile.api.taxreturn.models.SubmissionEvent;
import gov.irs.directfile.api.taxreturn.models.TaxReturn;
import gov.irs.directfile.api.taxreturn.models.TaxReturnSubmission;
import gov.irs.directfile.api.taxreturn.submissions.SendEmailQueueService;
import gov.irs.directfile.api.taxreturn.submissions.lock.AdvisoryLockRepository;
import gov.irs.directfile.api.user.UserService;
import gov.irs.directfile.api.user.models.User;
import gov.irs.directfile.models.FactTypeWithItem;
import gov.irs.directfile.models.message.event.SubmissionEventTypeEnum;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaxReturnServiceTest {

    @Mock
    private AuditService auditService;

    @Mock
    private TaxReturnRepository taxReturnRepo;

    @Mock
    private TaxReturnSubmissionRepository taxReturnSubmissionRepo;

    @Mock
    private UserService userService;

    @Mock
    private DispatchService dispatchService;

    @Mock
    private FactGraphService factGraphService;

    @Mock
    private StatusEndpointProperties statusEndpointProperties;

    @Mock
    private SendEmailQueueService sendEmailQueueService;

    @Mock
    private SubmissionEventRepository submissionEventRepository;

    @Mock
    private AdvisoryLockRepository advisoryLockRepository;

    @Mock
    private StatusResponseBodyCacheService statusResponseBodyCacheService;

    @Mock
    private DataImportGatingService dataImportGatingService;

    private TaxReturnService taxReturnService;

    private static final UUID TAX_RETURN_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final int TAX_YEAR = 2024;
    private static final String USER_EMAIL = "test@example.com";
    private static final String USER_TIN = "123-45-6789";

    private final Clock fixedClock = Clock.fixed(
            Instant.parse("2024-04-15T12:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        taxReturnService = new TaxReturnService(
                auditService,
                taxReturnRepo,
                taxReturnSubmissionRepo,
                userService,
                dispatchService,
                factGraphService,
                restClientBuilder,
                statusEndpointProperties,
                sendEmailQueueService,
                submissionEventRepository,
                fixedClock,
                advisoryLockRepository,
                statusResponseBodyCacheService,
                dataImportGatingService);
    }

    // --- findByUserId ---

    @Test
    void givenUserId_whenFindByUserId_thenDelegatesToRepository() {
        List<TaxReturn> expected = List.of(TaxReturn.testObjectFactory());
        when(taxReturnRepo.findByUserId(USER_ID)).thenReturn(expected);

        List<TaxReturn> result = taxReturnService.findByUserId(USER_ID);

        assertThat(result).isEqualTo(expected);
        verify(taxReturnRepo).findByUserId(USER_ID);
    }

    // --- findByIdAndUserId ---

    @Test
    void givenExistingTaxReturn_whenFindByIdAndUserId_thenReturnsTaxReturn() {
        TaxReturn taxReturn = TaxReturn.testObjectFactory();
        when(taxReturnRepo.findByIdAndUserId(TAX_RETURN_ID, USER_ID)).thenReturn(Optional.of(taxReturn));

        Optional<TaxReturn> result = taxReturnService.findByIdAndUserId(TAX_RETURN_ID, USER_ID);

        assertTrue(result.isPresent());
        assertThat(result.get()).isEqualTo(taxReturn);
    }

    @Test
    void givenNoTaxReturnForUser_whenFindByIdAndUserId_thenFallsBackToDevUser() {
        TaxReturn taxReturn = TaxReturn.testObjectFactory();
        UUID devUserId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        User devUser = mock(User.class);
        when(devUser.getId()).thenReturn(devUserId);

        when(taxReturnRepo.findByIdAndUserId(TAX_RETURN_ID, USER_ID)).thenReturn(Optional.empty());
        when(userService.getOrCreateUserDev()).thenReturn(Optional.of(devUser));
        when(taxReturnRepo.findByIdAndUserId(TAX_RETURN_ID, devUserId)).thenReturn(Optional.of(taxReturn));

        Optional<TaxReturn> result = taxReturnService.findByIdAndUserId(TAX_RETURN_ID, USER_ID);

        assertTrue(result.isPresent());
        verify(userService).getOrCreateUserDev();
    }

    @Test
    void givenNoTaxReturnAtAll_whenFindByIdAndUserId_thenReturnsEmpty() {
        UUID devUserId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        User devUser = mock(User.class);
        when(devUser.getId()).thenReturn(devUserId);

        when(taxReturnRepo.findByIdAndUserId(TAX_RETURN_ID, USER_ID)).thenReturn(Optional.empty());
        when(userService.getOrCreateUserDev()).thenReturn(Optional.of(devUser));
        when(taxReturnRepo.findByIdAndUserId(TAX_RETURN_ID, devUserId)).thenReturn(Optional.empty());

        Optional<TaxReturn> result = taxReturnService.findByIdAndUserId(TAX_RETURN_ID, USER_ID);

        assertTrue(result.isEmpty());
    }

    // --- create ---

    @Test
    void givenValidInput_whenCreate_thenReturnsSavedTaxReturn() throws Exception {
        Map<String, FactTypeWithItem> facts = new HashMap<>();
        User user = new User(UUID.randomUUID());
        TaxReturn savedTaxReturn = TaxReturn.testObjectFactory();

        when(taxReturnRepo.findByUserIdAndTaxYear(USER_ID, TAX_YEAR)).thenReturn(Optional.empty());
        when(userService.getOrCreateUserDev()).thenReturn(Optional.of(user));
        when(factGraphService.factsParseCorrectly(any())).thenReturn(true);
        when(dataImportGatingService.getBehavior(USER_EMAIL))
                .thenReturn(DataImportBehavior.DATA_IMPORT_ABOUT_YOU_BASIC);
        when(taxReturnRepo.save(any(TaxReturn.class))).thenReturn(savedTaxReturn);

        TaxReturn result = taxReturnService.create(
                TAX_YEAR, facts, USER_ID, USER_EMAIL, USER_TIN, "127.0.0.1", 8080, "TestAgent");

        assertNotNull(result);
        verify(taxReturnRepo).save(any(TaxReturn.class));
        verify(dataImportGatingService).getBehavior(USER_EMAIL);
    }

    @Test
    void givenExistingTaxReturn_whenCreate_thenThrowsInvalidOperationException() {
        Map<String, FactTypeWithItem> facts = new HashMap<>();
        TaxReturn existing = TaxReturn.testObjectFactory();
        when(taxReturnRepo.findByUserIdAndTaxYear(USER_ID, TAX_YEAR)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> taxReturnService.create(
                TAX_YEAR, facts, USER_ID, USER_EMAIL, USER_TIN, "127.0.0.1", 8080, "TestAgent"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void givenNoUser_whenCreate_thenThrowsInvalidDataException() {
        Map<String, FactTypeWithItem> facts = new HashMap<>();
        when(taxReturnRepo.findByUserIdAndTaxYear(USER_ID, TAX_YEAR)).thenReturn(Optional.empty());
        when(userService.getOrCreateUserDev()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taxReturnService.create(
                TAX_YEAR, facts, USER_ID, USER_EMAIL, USER_TIN, "127.0.0.1", 8080, "TestAgent"))
                .isInstanceOf(InvalidDataException.class)
                .hasMessageContaining("No user found");
    }

    @Test
    void givenFactsFailToParse_whenCreate_thenThrowsFactGraphParseException() {
        Map<String, FactTypeWithItem> facts = new HashMap<>();
        User user = new User(UUID.randomUUID());

        when(taxReturnRepo.findByUserIdAndTaxYear(USER_ID, TAX_YEAR)).thenReturn(Optional.empty());
        when(userService.getOrCreateUserDev()).thenReturn(Optional.of(user));
        when(factGraphService.factsParseCorrectly(any())).thenReturn(false);

        assertThatThrownBy(() -> taxReturnService.create(
                TAX_YEAR, facts, USER_ID, USER_EMAIL, USER_TIN, "127.0.0.1", 8080, "TestAgent"))
                .isInstanceOf(FactGraphParseResponseStatusException.class);
    }

    @Test
    void givenBlankTin_whenCreate_thenThrowsInvalidDataException() {
        Map<String, FactTypeWithItem> facts = new HashMap<>();
        User user = new User(UUID.randomUUID());

        when(taxReturnRepo.findByUserIdAndTaxYear(USER_ID, TAX_YEAR)).thenReturn(Optional.empty());
        when(userService.getOrCreateUserDev()).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> taxReturnService.create(
                TAX_YEAR, facts, USER_ID, USER_EMAIL, "", "127.0.0.1", 8080, "TestAgent"))
                .isInstanceOf(InvalidDataException.class)
                .hasMessageContaining("TIN is blank");
    }

    @Test
    void givenInvalidTinLength_whenCreate_thenThrowsInvalidDataException() {
        Map<String, FactTypeWithItem> facts = new HashMap<>();
        User user = new User(UUID.randomUUID());

        when(taxReturnRepo.findByUserIdAndTaxYear(USER_ID, TAX_YEAR)).thenReturn(Optional.empty());
        when(userService.getOrCreateUserDev()).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> taxReturnService.create(
                TAX_YEAR, facts, USER_ID, USER_EMAIL, "12345", "127.0.0.1", 8080, "TestAgent"))
                .isInstanceOf(InvalidDataException.class)
                .hasMessageContaining("Invalid TIN");
    }

    // --- update ---

    @Test
    void givenExistingTaxReturn_whenUpdate_thenReturnsSavedTaxReturn() throws Exception {
        Map<String, FactTypeWithItem> facts = new HashMap<>();
        TaxReturn existing = TaxReturn.testObjectFactory();
        TaxReturn saved = TaxReturn.testObjectFactory();

        when(taxReturnRepo.findByIdAndUserId(TAX_RETURN_ID, USER_ID)).thenReturn(Optional.of(existing));
        when(factGraphService.factsParseCorrectly(facts)).thenReturn(true);
        when(taxReturnRepo.save(existing)).thenReturn(saved);

        TaxReturn result = taxReturnService.update(TAX_RETURN_ID, facts, "storeData", true, USER_ID);

        assertNotNull(result);
        verify(taxReturnRepo).save(existing);
    }

    @Test
    void givenNonExistentTaxReturn_whenUpdate_thenThrowsTaxReturnNotFound() {
        Map<String, FactTypeWithItem> facts = new HashMap<>();

        // Both the initial lookup and the dev-user fallback return empty
        when(taxReturnRepo.findByIdAndUserId(eq(TAX_RETURN_ID), any(UUID.class))).thenReturn(Optional.empty());
        when(userService.getOrCreateUserDev()).thenReturn(Optional.of(
                new User(UUID.fromString("00000000-0000-0000-0000-000000000000"))));

        assertThatThrownBy(() -> taxReturnService.update(TAX_RETURN_ID, facts, null, null, USER_ID))
                .isInstanceOf(TaxReturnNotFoundResponseStatusException.class);
    }

    @Test
    void givenFactsFailToParse_whenUpdate_thenThrowsInvalidDataException() {
        Map<String, FactTypeWithItem> facts = new HashMap<>();
        TaxReturn existing = TaxReturn.testObjectFactory();

        when(taxReturnRepo.findByIdAndUserId(TAX_RETURN_ID, USER_ID)).thenReturn(Optional.of(existing));
        when(factGraphService.factsParseCorrectly(facts)).thenReturn(false);

        assertThatThrownBy(() -> taxReturnService.update(TAX_RETURN_ID, facts, null, null, USER_ID))
                .isInstanceOf(InvalidDataException.class)
                .hasMessageContaining("do not parse correctly");
    }

    @Test
    void givenNullSurveyOptIn_whenUpdate_thenSurveyOptInNotSet() throws Exception {
        Map<String, FactTypeWithItem> facts = new HashMap<>();
        TaxReturn existing = TaxReturn.testObjectFactory();
        existing.setSurveyOptIn(true);

        when(taxReturnRepo.findByIdAndUserId(TAX_RETURN_ID, USER_ID)).thenReturn(Optional.of(existing));
        when(factGraphService.factsParseCorrectly(facts)).thenReturn(true);
        when(taxReturnRepo.save(existing)).thenReturn(existing);

        taxReturnService.update(TAX_RETURN_ID, facts, "storeData", null, USER_ID);

        // surveyOptIn should remain the original value (true) since null was passed
        assertThat(existing.getSurveyOptIn()).isTrue();
    }

    // --- isTaxReturnEditable ---

    @Test
    void givenNoSubmissions_whenIsTaxReturnEditable_thenReturnsTrue() {
        when(taxReturnSubmissionRepo.isTaxReturnEditable(TAX_RETURN_ID)).thenReturn(Optional.empty());

        boolean result = taxReturnService.isTaxReturnEditable(TAX_RETURN_ID);

        assertTrue(result);
    }

    @Test
    void givenEditableSubmission_whenIsTaxReturnEditable_thenReturnsTrue() {
        when(taxReturnSubmissionRepo.isTaxReturnEditable(TAX_RETURN_ID)).thenReturn(Optional.of(true));

        boolean result = taxReturnService.isTaxReturnEditable(TAX_RETURN_ID);

        assertTrue(result);
    }

    @Test
    void givenNonEditableSubmission_whenIsTaxReturnEditable_thenReturnsFalse() {
        when(taxReturnSubmissionRepo.isTaxReturnEditable(TAX_RETURN_ID)).thenReturn(Optional.of(false));

        boolean result = taxReturnService.isTaxReturnEditable(TAX_RETURN_ID);

        assertFalse(result);
    }

    // --- submit (advisory lock) ---

    @Test
    void givenLockNotAcquired_whenSubmit_thenThrowsUneditableTaxReturn() {
        Map<String, FactTypeWithItem> facts = new HashMap<>();
        when(advisoryLockRepository.acquireLock(anyInt())).thenReturn(false);

        assertThatThrownBy(() -> taxReturnService.submit(
                TAX_RETURN_ID, facts, USER_ID, null, "127.0.0.1", 8080, "TestAgent"))
                .isInstanceOf(UneditableTaxReturnResponseStatusException.class);

        verify(advisoryLockRepository, never()).releaseLock(anyInt());
    }

    @Test
    void givenLockAcquiredButFactsParseFails_whenSubmit_thenReleasesLockAndThrows() {
        Map<String, FactTypeWithItem> facts = new HashMap<>();
        when(advisoryLockRepository.acquireLock(anyInt())).thenReturn(true);
        when(factGraphService.getGraph(facts)).thenThrow(new FactGraphParseException("bad facts"));

        assertThatThrownBy(() -> taxReturnService.submit(
                TAX_RETURN_ID, facts, USER_ID, null, "127.0.0.1", 8080, "TestAgent"))
                .isInstanceOf(FactGraphParseResponseStatusException.class);

        verify(advisoryLockRepository).releaseLock(anyInt());
    }

    // --- getStatus ---

    @Test
    void givenNonExistentTaxReturn_whenGetStatus_thenThrowsTaxReturnNotFound() {
        when(taxReturnRepo.findByIdAndUserId(eq(TAX_RETURN_ID), any(UUID.class))).thenReturn(Optional.empty());
        when(userService.getOrCreateUserDev()).thenReturn(Optional.of(
                new User(UUID.fromString("00000000-0000-0000-0000-000000000000"))));

        assertThatThrownBy(() -> taxReturnService.getStatus(TAX_RETURN_ID, USER_ID))
                .isInstanceOf(TaxReturnNotFoundResponseStatusException.class);
    }

    @Test
    void givenNoSubmission_whenGetStatus_thenThrowsResponseStatusException() {
        TaxReturn taxReturn = TaxReturn.testObjectFactory();
        when(taxReturnRepo.findByIdAndUserId(TAX_RETURN_ID, USER_ID)).thenReturn(Optional.of(taxReturn));
        when(taxReturnSubmissionRepo.findLatestTaxReturnSubmissionByTaxReturnId(TAX_RETURN_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> taxReturnService.getStatus(TAX_RETURN_ID, USER_ID))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void givenCachedStatus_whenGetStatus_thenReturnsCachedResult() {
        TaxReturn taxReturn = TaxReturn.testObjectFactory();
        TaxReturnSubmission submission = TaxReturnSubmission.testObjectFactory(taxReturn);
        submission.setSubmissionId("sub-123");
        StatusResponseBody cachedBody = new StatusResponseBody(
                Status.Accepted, "status.accepted", List.of(), new Date());

        when(taxReturnRepo.findByIdAndUserId(TAX_RETURN_ID, USER_ID)).thenReturn(Optional.of(taxReturn));
        when(taxReturnSubmissionRepo.findLatestTaxReturnSubmissionByTaxReturnId(TAX_RETURN_ID))
                .thenReturn(Optional.of(submission));
        when(statusResponseBodyCacheService.get("sub-123")).thenReturn(Optional.of(cachedBody));

        StatusResponseBody result = taxReturnService.getStatus(TAX_RETURN_ID, USER_ID);

        assertThat(result).isEqualTo(cachedBody);
        // Should not query the submission event repository when cache hit
        verify(submissionEventRepository, never()).getLatestSubmissionEventByTaxReturnId(any());
    }

    // --- getStatusForSubmissionEvent ---

    @Test
    void givenAcceptedEvent_whenGetStatusForSubmissionEvent_thenReturnsAccepted() {
        SubmissionEvent event = SubmissionEvent.testObjectFactory(SubmissionEventTypeEnum.ACCEPTED);
        event.setCreatedAt(new Date());
        TaxReturn taxReturn = TaxReturn.testObjectFactory();

        StatusResponseBody result = taxReturnService.getStatusForSubmissionEvent(event, "sub-123", taxReturn);

        assertThat(result.getStatus()).isEqualTo(Status.Accepted);
        assertThat(result.getTranslationKey()).isEqualTo("status.accepted");
    }

    @Test
    void givenFailedEvent_whenGetStatusForSubmissionEvent_thenReturnsError() {
        SubmissionEvent event = SubmissionEvent.testObjectFactory(SubmissionEventTypeEnum.FAILED);
        event.setCreatedAt(new Date());
        TaxReturn taxReturn = TaxReturn.testObjectFactory();

        StatusResponseBody result = taxReturnService.getStatusForSubmissionEvent(event, "sub-123", taxReturn);

        assertThat(result.getStatus()).isEqualTo(Status.Error);
        assertThat(result.getTranslationKey()).isEqualTo("status.error");
    }

    @Test
    void givenProcessingEventWithPriorSubmission_whenGetStatusForSubmissionEvent_thenReturnsPending() {
        SubmissionEvent event = SubmissionEvent.testObjectFactory(SubmissionEventTypeEnum.PROCESSING);
        event.setCreatedAt(new Date());
        TaxReturn taxReturn = TaxReturn.testObjectFactory();
        taxReturn.setMostRecentSubmitTime(new Date());

        StatusResponseBody result = taxReturnService.getStatusForSubmissionEvent(event, "sub-123", taxReturn);

        assertThat(result.getStatus()).isEqualTo(Status.Pending);
    }

    @Test
    void givenProcessingEventWithNoPriorSubmission_whenGetStatusForSubmissionEvent_thenThrows() {
        SubmissionEvent event = SubmissionEvent.testObjectFactory(SubmissionEventTypeEnum.PROCESSING);
        event.setCreatedAt(new Date());
        TaxReturn taxReturn = TaxReturn.testObjectFactory();
        // submitTime is null by default, so hasBeenSubmittedAtLeastOnce() returns false

        assertThatThrownBy(() -> taxReturnService.getStatusForSubmissionEvent(event, "sub-123", taxReturn))
                .isInstanceOf(ResponseStatusException.class);
    }

    // --- getFormattedSystemTimestampForOffset ---

    @Test
    void givenEasternTimezoneOffset_whenGetFormattedSystemTimestamp_thenFormatsCorrectly() {
        // EDT offset is 240 minutes (UTC-4). JS sends positive for behind-UTC.
        Instant instant = Instant.parse("2024-04-15T16:00:00Z");

        String result = TaxReturnService.getFormattedSystemTimestampForOffset(240, instant);

        // UTC-4 means 16:00 UTC -> 12:00 EDT
        assertThat(result).isEqualTo("2024-04-15T12:00:00-04:00");
    }

    @Test
    void givenUtcOffset_whenGetFormattedSystemTimestamp_thenFormatsAtUtc() {
        Instant instant = Instant.parse("2024-04-15T16:00:00Z");

        String result = TaxReturnService.getFormattedSystemTimestampForOffset(0, instant);

        assertThat(result).isEqualTo("2024-04-15T16:00:00Z");
    }

    @Test
    void givenPositiveTimezoneOffset_whenGetFormattedSystemTimestamp_thenFormatsAheadOfUtc() {
        // Abu Dhabi is UTC+4 => JS offset is -240
        Instant instant = Instant.parse("2024-04-15T12:00:00Z");

        String result = TaxReturnService.getFormattedSystemTimestampForOffset(-240, instant);

        // UTC+4 means 12:00 UTC -> 16:00 local
        assertThat(result).isEqualTo("2024-04-15T16:00:00+04:00");
    }

    // --- getLatestSubmissionEventByTaxReturnIdPreferringAcceptedSubmission ---

    @Test
    void givenAcceptedAndRejectedEvents_whenGetLatestPreferringAccepted_thenReturnsAccepted() {
        TaxReturn taxReturn = TaxReturn.testObjectFactory();
        TaxReturnSubmission submission = TaxReturnSubmission.testObjectFactory(taxReturn);

        SubmissionEvent acceptedEvent = submission.addSubmissionEvent(SubmissionEventTypeEnum.ACCEPTED);
        submission.addSubmissionEvent(SubmissionEventTypeEnum.REJECTED);

        when(taxReturnSubmissionRepo.findLatestTaxReturnSubmissionByTaxReturnId(TAX_RETURN_ID))
                .thenReturn(Optional.of(submission));

        SubmissionEvent result = taxReturnService
                .getLatestSubmissionEventByTaxReturnIdPreferringAcceptedSubmission(TAX_RETURN_ID);

        assertThat(result.getEventType()).isEqualTo(SubmissionEventTypeEnum.ACCEPTED);
    }

    @Test
    void givenNoSubmission_whenGetLatestPreferringAccepted_thenReturnsNull() {
        when(taxReturnSubmissionRepo.findLatestTaxReturnSubmissionByTaxReturnId(TAX_RETURN_ID))
                .thenReturn(Optional.empty());

        SubmissionEvent result = taxReturnService
                .getLatestSubmissionEventByTaxReturnIdPreferringAcceptedSubmission(TAX_RETURN_ID);

        assertNull(result);
    }

    @Test
    void givenOnlyRejectedEvent_whenGetLatestPreferringAccepted_thenReturnsRejected() {
        TaxReturn taxReturn = TaxReturn.testObjectFactory();
        TaxReturnSubmission submission = TaxReturnSubmission.testObjectFactory(taxReturn);
        submission.addSubmissionEvent(SubmissionEventTypeEnum.REJECTED);

        when(taxReturnSubmissionRepo.findLatestTaxReturnSubmissionByTaxReturnId(TAX_RETURN_ID))
                .thenReturn(Optional.of(submission));

        SubmissionEvent result = taxReturnService
                .getLatestSubmissionEventByTaxReturnIdPreferringAcceptedSubmission(TAX_RETURN_ID);

        assertThat(result.getEventType()).isEqualTo(SubmissionEventTypeEnum.REJECTED);
    }

    // --- getLatestSubmissionEventByTaxReturnId ---

    @Test
    void givenSubmissionEventExists_whenGetLatestByTaxReturnId_thenReturnsEvent() {
        SubmissionEvent event = SubmissionEvent.testObjectFactory(SubmissionEventTypeEnum.PROCESSING);
        when(submissionEventRepository.getLatestSubmissionEventByTaxReturnId(TAX_RETURN_ID))
                .thenReturn(Optional.of(event));

        SubmissionEvent result = taxReturnService.getLatestSubmissionEventByTaxReturnId(TAX_RETURN_ID);

        assertNotNull(result);
        assertThat(result.getEventType()).isEqualTo(SubmissionEventTypeEnum.PROCESSING);
    }

    @Test
    void givenNoSubmissionEvent_whenGetLatestByTaxReturnId_thenReturnsNull() {
        when(submissionEventRepository.getLatestSubmissionEventByTaxReturnId(TAX_RETURN_ID))
                .thenReturn(Optional.empty());

        SubmissionEvent result = taxReturnService.getLatestSubmissionEventByTaxReturnId(TAX_RETURN_ID);

        assertNull(result);
    }

    // --- findTaxReturnSubmissionsForAPIResponse ---

    @Test
    void givenTaxReturnId_whenFindSubmissionsForAPIResponse_thenDelegatesToRepository() {
        List<TaxReturnSubmission> expected = List.of(TaxReturnSubmission.testObjectFactory());
        when(taxReturnSubmissionRepo.findAllTaxReturnSubmissionsByTaxReturnId(TAX_RETURN_ID)).thenReturn(expected);

        List<TaxReturnSubmission> result = taxReturnService.findTaxReturnSubmissionsForAPIResponse(TAX_RETURN_ID);

        assertThat(result).isEqualTo(expected);
        verify(taxReturnSubmissionRepo).findAllTaxReturnSubmissionsByTaxReturnId(TAX_RETURN_ID);
    }

    // --- isResetting ---

    @Test
    void givenOnlyEmailFact_whenIsResetting_thenReturnsTrue() {
        Map<String, FactTypeWithItem> facts = Map.of("/email", new FactTypeWithItem("type", null));

        boolean result = taxReturnService.isResetting(facts);

        assertTrue(result);
    }

    @Test
    void givenMultipleFacts_whenIsResetting_thenReturnsFalse() {
        Map<String, FactTypeWithItem> facts = Map.of(
                "/email", new FactTypeWithItem("type", null),
                "/other", new FactTypeWithItem("type", null));

        boolean result = taxReturnService.isResetting(facts);

        assertFalse(result);
    }

    @Test
    void givenNoEmailFact_whenIsResetting_thenReturnsFalse() {
        Map<String, FactTypeWithItem> facts = Map.of("/other", new FactTypeWithItem("type", null));

        boolean result = taxReturnService.isResetting(facts);

        assertFalse(result);
    }
}
