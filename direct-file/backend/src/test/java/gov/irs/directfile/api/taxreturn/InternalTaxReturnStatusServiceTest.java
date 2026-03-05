package gov.irs.directfile.api.taxreturn;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import gov.irs.directfile.api.audit.AuditLogElement;
import gov.irs.directfile.api.audit.AuditService;
import gov.irs.directfile.api.io.documentstore.S3StorageService;
import gov.irs.directfile.api.taxreturn.dto.Status;
import gov.irs.directfile.api.taxreturn.models.SubmissionEvent;
import gov.irs.directfile.api.taxreturn.models.TaxReturnSubmission;
import gov.irs.directfile.models.TaxReturnStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalTaxReturnStatusServiceTest {

    @Mock
    private TaxReturnService taxReturnService;

    @Mock
    private S3StorageService s3StorageService;

    @Mock
    private AuditService auditService;

    private InternalTaxReturnStatusService internalTaxReturnStatusService;

    private static final int TAX_FILING_YEAR = 2024;
    private static final UUID TAX_RETURN_ID = UUID.randomUUID();
    private static final String SUBMISSION_ID = "submission-123";

    @BeforeEach
    void setUp() {
        internalTaxReturnStatusService =
                new InternalTaxReturnStatusService(taxReturnService, s3StorageService, auditService);
    }

    @Test
    void givenValidSubmission_whenGetStatus_thenReturnsCorrectStatus() {
        SubmissionEvent submissionEvent = mock(SubmissionEvent.class);
        TaxReturnSubmission taxReturnSubmission = mock(TaxReturnSubmission.class);
        when(submissionEvent.getSubmission()).thenReturn(taxReturnSubmission);
        when(taxReturnSubmission.getSubmissionId()).thenReturn(SUBMISSION_ID);
        when(submissionEvent.getStatus()).thenReturn(Status.Accepted);
        when(taxReturnService.getLatestSubmissionEventByTaxReturnIdPreferringAcceptedSubmission(TAX_RETURN_ID))
                .thenReturn(submissionEvent);
        when(s3StorageService.doesObjectAlreadyExist(anyString())).thenReturn(true);

        TaxReturnStatus result = internalTaxReturnStatusService.getTaxReturnStatusInternal(
                TAX_FILING_YEAR, TAX_RETURN_ID, SUBMISSION_ID);

        assertThat(result.status()).isEqualTo(Status.Accepted.toString());
        assertThat(result.exists()).isTrue();
        verify(auditService).addEventProperty(AuditLogElement.MEF_SUBMISSION_ID, SUBMISSION_ID);
        verify(auditService).addEventProperty(AuditLogElement.TAX_PERIOD, String.valueOf(TAX_FILING_YEAR));
    }

    @Test
    void givenS3ObjectNotFound_whenGetStatus_thenReturnsFalseExists() {
        SubmissionEvent submissionEvent = mock(SubmissionEvent.class);
        TaxReturnSubmission taxReturnSubmission = mock(TaxReturnSubmission.class);
        when(submissionEvent.getSubmission()).thenReturn(taxReturnSubmission);
        when(taxReturnSubmission.getSubmissionId()).thenReturn(SUBMISSION_ID);
        when(submissionEvent.getStatus()).thenReturn(Status.Pending);
        when(taxReturnService.getLatestSubmissionEventByTaxReturnIdPreferringAcceptedSubmission(TAX_RETURN_ID))
                .thenReturn(submissionEvent);
        when(s3StorageService.doesObjectAlreadyExist(anyString())).thenReturn(false);

        TaxReturnStatus result = internalTaxReturnStatusService.getTaxReturnStatusInternal(
                TAX_FILING_YEAR, TAX_RETURN_ID, SUBMISSION_ID);

        assertThat(result.status()).isEqualTo(Status.Pending.toString());
        assertThat(result.exists()).isFalse();
    }

    @Test
    void givenException_whenGetStatus_thenReturnsErrorStatus() {
        when(taxReturnService.getLatestSubmissionEventByTaxReturnIdPreferringAcceptedSubmission(TAX_RETURN_ID))
                .thenThrow(new RuntimeException("Database error"));

        TaxReturnStatus result = internalTaxReturnStatusService.getTaxReturnStatusInternal(
                TAX_FILING_YEAR, TAX_RETURN_ID, SUBMISSION_ID);

        assertThat(result.status()).isEqualTo(Status.Error.name());
        assertThat(result.exists()).isFalse();
    }
}
