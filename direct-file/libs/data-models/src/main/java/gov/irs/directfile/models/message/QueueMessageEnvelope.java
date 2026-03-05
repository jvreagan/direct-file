package gov.irs.directfile.models.message;

import java.util.Map;

public record QueueMessageEnvelope(
        String messageId, String body, String receiptHandle, Map<String, String> attributes) {}
