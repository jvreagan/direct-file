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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DlqS3NotificationEventQueueListenerServiceTest {

    @Mock
    private MessageQueueConfigurationProperties messageQueueConfigurationProperties;

    private DlqS3NotificationEventQueueListenerService dlqListenerService;

    @BeforeEach
    void setUp() {
        when(messageQueueConfigurationProperties.getDlqS3NotificationEventQueue())
                .thenReturn("test-dlq-s3-notification-queue");
        when(messageQueueConfigurationProperties.getS3NotificationEventQueue())
                .thenReturn("test-s3-notification-queue");
        dlqListenerService = new DlqS3NotificationEventQueueListenerService(messageQueueConfigurationProperties);
    }

    @Test
    void givenValidMessage_whenOnMessage_thenLogsRawText() throws JMSException {
        // given
        TextMessage mockMessage = mock(TextMessage.class);
        when(mockMessage.getText()).thenReturn("some raw s3 notification event text");

        // when - should not throw; it just logs the raw text
        assertDoesNotThrow(() -> dlqListenerService.onMessage(mockMessage));

        // then - getText was invoked to read the message
        verify(mockMessage, times(1)).getText();
    }

    @Test
    void givenJmsException_whenOnMessage_thenLogsError() throws JMSException {
        // given
        TextMessage mockMessage = mock(TextMessage.class);
        when(mockMessage.getText()).thenThrow(new JMSException("failed to retrieve text"));

        // when - should not throw; the JMSException is caught and logged
        assertDoesNotThrow(() -> dlqListenerService.onMessage(mockMessage));

        // then
        verify(mockMessage, times(1)).getText();
    }
}
