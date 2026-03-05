package gov.irs.directfile.status.mef.client;

import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import gov.irs.mef.exception.ServiceException;
import gov.irs.mef.exception.ToolkitException;
import gov.irs.mef.services.ServiceContext;
import gov.irs.mef.services.transmitter.mtom.GetAcksMTOMClient;
import gov.irs.mef.services.transmitter.mtom.GetAcksResult;

import gov.irs.directfile.status.domain.GetAcksResultWrapper;

@Slf4j
@Service
public class MeFAcksMTOMClientService {
    private final GetAcksMTOMClient getAcksMTOMClient;

    public MeFAcksMTOMClientService(GetAcksMTOMClient getAcksMTOMClient) {
        this.getAcksMTOMClient = getAcksMTOMClient;
    }

    public GetAcksResultWrapper getAcks(ServiceContext serviceContext, Set<String> submissionIds)
            throws ServiceException, ToolkitException {
        log.info("Getting acknowledgements from MeF for {} submissions", submissionIds.size());
        GetAcksResult result = getAcksMTOMClient.invoke(serviceContext, submissionIds);
        return new GetAcksResultWrapper(result);
    }
}
