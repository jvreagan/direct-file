package gov.irs.directfile.models.message;

import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;

@Slf4j
public class SnsMessageTopic implements MessageTopic {
    private final SnsClient snsClient;

    public SnsMessageTopic(SnsClient snsClient) {
        this.snsClient = snsClient;
    }

    @Override
    public void publish(String topicArn, String message) {
        publish(topicArn, message, Map.of());
    }

    @Override
    public void publish(String topicArn, String message, Map<String, String> attributes) {
        Map<String, MessageAttributeValue> msgAttributes = new HashMap<>();
        attributes.forEach((k, v) -> msgAttributes.put(
                k,
                MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue(v)
                        .build()));

        PublishRequest request = PublishRequest.builder()
                .topicArn(topicArn)
                .message(message)
                .messageAttributes(msgAttributes)
                .build();
        PublishResponse response;
        try {
            response = snsClient.publish(request);
        } catch (Exception e) {
            throw new PublisherException("Exception publishing to SNS: " + e.getMessage(), e);
        }
        if (response.sdkHttpResponse().isSuccessful()) {
            log.info("Published message to SNS topic: {}", topicArn);
        } else {
            throw new PublisherException("SNS publish unsuccessful. HTTP status: "
                    + response.sdkHttpResponse().statusCode());
        }
    }
}
