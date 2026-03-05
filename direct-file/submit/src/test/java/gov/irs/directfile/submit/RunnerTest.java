package gov.irs.directfile.submit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import gov.irs.directfile.submit.command.Action;
import gov.irs.directfile.submit.command.ActionType;
import gov.irs.directfile.submit.domain.ActionQueue;
import gov.irs.directfile.submit.service.ActionHandler;
import gov.irs.directfile.submit.service.OfflineModeService;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RunnerTest {

    @Mock
    private ActionHandler actionHandler;

    private OfflineModeService offlineModeService;
    private ActionQueue actionQueue;
    private Runner runner;

    @BeforeEach
    void setUp() {
        offlineModeService = new OfflineModeService();
        actionQueue = new ActionQueue();
        runner = new Runner(actionQueue, actionHandler, offlineModeService);
    }

    @Test
    void givenOfflineMode_whenStep_thenNoActionsTaken() throws InterruptedException {
        // given: offline mode is enabled
        offlineModeService.enableOfflineMode();

        // Add an action to newActions to verify it is NOT consumed
        Action mockAction = createMockAction();
        actionQueue.getNewActions().add(mockAction);

        // when
        runner.step();

        // then: actionHandler should never be invoked
        verifyNoInteractions(actionHandler);
    }

    @Test
    void givenNewActionsAvailable_whenStep_thenHandlesNewAction() throws InterruptedException {
        // given: inProgressActions is empty, newActions has an action
        Action mockAction = createMockAction();
        actionQueue.getNewActions().add(mockAction);

        // when
        runner.step();

        // then
        verify(actionHandler).handleAction(mockAction);
    }

    @Test
    void givenInProgressActionsAvailable_whenStep_thenHandlesInProgressAction() throws InterruptedException {
        // given: inProgressActions has an action
        Action inProgressAction = createMockAction();
        actionQueue.getInProgressActions().add(inProgressAction);

        // Also add a new action to verify it is NOT consumed when in-progress exists
        Action newAction = createMockAction();
        actionQueue.getNewActions().add(newAction);

        // when
        runner.step();

        // then: the in-progress action should be handled, not the new action
        verify(actionHandler).handleAction(inProgressAction);
        verify(actionHandler, never()).handleAction(newAction);
    }

    private Action createMockAction() {
        return new Action() {
            @Override
            public ActionType getType() {
                return ActionType.CLEANUP;
            }
        };
    }
}
