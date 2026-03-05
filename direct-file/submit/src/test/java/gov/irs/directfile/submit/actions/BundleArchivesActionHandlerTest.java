package gov.irs.directfile.submit.actions;

import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import gov.irs.mef.exception.ToolkitException;
import gov.irs.mef.inputcomposition.PostmarkedSubmissionArchive;
import gov.irs.mef.inputcomposition.SubmissionBuilder;
import gov.irs.mef.inputcomposition.SubmissionContainer;

import gov.irs.directfile.audit.events.TinType;
import gov.irs.directfile.submit.actions.exception.BundleArchiveActionException;
import gov.irs.directfile.submit.actions.results.BundleArchivesActionResult;
import gov.irs.directfile.submit.actions.results.CreateArchiveActionResult;
import gov.irs.directfile.submit.command.BundleArchiveAction;
import gov.irs.directfile.submit.config.Config;
import gov.irs.directfile.submit.config.SnsClientTestConfiguration;
import gov.irs.directfile.submit.config.SynchronousS3TestConfiguration;
import gov.irs.directfile.submit.domain.SubmissionArchiveContainer;
import gov.irs.directfile.submit.domain.SubmissionBatch;
import gov.irs.directfile.submit.domain.UserContextData;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;

@SpringBootTest
@Import({SynchronousS3TestConfiguration.class, SnsClientTestConfiguration.class})
public class BundleArchivesActionHandlerTest {

    // NOTE: Needed because we're mocking MEF Classes. MeF SDK expects this env variable A2A_TOOLKIT_HOME to be defined
    @BeforeAll
    public static void setupSystemProperties() {
        String userDirectory = System.getProperty("user.dir");
        System.setProperty("A2A_TOOLKIT_HOME", userDirectory + "/src/test/resources/");
    }

    @AfterAll
    public static void cleanupSystemProperties() {
        System.clearProperty("A2A_TOOLKIT_HOME");
    }

    @Autowired
    Config config;

    // default test data
    final UserContextData userContextData1 = new UserContextData(
            "00000",
            "00000000-0000-0000-0000-000000000000",
            "11111111-1111-1111-1111-111111111111",
            "111001111",
            TinType.INDIVIDUAL,
            "0.0.0.0",
            "2024-01-01");
    final UserContextData userContextData2 = new UserContextData(
            "11111",
            "88888888-8888-8888-8888-888888888888",
            "99999999-9999-9999-9999-999999999999",
            "111002222",
            TinType.INDIVIDUAL,
            "1.1.1.1",
            "2024-01-01");
    final PostmarkedSubmissionArchive mockSubmissionArchive1 = mock(PostmarkedSubmissionArchive.class);
    final PostmarkedSubmissionArchive mockSubmissionArchive2 = mock(PostmarkedSubmissionArchive.class);

    final List<SubmissionArchiveContainer> submissionArchiveContainers = List.of(
            new SubmissionArchiveContainer(userContextData1, mockSubmissionArchive1),
            new SubmissionArchiveContainer(userContextData2, mockSubmissionArchive2));

    @Test
    void bundleArchivesActionReturnsResultOnSuccess() throws ActionException {
        // given
        ActionContext actionContext = new ActionContext(config);
        SubmissionBatch submissionBatch = new SubmissionBatch(0L, "");
        CreateArchiveActionResult createArchiveActionResult =
                new CreateArchiveActionResult(submissionBatch, submissionArchiveContainers);
        BundleArchivesActionHandler bundleArchivesActionHandler = new BundleArchivesActionHandler(actionContext);

        SubmissionContainer mockContainer = mock(SubmissionContainer.class);

        try (MockedStatic<SubmissionBuilder> mockSubmissionBuilder = Mockito.mockStatic(SubmissionBuilder.class)) {
            mockSubmissionBuilder
                    .when(() -> SubmissionBuilder.createSubmissionContainer(
                            any(PostmarkedSubmissionArchive[].class), anyString()))
                    .thenReturn(mockContainer);

            // when
            BundleArchivesActionResult result =
                    bundleArchivesActionHandler.handleBundleCommand(new BundleArchiveAction(createArchiveActionResult));

            // then
            assertNotNull(result);
            assertEquals(submissionBatch, result.getBatch());
            assertNotNull(result.getBundledArchives());
            assertEquals(2, result.getBundledArchives().UserContexts.size());
        }
    }

    @Test
    void bundleArchivesActionThrowsBundleArchiveActionExceptionOnFailure() {
        // given
        ActionContext actionContext = new ActionContext(config);
        SubmissionBatch submissionBatch = new SubmissionBatch(0L, "");
        CreateArchiveActionResult createArchiveActionResult =
                new CreateArchiveActionResult(submissionBatch, submissionArchiveContainers);
        BundleArchivesActionHandler bundleArchivesActionHandler = new BundleArchivesActionHandler(actionContext);

        try (MockedStatic<SubmissionBuilder> mockSubmissionBuilder = Mockito.mockStatic(SubmissionBuilder.class)) {
            mockSubmissionBuilder
                    .when(() -> SubmissionBuilder.createSubmissionContainer(
                            any(PostmarkedSubmissionArchive[].class), anyString()))
                    .thenThrow(new ToolkitException("test toolkit exception"));

            // when / then
            BundleArchiveActionException thrown = assertThrows(
                    BundleArchiveActionException.class,
                    () -> bundleArchivesActionHandler.handleBundleCommand(
                            new BundleArchiveAction(createArchiveActionResult)));

            assertEquals(2, thrown.getUserContextDataList().size());
            assertEquals(submissionBatch, thrown.getBatch());
            assertInstanceOf(ToolkitException.class, thrown.getCause());
        }
    }
}
