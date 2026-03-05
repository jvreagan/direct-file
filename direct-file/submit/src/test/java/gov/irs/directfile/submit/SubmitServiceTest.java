package gov.irs.directfile.submit;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import gov.irs.directfile.submit.config.Config;
import gov.irs.directfile.submit.config.DirectoriesConfig;
import gov.irs.directfile.submit.config.MessageQueueConfig;
import gov.irs.directfile.submit.service.SqsConnectionSetupService;
import gov.irs.directfile.submit.service.UserSubmissionConsumer;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmitServiceTest {

    @Mock
    private SqsConnectionSetupService sqsConnectionSetupService;

    @Mock
    private UserSubmissionConsumer userSubmissionConsumer;

    @Mock
    private Config config;

    @Mock
    private DirectoriesConfig directoriesConfig;

    @Mock
    private MessageQueueConfig messageQueueConfig;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // Set up directories config to return temp paths that already exist,
        // so DirectoryCreator.CreateDirectories() succeeds
        when(config.getDirectories()).thenReturn(directoriesConfig);
        when(directoriesConfig.getInput()).thenReturn(tempDir.resolve("input").toString());
        when(directoriesConfig.getToProcess())
                .thenReturn(tempDir.resolve("toProcess").toString());
        when(directoriesConfig.getProcessed())
                .thenReturn(tempDir.resolve("processed").toString());
        when(directoriesConfig.getToBatch())
                .thenReturn(tempDir.resolve("toBatch").toString());
        when(directoriesConfig.getBatched())
                .thenReturn(tempDir.resolve("batched").toString());
        when(directoriesConfig.getSubmitted())
                .thenReturn(tempDir.resolve("submitted").toString());
    }

    @Test
    void givenTestMode_whenSetup_thenSkipsSqsSetup() throws Exception {
        // given
        when(config.isRunnerDisabledForTesting()).thenReturn(true);

        SubmitService submitService = new SubmitService(config, sqsConnectionSetupService, userSubmissionConsumer);

        // when
        submitService.setup();

        // then: SQS setup should not be invoked
        verifyNoInteractions(sqsConnectionSetupService);
    }

    @Test
    void givenSqsEnabled_whenSetup_thenSetupsSqsConnection() throws Exception {
        // given
        when(config.isRunnerDisabledForTesting()).thenReturn(false);
        when(config.getMessageQueue()).thenReturn(messageQueueConfig);
        when(messageQueueConfig.isSqsMessageHandlingEnabled()).thenReturn(true);

        SubmitService submitService = new SubmitService(config, sqsConnectionSetupService, userSubmissionConsumer);

        // when
        submitService.setup();

        // then
        verify(sqsConnectionSetupService).setup(userSubmissionConsumer);
    }
}
