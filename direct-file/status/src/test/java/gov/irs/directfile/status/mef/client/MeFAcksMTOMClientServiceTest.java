package gov.irs.directfile.status.mef.client;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import gov.irs.mef.AcknowledgementList;
import gov.irs.mef.services.ServiceContext;
import gov.irs.mef.services.transmitter.mtom.GetAcksMTOMClient;
import gov.irs.mef.services.transmitter.mtom.GetAcksResult;

import gov.irs.directfile.status.domain.GetAcksResultWrapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeFAcksMTOMClientServiceTest {
    private MeFAcksMTOMClientService acksMTOMClientService;

    @Mock
    private GetAcksMTOMClient getAcksMTOMClient;

    @Mock
    private ServiceContext serviceContext;

    @BeforeEach
    void setUp() {
        acksMTOMClientService = new MeFAcksMTOMClientService(getAcksMTOMClient);
    }

    @Test
    void givenSubmissionIds_whenGetAcks_thenDelegatesToClientAndWrapsResult() throws Exception {
        Set<String> submissionIds = Set.of("sub-1", "sub-2");
        GetAcksResult mockResult = new GetAcksResult();
        mockResult.setAcknowledgementList(new AcknowledgementList());
        when(getAcksMTOMClient.invoke(serviceContext, submissionIds)).thenReturn(mockResult);

        GetAcksResultWrapper result = acksMTOMClientService.getAcks(serviceContext, submissionIds);

        assertNotNull(result);
        verify(getAcksMTOMClient).invoke(serviceContext, submissionIds);
    }
}
