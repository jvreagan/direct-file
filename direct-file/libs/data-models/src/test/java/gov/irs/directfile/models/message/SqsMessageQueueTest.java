package gov.irs.directfile.models.message;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SqsMessageQueueTest {
    private SqsMessageQueue sqsMessageQueue;

    @Mock
    private SqsClient sqsClient;

    @Mock
    private GetQueueUrlResponse getQueueUrlResponse;

    @Mock
    private SendMessageResponse sendMessageResponse;

    @Mock
    private SdkHttpResponse sdkHttpResponse;

    @Mock
    private DeleteMessageResponse deleteMessageResponse;

    private final String queueName = "test-queue";
    private final String queueUrl = "http://localhost:4566/000000000000/test-queue";

    @BeforeEach
    void setUp() {
        sqsMessageQueue = new SqsMessageQueue(sqsClient);
    }

    @Test
    void givenValidMessage_whenSend_thenSendsToCorrectQueue() {
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class))).thenReturn(getQueueUrlResponse);
        when(getQueueUrlResponse.queueUrl()).thenReturn(queueUrl);
        when(sqsClient.sendMessage(any(SendMessageRequest.class))).thenReturn(sendMessageResponse);
        when(sendMessageResponse.sdkHttpResponse()).thenReturn(sdkHttpResponse);
        when(sdkHttpResponse.isSuccessful()).thenReturn(true);

        sqsMessageQueue.send(queueName, "hello", Map.of("key", "value"));

        ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsClient).sendMessage(captor.capture());
        SendMessageRequest request = captor.getValue();
        assertEquals(queueUrl, request.queueUrl());
        assertEquals("hello", request.messageBody());
        assertEquals("value", request.messageAttributes().get("key").stringValue());
        assertEquals("String", request.messageAttributes().get("key").dataType());
    }

    @Test
    void givenQueueUrlAlreadyResolved_whenSendTwice_thenGetQueueUrlCalledOnce() {
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class))).thenReturn(getQueueUrlResponse);
        when(getQueueUrlResponse.queueUrl()).thenReturn(queueUrl);
        when(sqsClient.sendMessage(any(SendMessageRequest.class))).thenReturn(sendMessageResponse);
        when(sendMessageResponse.sdkHttpResponse()).thenReturn(sdkHttpResponse);
        when(sdkHttpResponse.isSuccessful()).thenReturn(true);

        sqsMessageQueue.send(queueName, "msg1", Map.of());
        sqsMessageQueue.send(queueName, "msg2", Map.of());

        verify(sqsClient, times(1)).getQueueUrl(any(GetQueueUrlRequest.class));
        verify(sqsClient, times(2)).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void givenSqsClientThrowsOnGetQueueUrl_whenSend_thenThrowsPublisherException() {
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenThrow(QueueDoesNotExistException.builder().build());

        assertThrows(PublisherException.class, () -> sqsMessageQueue.send(queueName, "hello", Map.of()));
    }

    @Test
    void givenSqsClientThrowsOnSendMessage_whenSend_thenThrowsPublisherException() {
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class))).thenReturn(getQueueUrlResponse);
        when(getQueueUrlResponse.queueUrl()).thenReturn(queueUrl);
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenThrow(InvalidMessageContentsException.builder().build());

        assertThrows(PublisherException.class, () -> sqsMessageQueue.send(queueName, "hello", Map.of()));
    }

    @Test
    void givenUnsuccessfulHttpResponse_whenSend_thenThrowsPublisherException() {
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class))).thenReturn(getQueueUrlResponse);
        when(getQueueUrlResponse.queueUrl()).thenReturn(queueUrl);
        when(sqsClient.sendMessage(any(SendMessageRequest.class))).thenReturn(sendMessageResponse);
        when(sendMessageResponse.sdkHttpResponse()).thenReturn(sdkHttpResponse);
        when(sdkHttpResponse.isSuccessful()).thenReturn(false);
        when(sdkHttpResponse.statusCode()).thenReturn(500);

        assertThrows(PublisherException.class, () -> sqsMessageQueue.send(queueName, "hello", Map.of()));
    }

    @Test
    void givenMessagesInQueue_whenReceive_thenReturnsMappedEnvelopes() {
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class))).thenReturn(getQueueUrlResponse);
        when(getQueueUrlResponse.queueUrl()).thenReturn(queueUrl);

        Message sqsMessage = Message.builder()
                .messageId("msg-123")
                .body("hello")
                .receiptHandle("receipt-123")
                .messageAttributes(Map.of(
                        "key",
                        MessageAttributeValue.builder()
                                .dataType("String")
                                .stringValue("value")
                                .build()))
                .build();

        ReceiveMessageResponse receiveResponse =
                ReceiveMessageResponse.builder().messages(sqsMessage).build();
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(receiveResponse);

        List<QueueMessageEnvelope> envelopes = sqsMessageQueue.receive(queueName, 10, 5);

        assertEquals(1, envelopes.size());
        assertEquals("msg-123", envelopes.get(0).messageId());
        assertEquals("hello", envelopes.get(0).body());
        assertEquals("receipt-123", envelopes.get(0).receiptHandle());
        assertEquals("value", envelopes.get(0).attributes().get("key"));
    }

    @Test
    void givenSqsClientThrowsOnReceive_whenReceive_thenThrowsPublisherException() {
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class))).thenReturn(getQueueUrlResponse);
        when(getQueueUrlResponse.queueUrl()).thenReturn(queueUrl);
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenThrow(SqsException.builder().message("error").build());

        assertThrows(PublisherException.class, () -> sqsMessageQueue.receive(queueName, 10, 5));
    }

    @Test
    void givenValidReceiptHandle_whenDelete_thenCallsDeleteMessage() {
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class))).thenReturn(getQueueUrlResponse);
        when(getQueueUrlResponse.queueUrl()).thenReturn(queueUrl);
        when(sqsClient.deleteMessage(any(DeleteMessageRequest.class))).thenReturn(deleteMessageResponse);

        sqsMessageQueue.delete(queueName, "receipt-123");

        ArgumentCaptor<DeleteMessageRequest> captor = ArgumentCaptor.forClass(DeleteMessageRequest.class);
        verify(sqsClient).deleteMessage(captor.capture());
        assertEquals(queueUrl, captor.getValue().queueUrl());
        assertEquals("receipt-123", captor.getValue().receiptHandle());
    }

    @Test
    void givenSqsClientThrowsOnDelete_whenDelete_thenThrowsPublisherException() {
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class))).thenReturn(getQueueUrlResponse);
        when(getQueueUrlResponse.queueUrl()).thenReturn(queueUrl);
        when(sqsClient.deleteMessage(any(DeleteMessageRequest.class)))
                .thenThrow(SqsException.builder().message("error").build());

        assertThrows(PublisherException.class, () -> sqsMessageQueue.delete(queueName, "receipt-123"));
    }
}
