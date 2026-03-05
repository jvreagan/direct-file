package gov.irs.directfile.api.stateapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import gov.irs.factgraph.Graph;

import gov.irs.directfile.api.config.StateApiEndpointProperties;
import gov.irs.directfile.api.loaders.errors.FactGraphSaveException;
import gov.irs.directfile.api.loaders.service.FactGraphService;
import gov.irs.directfile.api.stateapi.domain.export.ExportableFacts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StateApiServiceTest {

    @Mock
    private FactGraphService factGraphService;

    @Mock
    private StateApiEndpointProperties stateApiEndpointProperties;

    private StateApiService stateApiService;

    @BeforeEach
    void setUp() {
        when(stateApiEndpointProperties.getBaseUrl()).thenReturn("http://localhost:8080");
        stateApiService = new StateApiService(factGraphService, stateApiEndpointProperties);
    }

    @Test
    void givenValidConfig_whenConstructed_thenNoException() {
        assertDoesNotThrow(() -> {
            when(stateApiEndpointProperties.getBaseUrl()).thenReturn("http://localhost:9999");
            new StateApiService(factGraphService, stateApiEndpointProperties);
        });
    }

    @Test
    void givenGraph_whenGetExportToStateFacts_thenReturnsExportableFacts(@Mock Graph graph)
            throws JsonProcessingException, FactGraphSaveException {
        ExportableFacts result = stateApiService.getExportToStateFacts(graph);

        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(ExportableFacts.class);
        assertThat(result).isEmpty();
    }

    @Test
    void givenNullGraph_whenGetExportToStateFacts_thenReturnsExportableFacts()
            throws JsonProcessingException, FactGraphSaveException {
        ExportableFacts result = stateApiService.getExportToStateFacts(null);

        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(ExportableFacts.class);
        assertThat(result).isEmpty();
    }
}
