package gov.irs.directfile.status.services;

import com.amazon.sqs.javamessaging.SQSConnection;
import com.amazon.sqs.javamessaging.SQSConnectionFactory;
import jakarta.jms.JMSException;
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

import gov.irs.directfile.status.config.MessageQueueConfiguration;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SqsConnectionSetupServiceTest {

    @Mock
    private SqsClient sqsClient;

    @Mock
    private MessageQueueConfiguration messageQueueConfiguration;

    @Mock
    private MessageQueueListenerService messageQueueListenerService;

    /**
     * Stubs the {@link SQSConnectionFactory} mock so the full JMS call chain
     * ({@code createConnection -> createSession -> createQueue -> createConsumer})
     * returns mocks. The returned {@link SQSConnection} can be used for additional
     * verification (e.g. {@code verify(conn).start()}).
     */
    private SQSConnection stubConnectionFactory(SQSConnectionFactory factory) throws JMSException {
        SQSConnection mockConnection = mock(SQSConnection.class);
        Session mockSession = mock(Session.class);

        when(factory.createConnection()).thenReturn(mockConnection);
        when(mockConnection.createSession(anyBoolean(), anyInt())).thenReturn(mockSession);
        when(mockSession.createQueue(anyString())).thenReturn(mock(Queue.class));
        when(mockSession.createConsumer(any())).thenReturn(mock(MessageConsumer.class));

        return mockConnection;
    }

    // ---- success path ----

    @Test
    void givenValidConfig_whenConstructed_thenCreatesConnectionAndStarts() throws Exception {
        when(messageQueueConfiguration.getPendingSubmissionQueue()).thenReturn("pending-submission-queue");

        // Capture the SQSConnection mock created during factory stubbing so we can
        // verify that start() was invoked on the exact same instance.
        final SQSConnection[] capturedConnection = new SQSConnection[1];

        try (MockedConstruction<SQSConnectionFactory> mockedFactory =
                mockConstruction(SQSConnectionFactory.class, (factory, context) -> {
                    capturedConnection[0] = stubConnectionFactory(factory);
                })) {

            SqsConnectionSetupService service =
                    new SqsConnectionSetupService(sqsClient, messageQueueConfiguration, messageQueueListenerService);

            // The factory was constructed exactly once
            SQSConnectionFactory createdFactory = mockedFactory.constructed().get(0);
            verify(createdFactory).createConnection();

            // The connection was started after the consumer was wired up
            verify(capturedConnection[0]).start();
        }
    }

    // ---- cleanup ----

    @Test
    void givenActiveConnection_whenCleanup_thenStopsAndClosesConnection() throws Exception {
        when(messageQueueConfiguration.getPendingSubmissionQueue()).thenReturn("pending-submission-queue");

        try (MockedConstruction<SQSConnectionFactory> mockedFactory =
                mockConstruction(SQSConnectionFactory.class, (factory, context) -> {
                    stubConnectionFactory(factory);
                })) {

            SqsConnectionSetupService service =
                    new SqsConnectionSetupService(sqsClient, messageQueueConfiguration, messageQueueListenerService);

            // Grab the SQSConnection that was stored in the service's private field
            SQSConnection connectionField = (SQSConnection) ReflectionTestUtils.getField(service, "connection");

            // Invoke the private @PreDestroy method
            ReflectionTestUtils.invokeMethod(service, "cleanup");

            // Verify stop() then close() were called on the connection
            verify(connectionField).stop();
            verify(connectionField).close();
        }
    }

    @Test
    void givenNullConnection_whenCleanup_thenNoException() throws Exception {
        when(messageQueueConfiguration.getPendingSubmissionQueue()).thenReturn("pending-submission-queue");

        try (MockedConstruction<SQSConnectionFactory> mockedFactory =
                mockConstruction(SQSConnectionFactory.class, (factory, context) -> {
                    stubConnectionFactory(factory);
                })) {

            SqsConnectionSetupService service =
                    new SqsConnectionSetupService(sqsClient, messageQueueConfiguration, messageQueueListenerService);

            // Force the connection field to null to simulate an uninitialised state
            ReflectionTestUtils.setField(service, "connection", null);

            assertThatNoException().isThrownBy(() -> ReflectionTestUtils.invokeMethod(service, "cleanup"));
        }
    }

    // ---- JMSException handling ----

    @Test
    void givenFactoryThrowsJMSException_whenConstructed_thenExceptionPropagates() {
        when(messageQueueConfiguration.getPendingSubmissionQueue()).thenReturn("pending-submission-queue");

        try (MockedConstruction<SQSConnectionFactory> mockedFactory =
                mockConstruction(SQSConnectionFactory.class, (factory, context) -> {
                    when(factory.createConnection()).thenThrow(new JMSException("connection refused"));
                })) {

            assertThatThrownBy(() ->
                    new SqsConnectionSetupService(sqsClient, messageQueueConfiguration, messageQueueListenerService))
                    .isInstanceOf(JMSException.class)
                    .hasMessageContaining("connection refused");
        }
    }
}
