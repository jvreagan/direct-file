package gov.irs.directfile.api.taxreturn.submissions;

import jakarta.jms.JMSException;
import jakarta.jms.TextMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import gov.irs.directfile.api.config.MessageQueueConfigurationProperties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfirmationQueueListenerServiceTest {

    @Mock
    private SubmissionConfirmationMessageRouter submissionConfirmationMessageRouter;

    @Mock
    private MessageQueueConfigurationProperties messageQueueConfigurationProperties;

    private ConfirmationQueueListenerService confirmationQueueListenerService;

    @BeforeEach
    void setUp() {
        when(messageQueueConfigurationProperties.getSubmissionConfirmationQueue())
                .thenReturn("test-confirmation-queue");
        confirmationQueueListenerService = new ConfirmationQueueListenerService(
                messageQueueConfigurationProperties, submissionConfirmationMessageRouter);
    }

    @Test
    void givenValidMessage_whenOnMessage_thenRoutesAndAcknowledges() throws JMSException {
        // given
        String validJson =
                """
                {
                  "payload": {
                    "@type": "SubmissionConfirmationPayloadV1",
                    "submissionIdToTaxReturnIdMap": {}
                  },
                  "headers": {
                    "headers": {
                      "VERSION": "V1"
                    }
                  }
                }
                """;
        TextMessage mockMessage = mock(TextMessage.class);
        when(mockMessage.getText()).thenReturn(validJson);

        // when
        assertDoesNotThrow(() -> confirmationQueueListenerService.onMessage(mockMessage));

        // then
        verify(submissionConfirmationMessageRouter, times(1)).handleSubmissionConfirmationMessage(any());
        verify(mockMessage, times(1)).acknowledge();
    }

    @Test
    void givenInvalidJson_whenOnMessage_thenLogsErrorNoException() throws JMSException {
        // given
        TextMessage mockMessage = mock(TextMessage.class);
        when(mockMessage.getText()).thenReturn("{invalid json content}}}}");

        // when - should not throw; error is caught and logged
        assertDoesNotThrow(() -> confirmationQueueListenerService.onMessage(mockMessage));

        // then - acknowledge should NOT be called since parsing failed
        verify(mockMessage, never()).acknowledge();
        verify(submissionConfirmationMessageRouter, never()).handleSubmissionConfirmationMessage(any());
    }

    @Test
    void givenJmsException_whenOnMessage_thenLogsError() throws JMSException {
        // given
        TextMessage mockMessage = mock(TextMessage.class);
        when(mockMessage.getText()).thenThrow(new JMSException("connection lost"));

        // when - should not throw
        assertDoesNotThrow(() -> confirmationQueueListenerService.onMessage(mockMessage));

        // then
        verify(submissionConfirmationMessageRouter, never()).handleSubmissionConfirmationMessage(any());
        verify(mockMessage, never()).acknowledge();
    }
}
