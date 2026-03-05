package gov.irs.directfile.status.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsClient;

import gov.irs.directfile.status.config.MessageQueueConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatusChangeSqsPublisherTest {

    @Mock
    private SqsClient sqsClient;

    @Mock
    private MessageQueueConfiguration messageQueueConfiguration;

    private StatusChangeSqsPublisher publisher;

    @BeforeEach
    void setUp() {
        when(messageQueueConfiguration.getStatusChangeQueue()).thenReturn("status-change-queue");
        publisher = new StatusChangeSqsPublisher(sqsClient, messageQueueConfiguration);
    }

    @Test
    void givenValidConfig_whenConstructed_thenNoException() {
        assertThat(publisher).isNotNull();
    }

    @Test
    void givenPublisher_whenConstructed_thenImplementsStatusChangePublisher() {
        assertThat(publisher).isInstanceOf(StatusChangePublisher.class);
    }
}
