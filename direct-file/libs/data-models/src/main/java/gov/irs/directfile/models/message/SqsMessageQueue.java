package gov.irs.directfile.models.message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

@Slf4j
public class SqsMessageQueue implements MessageQueue {
    private final SqsClient sqsClient;
    private final Map<String, String> queueUrlCache = new HashMap<>();

    public SqsMessageQueue(SqsClient sqsClient) {
        this.sqsClient = sqsClient;
    }

    private String resolveQueueUrl(String queueName) {
        return queueUrlCache.computeIfAbsent(queueName, name -> {
            try {
                GetQueueUrlResponse response = sqsClient.getQueueUrl(
                        GetQueueUrlRequest.builder().queueName(name).build());
                log.info("Resolved SQS queue URL for: {}", name);
                return response.queueUrl();
            } catch (Exception e) {
                throw new PublisherException("Failed to resolve queue URL for: " + name, e);
            }
        });
    }

    @Override
    public void send(String queueName, String message, Map<String, String> attributes) {
        String queueUrl = resolveQueueUrl(queueName);
        Map<String, MessageAttributeValue> msgAttributes = new HashMap<>();
        attributes.forEach((k, v) -> msgAttributes.put(
                k,
                MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue(v)
                        .build()));

        SendMessageRequest request = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(message)
                .messageAttributes(msgAttributes)
                .build();
        SendMessageResponse response;
        try {
            response = sqsClient.sendMessage(request);
        } catch (Exception e) {
            throw new PublisherException("Exception sending SQS message: " + e.getMessage(), e);
        }
        if (response.sdkHttpResponse().isSuccessful()) {
            log.info("Sent message to SQS queue: {}", queueName);
        } else {
            throw new PublisherException("SQS sendMessage unsuccessful. HTTP status: "
                    + response.sdkHttpResponse().statusCode());
        }
    }

    @Override
    public List<QueueMessageEnvelope> receive(String queueName, int maxMessages, int waitTimeSeconds) {
        String queueUrl = resolveQueueUrl(queueName);
        ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(maxMessages)
                .waitTimeSeconds(waitTimeSeconds)
                .messageAttributeNames("All")
                .build();
        try {
            ReceiveMessageResponse response = sqsClient.receiveMessage(request);
            List<QueueMessageEnvelope> envelopes = new ArrayList<>();
            for (Message msg : response.messages()) {
                Map<String, String> attrs = new HashMap<>();
                msg.messageAttributes().forEach((k, v) -> attrs.put(k, v.stringValue()));
                envelopes.add(new QueueMessageEnvelope(msg.messageId(), msg.body(), msg.receiptHandle(), attrs));
            }
            return envelopes;
        } catch (Exception e) {
            throw new PublisherException("Exception receiving SQS messages: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String queueName, String receiptHandle) {
        String queueUrl = resolveQueueUrl(queueName);
        DeleteMessageRequest request = DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(receiptHandle)
                .build();
        try {
            sqsClient.deleteMessage(request);
            log.info("Deleted message from SQS queue: {}", queueName);
        } catch (Exception e) {
            throw new PublisherException("Exception deleting SQS message: " + e.getMessage(), e);
        }
    }
}
