package gov.irs.directfile.models.storage;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface DocumentStorageService {
    void store(String key, InputStream data, Map<String, String> metadata) throws IOException;

    default void store(String key, InputStream data) throws IOException {
        store(key, data, Map.of());
    }

    InputStream retrieve(String key) throws IOException;

    void delete(String key);

    List<StorageResource> list(String prefix);

    boolean exists(String key);
}
