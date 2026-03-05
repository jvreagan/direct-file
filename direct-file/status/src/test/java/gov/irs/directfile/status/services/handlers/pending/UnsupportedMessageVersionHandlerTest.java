package gov.irs.directfile.status.services.handlers.pending;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import gov.irs.directfile.models.message.MessageHeaderAttribute;
import gov.irs.directfile.models.message.QueueMessageHeaders;
import gov.irs.directfile.models.message.pending.VersionedPendingSubmissionMessage;
import gov.irs.directfile.models.message.pending.payload.AbstractPendingSubmissionPayload;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnsupportedMessageVersionHandlerTest {

    private UnsupportedMessageVersionHandler handler;

    @Mock
    private VersionedPendingSubmissionMessage<AbstractPendingSubmissionPayload> message;

    @BeforeEach
    void setUp() {
        handler = new UnsupportedMessageVersionHandler();
    }

    @Test
    void givenMessage_whenHandle_thenNoException() {
        QueueMessageHeaders headers =
                new QueueMessageHeaders().addHeader(MessageHeaderAttribute.VERSION, "UNSUPPORTED");
        when(message.getHeaders()).thenReturn(headers);

        assertDoesNotThrow(() -> handler.handlePendingSubmissionMessage(message));
    }

    @Test
    void givenMessageWithHeaders_whenHandle_thenLogsAndCompletes() {
        QueueMessageHeaders headers = new QueueMessageHeaders().addHeader(MessageHeaderAttribute.VERSION, "V99");
        when(message.getHeaders()).thenReturn(headers);

        // Should log error but not throw
        handler.handlePendingSubmissionMessage(message);
    }
}
