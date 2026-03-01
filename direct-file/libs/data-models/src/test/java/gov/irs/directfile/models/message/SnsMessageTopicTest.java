package gov.irs.directfile.models.message;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SnsMessageTopicTest {
    private SnsMessageTopic snsMessageTopic;

    @Mock
    private SnsClient snsClient;

    @Mock
    private PublishResponse publishResponse;

    @Mock
    private SdkHttpResponse sdkHttpResponse;

    private final String topicArn = "arn:aws:sns:us-east-1:000000000000:test-topic";

    @BeforeEach
    void setUp() {
        snsMessageTopic = new SnsMessageTopic(snsClient);
    }

    @Test
    void givenValidMessage_whenPublish_thenPublishesToCorrectArn() {
        when(snsClient.publish(any(PublishRequest.class))).thenReturn(publishResponse);
        when(publishResponse.sdkHttpResponse()).thenReturn(sdkHttpResponse);
        when(sdkHttpResponse.isSuccessful()).thenReturn(true);

        snsMessageTopic.publish(topicArn, "hello", Map.of("attr1", "val1"));

        ArgumentCaptor<PublishRequest> captor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(snsClient).publish(captor.capture());
        PublishRequest request = captor.getValue();
        assertEquals(topicArn, request.topicArn());
        assertEquals("hello", request.message());
        assertEquals("val1", request.messageAttributes().get("attr1").stringValue());
        assertEquals("String", request.messageAttributes().get("attr1").dataType());
    }

    @Test
    void givenNoAttributes_whenPublishSimple_thenPublishesWithEmptyAttributes() {
        when(snsClient.publish(any(PublishRequest.class))).thenReturn(publishResponse);
        when(publishResponse.sdkHttpResponse()).thenReturn(sdkHttpResponse);
        when(sdkHttpResponse.isSuccessful()).thenReturn(true);

        snsMessageTopic.publish(topicArn, "hello");

        ArgumentCaptor<PublishRequest> captor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(snsClient).publish(captor.capture());
        assertTrue(captor.getValue().messageAttributes().isEmpty());
    }

    @Test
    void givenSnsClientThrows_whenPublish_thenThrowsPublisherException() {
        when(snsClient.publish(any(PublishRequest.class)))
                .thenThrow(NotFoundException.builder().build());

        assertThrows(PublisherException.class, () -> snsMessageTopic.publish(topicArn, "hello"));
        verify(snsClient, times(1)).publish(any(PublishRequest.class));
    }

    @Test
    void givenUnsuccessfulHttpResponse_whenPublish_thenThrowsPublisherException() {
        when(snsClient.publish(any(PublishRequest.class))).thenReturn(publishResponse);
        when(publishResponse.sdkHttpResponse()).thenReturn(sdkHttpResponse);
        when(sdkHttpResponse.isSuccessful()).thenReturn(false);
        when(sdkHttpResponse.statusCode()).thenReturn(500);

        assertThrows(PublisherException.class, () -> snsMessageTopic.publish(topicArn, "hello"));
        verify(snsClient, times(1)).publish(any(PublishRequest.class));
    }

    @Test
    void givenMultipleAttributes_whenPublish_thenAllAttributesMapped() {
        when(snsClient.publish(any(PublishRequest.class))).thenReturn(publishResponse);
        when(publishResponse.sdkHttpResponse()).thenReturn(sdkHttpResponse);
        when(sdkHttpResponse.isSuccessful()).thenReturn(true);

        Map<String, String> attrs = Map.of("key1", "val1", "key2", "val2", "key3", "val3");
        snsMessageTopic.publish(topicArn, "hello", attrs);

        ArgumentCaptor<PublishRequest> captor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(snsClient).publish(captor.capture());
        assertEquals(3, captor.getValue().messageAttributes().size());
    }
}
