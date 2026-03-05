package gov.irs.directfile.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;

import gov.irs.directfile.models.message.*;

@Configuration
public class MessagingConfiguration {

    @Bean
    @Profile(BeanProfiles.AWS)
    public MessageQueue sqsMessageQueue(SqsClient sqsClient) {
        return new SqsMessageQueue(sqsClient);
    }

    @Bean
    @Profile(BeanProfiles.LOCAL)
    public MessageQueue inMemoryMessageQueue() {
        return new InMemoryMessageQueue();
    }

    @Bean
    @Profile(BeanProfiles.AWS)
    public MessageTopic snsMessageTopic(SnsClient snsClient) {
        return new SnsMessageTopic(snsClient);
    }

    @Bean
    @Profile(BeanProfiles.LOCAL)
    public MessageTopic inMemoryMessageTopic() {
        return new InMemoryMessageTopic();
    }
}
