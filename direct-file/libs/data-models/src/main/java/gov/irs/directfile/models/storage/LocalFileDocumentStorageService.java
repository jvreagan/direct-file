package gov.irs.directfile.models.storage;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LocalFileDocumentStorageService implements DocumentStorageService {
    private final Path baseDirectory;

    public LocalFileDocumentStorageService(Path baseDirectory) {
        this.baseDirectory = baseDirectory;
        try {
            Files.createDirectories(baseDirectory);
        } catch (IOException e) {
            throw new StorageException("Failed to create base directory: " + baseDirectory, e);
        }
    }

    private Path resolvePath(String key) {
        return baseDirectory.resolve(key);
    }

    @Override
    public void store(String key, InputStream data, Map<String, String> metadata) throws IOException {
        Path filePath = resolvePath(key);
        Path parent = filePath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.copy(data, filePath, StandardCopyOption.REPLACE_EXISTING);
        log.debug("LocalFile: stored {}", filePath);
    }

    @Override
    public InputStream retrieve(String key) throws IOException {
        Path filePath = resolvePath(key);
        if (!Files.exists(filePath)) {
            throw new StorageException("File not found: " + filePath);
        }
        return new FileInputStream(filePath.toFile());
    }

    @Override
    public void delete(String key) {
        Path filePath = resolvePath(key);
        try {
            Files.deleteIfExists(filePath);
            log.debug("LocalFile: deleted {}", filePath);
        } catch (IOException e) {
            throw new StorageException("Failed to delete: " + filePath, e);
        }
    }

    @Override
    public List<StorageResource> list(String prefix) {
        Path prefixPath = resolvePath(prefix);
        List<StorageResource> resources = new ArrayList<>();
        if (!Files.exists(prefixPath)) {
            return resources;
        }
        try (Stream<Path> paths = Files.walk(prefixPath)) {
            paths.filter(Files::isRegularFile).forEach(path -> {
                String relativePath = baseDirectory.relativize(path).toString();
                String resourceId = path.getFileName().toString();
                int dotIndex = resourceId.lastIndexOf(".");
                if (dotIndex > 0) {
                    resourceId = resourceId.substring(0, dotIndex);
                }
                try {
                    resources.add(new StorageResource(
                            relativePath,
                            resourceId,
                            Files.getLastModifiedTime(path).toInstant()));
                } catch (IOException e) {
                    resources.add(new StorageResource(relativePath, resourceId, Instant.now()));
                }
            });
        } catch (IOException e) {
            throw new StorageException("Failed to list files with prefix: " + prefix, e);
        }
        return resources;
    }

    @Override
    public boolean exists(String key) {
        return Files.exists(resolvePath(key));
    }
}
