package gov.irs.directfile.models.storage;

import java.time.Instant;

public record StorageResource(String key, String resourceId, Instant lastModified) {}
