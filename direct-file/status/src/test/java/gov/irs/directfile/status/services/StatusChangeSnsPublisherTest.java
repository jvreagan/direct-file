package gov.irs.directfile.status.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sns.SnsClient;

import gov.irs.directfile.status.config.SnsConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatusChangeSnsPublisherTest {

    @Mock
    private SnsClient snsClient;

    @Mock
    private SnsConfiguration snsConfiguration;

    private StatusChangeSnsPublisher publisher;

    @BeforeEach
    void setUp() {
        when(snsConfiguration.getStatusChangeTopicArn()).thenReturn("arn:aws:sns:us-east-1:123456789:test-topic");
        publisher = new StatusChangeSnsPublisher(snsClient, snsConfiguration);
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
