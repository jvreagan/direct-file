package gov.irs.directfile.models.storage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class LocalFileDocumentStorageServiceTest {
    @TempDir
    Path tempDir;

    private LocalFileDocumentStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new LocalFileDocumentStorageService(tempDir);
    }

    @Test
    void givenData_whenStore_thenFileIsCreated() throws IOException {
        InputStream data = new ByteArrayInputStream("hello world".getBytes(StandardCharsets.UTF_8));

        storageService.store("test.txt", data);

        assertTrue(storageService.exists("test.txt"));
    }

    @Test
    void givenStoredFile_whenRetrieve_thenReturnsCorrectContent() throws IOException {
        String content = "hello world";
        storageService.store("test.txt", new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));

        InputStream retrieved = storageService.retrieve("test.txt");
        String result = new String(retrieved.readAllBytes(), StandardCharsets.UTF_8);

        assertEquals(content, result);
    }

    @Test
    void givenStoredFile_whenDelete_thenFileIsRemoved() throws IOException {
        storageService.store("test.txt", new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8)));
        assertTrue(storageService.exists("test.txt"));

        storageService.delete("test.txt");

        assertFalse(storageService.exists("test.txt"));
    }

    @Test
    void givenNonexistentFile_whenRetrieve_thenThrowsStorageException() {
        assertThrows(StorageException.class, () -> storageService.retrieve("nonexistent.txt"));
    }

    @Test
    void givenNestedPath_whenStore_thenCreatesParentDirectories() throws IOException {
        storageService.store(
                "subdir/nested/file.txt", new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8)));

        assertTrue(storageService.exists("subdir/nested/file.txt"));
        InputStream retrieved = storageService.retrieve("subdir/nested/file.txt");
        assertEquals("data", new String(retrieved.readAllBytes(), StandardCharsets.UTF_8));
    }

    @Test
    void givenExistingFile_whenStoreAgain_thenOverwrites() throws IOException {
        storageService.store("test.txt", new ByteArrayInputStream("original".getBytes(StandardCharsets.UTF_8)));
        storageService.store("test.txt", new ByteArrayInputStream("updated".getBytes(StandardCharsets.UTF_8)));

        InputStream retrieved = storageService.retrieve("test.txt");
        assertEquals("updated", new String(retrieved.readAllBytes(), StandardCharsets.UTF_8));
    }

    @Test
    void givenMultipleFiles_whenList_thenReturnsAllFiles() throws IOException {
        storageService.store("dir/file1.txt", new ByteArrayInputStream("a".getBytes(StandardCharsets.UTF_8)));
        storageService.store("dir/file2.txt", new ByteArrayInputStream("b".getBytes(StandardCharsets.UTF_8)));
        storageService.store("dir/sub/file3.txt", new ByteArrayInputStream("c".getBytes(StandardCharsets.UTF_8)));

        List<StorageResource> resources = storageService.list("dir");

        assertEquals(3, resources.size());
    }

    @Test
    void givenNonexistentPrefix_whenList_thenReturnsEmptyList() {
        List<StorageResource> resources = storageService.list("nonexistent");

        assertTrue(resources.isEmpty());
    }

    @Test
    void givenExistingFile_whenExists_thenReturnsTrue() throws IOException {
        storageService.store("test.txt", new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8)));

        assertTrue(storageService.exists("test.txt"));
    }

    @Test
    void givenNonexistentFile_whenExists_thenReturnsFalse() {
        assertFalse(storageService.exists("nonexistent.txt"));
    }

    @Test
    void givenNonexistentFile_whenDelete_thenDoesNotThrow() {
        assertDoesNotThrow(() -> storageService.delete("nonexistent.txt"));
    }
}
