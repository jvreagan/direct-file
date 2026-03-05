package gov.irs.directfile.submit.actions;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import gov.irs.directfile.submit.actions.results.CreateArchiveActionResult;
import gov.irs.directfile.submit.command.CreateArchiveAction;
import gov.irs.directfile.submit.domain.SubmissionBatch;
import gov.irs.directfile.submit.service.interfaces.ISynchronousDocumentStoreService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateArchiveActionHandlerTest {

    @Mock
    private ISynchronousDocumentStoreService storageService;

    @Mock
    private ActionContext actionContext;

    private CreateArchiveActionHandler createArchiveActionHandler;

    @BeforeEach
    void setUp() {
        createArchiveActionHandler = new CreateArchiveActionHandler(actionContext, storageService);
    }

    @Test
    void givenNoSubmissions_whenHandleCommand_thenReturnsEmptyArchives() throws ActionException {
        // given
        SubmissionBatch batch = new SubmissionBatch(0L, "env/batches/0");
        CreateArchiveAction action = new CreateArchiveAction(batch);
        when(storageService.getSubFolders("env/batches/0")).thenReturn(Collections.emptyList());

        // when
        CreateArchiveActionResult result = createArchiveActionHandler.handleCommand(action);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getSubmissionArchiveContainers()).isEmpty();
        assertThat(result.getBatch()).isEqualTo(batch);
    }

    @Test
    void givenStorageException_whenHandleCommand_thenThrowsCreateArchiveActionException() {
        // given
        SubmissionBatch batch = new SubmissionBatch(0L, "env/batches/0");
        CreateArchiveAction action = new CreateArchiveAction(batch);
        when(storageService.getSubFolders("env/batches/0")).thenThrow(new RuntimeException("S3 connection failed"));

        // when / then
        assertThatThrownBy(() -> createArchiveActionHandler.handleCommand(action))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("S3 connection failed");
    }
}
