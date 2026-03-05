package gov.irs.directfile.status.services.handlers.confirmation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import gov.irs.directfile.models.message.MessageHeaderAttribute;
import gov.irs.directfile.models.message.QueueMessageHeaders;
import gov.irs.directfile.models.message.confirmation.VersionedSubmissionConfirmationMessage;
import gov.irs.directfile.models.message.confirmation.payload.AbstractSubmissionConfirmationPayload;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnsupportedMessageVersionHandlerTest {

    private UnsupportedMessageVersionHandler handler;

    @Mock
    private VersionedSubmissionConfirmationMessage<AbstractSubmissionConfirmationPayload> message;

    @BeforeEach
    void setUp() {
        handler = new UnsupportedMessageVersionHandler();
    }

    @Test
    void givenMessage_whenHandle_thenNoException() {
        QueueMessageHeaders headers =
                new QueueMessageHeaders().addHeader(MessageHeaderAttribute.VERSION, "UNSUPPORTED");
        when(message.getHeaders()).thenReturn(headers);

        assertDoesNotThrow(() -> handler.handleSubmissionConfirmationMessage(message));
    }

    @Test
    void givenMessageWithHeaders_whenHandle_thenProcessesWithoutError() {
        QueueMessageHeaders headers = new QueueMessageHeaders().addHeader(MessageHeaderAttribute.VERSION, "V99");
        when(message.getHeaders()).thenReturn(headers);

        // Should log error but not throw
        handler.handleSubmissionConfirmationMessage(message);
    }
}
