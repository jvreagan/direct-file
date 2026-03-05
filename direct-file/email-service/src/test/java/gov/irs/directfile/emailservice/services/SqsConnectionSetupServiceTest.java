package gov.irs.directfile.emailservice.services;

import com.amazon.sqs.javamessaging.SQSConnection;
import com.amazon.sqs.javamessaging.SQSConnectionFactory;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;

import gov.irs.directfile.emailservice.config.EmailServiceConfigurationProperties;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SqsConnectionSetupServiceTest {

    @Mock
    private EmailServiceConfigurationProperties configProps;

    @Mock
    private SqsClient sqsClient;

    @Mock
    private EmailRecordKeepingService emailRecordKeepingService;

    @Mock
    private ISendService sender;

    @Test
    void givenValidConfig_whenSetup_thenCreatesConnectionAndStarts() throws Exception {
        EmailServiceConfigurationProperties.MessageQueue messageQueue =
                mock(EmailServiceConfigurationProperties.MessageQueue.class);
        when(configProps.getMessageQueue()).thenReturn(messageQueue);
        when(messageQueue.getSendEmailQueue()).thenReturn("test-queue");
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(GetQueueUrlResponse.builder()
                        .queueUrl("https://sqs.us-east-1.amazonaws.com/123456789/test-queue")
                        .build());

        try (MockedConstruction<SQSConnectionFactory> mockedFactory =
                mockConstruction(SQSConnectionFactory.class, (factory, context) -> {
                    SQSConnection mockConnection = mock(SQSConnection.class);
                    Session mockSession = mock(Session.class);
                    when(factory.createConnection()).thenReturn(mockConnection);
                    when(mockConnection.createSession(anyInt())).thenReturn(mockSession);
                    when(mockSession.createQueue(anyString())).thenReturn(mock(Queue.class));
                    when(mockSession.createConsumer(any())).thenReturn(mock(MessageConsumer.class));
                })) {

            SqsConnectionSetupService service =
                    new SqsConnectionSetupService(configProps, sqsClient, emailRecordKeepingService, sender);

            SQSConnectionFactory createdFactory = mockedFactory.constructed().get(0);
            verify(createdFactory).createConnection();
        }
    }

    @Test
    void givenNullConnection_whenCleanup_thenNoException() throws Exception {
        EmailServiceConfigurationProperties.MessageQueue messageQueue =
                mock(EmailServiceConfigurationProperties.MessageQueue.class);
        when(configProps.getMessageQueue()).thenReturn(messageQueue);
        when(messageQueue.getSendEmailQueue()).thenReturn("test-queue");
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(GetQueueUrlResponse.builder()
                        .queueUrl("https://sqs.us-east-1.amazonaws.com/123456789/test-queue")
                        .build());

        try (MockedConstruction<SQSConnectionFactory> mockedFactory =
                mockConstruction(SQSConnectionFactory.class, (factory, context) -> {
                    SQSConnection mockConnection = mock(SQSConnection.class);
                    Session mockSession = mock(Session.class);
                    when(factory.createConnection()).thenReturn(mockConnection);
                    when(mockConnection.createSession(anyInt())).thenReturn(mockSession);
                    when(mockSession.createQueue(anyString())).thenReturn(mock(Queue.class));
                    when(mockSession.createConsumer(any())).thenReturn(mock(MessageConsumer.class));
                })) {

            SqsConnectionSetupService service =
                    new SqsConnectionSetupService(configProps, sqsClient, emailRecordKeepingService, sender);

            // Set the connection to null to test cleanup with no active connection
            ReflectionTestUtils.setField(service, "connection", null);

            // Invoke the private cleanup method via reflection
            assertThatNoException().isThrownBy(() -> ReflectionTestUtils.invokeMethod(service, "cleanup"));
        }
    }
}
