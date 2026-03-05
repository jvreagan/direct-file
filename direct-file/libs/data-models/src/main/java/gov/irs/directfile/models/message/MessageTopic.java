package gov.irs.directfile.models.message;

import java.util.Map;

public interface MessageTopic {
    void publish(String topicId, String message);

    void publish(String topicId, String message, Map<String, String> attributes);
}
