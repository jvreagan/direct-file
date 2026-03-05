package gov.irs.directfile.api.taxreturn.submissions;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SqsException;

import gov.irs.directfile.api.config.MessageQueueConfigurationProperties;
import gov.irs.directfile.models.email.HtmlTemplate;
import gov.irs.directfile.models.message.SendEmailQueueMessageBody;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SendEmailQueueServiceTest {

    @Mock
    private SqsClient sqsClient;

    @Mock
    private MessageQueueConfigurationProperties messageQueueConfigurationProperties;

    @Captor
    private ArgumentCaptor<SendMessageRequest> sendMessageRequestCaptor;

    private SendEmailQueueService sendEmailQueueService;

    private static final String QUEUE_NAME = "send-email-queue";
    private static final String QUEUE_URL = "https://sqs.us-east-1.amazonaws.com/123456789/send-email-queue";

    @BeforeEach
    void setUp() {
        when(messageQueueConfigurationProperties.getSendEmailQueue()).thenReturn(QUEUE_NAME);
        sendEmailQueueService = new SendEmailQueueService(sqsClient, messageQueueConfigurationProperties);
    }

    private Map<HtmlTemplate, List<SendEmailQueueMessageBody>> createTestEmailMessages() {
        SendEmailQueueMessageBody messageBody = new SendEmailQueueMessageBody();
        return Map.of(HtmlTemplate.values()[0], List.of(messageBody));
    }

    private void stubQueueUrlResolution() {
        GetQueueUrlResponse queueUrlResponse =
                GetQueueUrlResponse.builder().queueUrl(QUEUE_URL).build();
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class))).thenReturn(queueUrlResponse);
    }

    @Test
    void givenValidMessage_whenEnqueue_thenSendsToSqs() {
        stubQueueUrlResolution();
        Map<HtmlTemplate, List<SendEmailQueueMessageBody>> emailMessages = createTestEmailMessages();

        sendEmailQueueService.enqueue(emailMessages);

        verify(sqsClient).sendMessage(sendMessageRequestCaptor.capture());
        SendMessageRequest capturedRequest = sendMessageRequestCaptor.getValue();
        assertThat(capturedRequest.queueUrl()).isEqualTo(QUEUE_URL);
        assertThat(capturedRequest.messageBody()).isNotBlank();
    }

    @Test
    void givenBlankQueueUrl_whenEnqueue_thenResolvesQueueUrl() {
        stubQueueUrlResolution();
        Map<HtmlTemplate, List<SendEmailQueueMessageBody>> emailMessages = createTestEmailMessages();

        sendEmailQueueService.enqueue(emailMessages);

        verify(sqsClient).getQueueUrl(any(GetQueueUrlRequest.class));
        verify(sqsClient).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void givenCachedQueueUrl_whenEnqueueCalledTwice_thenGetQueueUrlCalledOnce() {
        stubQueueUrlResolution();
        Map<HtmlTemplate, List<SendEmailQueueMessageBody>> emailMessages = createTestEmailMessages();

        sendEmailQueueService.enqueue(emailMessages);
        sendEmailQueueService.enqueue(emailMessages);

        verify(sqsClient, times(1)).getQueueUrl(any(GetQueueUrlRequest.class));
        verify(sqsClient, times(2)).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void givenSqsFailure_whenEnqueue_thenExceptionHandled() {
        stubQueueUrlResolution();
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenThrow(SqsException.builder().message("SQS unavailable").build());
        Map<HtmlTemplate, List<SendEmailQueueMessageBody>> emailMessages = createTestEmailMessages();

        assertThatNoException().isThrownBy(() -> sendEmailQueueService.enqueue(emailMessages));
    }
}
