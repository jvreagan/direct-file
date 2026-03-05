package gov.irs.directfile.submit.actions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import gov.irs.directfile.submit.config.Config;
import gov.irs.directfile.submit.config.DirectoriesConfig;
import gov.irs.directfile.submit.domain.DocumentStoreResource;
import gov.irs.directfile.submit.domain.SubmissionBatch;
import gov.irs.directfile.submit.service.interfaces.ISynchronousDocumentStoreService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CleanupActionHandlerTest {

    @Mock
    private ISynchronousDocumentStoreService documentStoreService;

    @Mock
    private ActionContext actionContext;

    @Mock
    private Config config;

    @Mock
    private DirectoriesConfig directoriesConfig;

    @TempDir
    Path tempDir;

    private CleanupActionHandler cleanupActionHandler;

    @BeforeEach
    void setUp() {
        cleanupActionHandler = new CleanupActionHandler(actionContext, documentStoreService);
    }

    @Test
    void givenNonExistentPath_whenCleanDirectory_thenNoException() {
        // given
        Path nonExistentPath = tempDir.resolve("does-not-exist");

        // when / then
        assertThatCode(() -> cleanupActionHandler.cleanDirectory(nonExistentPath))
                .doesNotThrowAnyException();
    }

    @Test
    void givenDirectoryWithFiles_whenCleanDirectory_thenFilesDeleted() throws IOException {
        // given: create a directory with files and a subdirectory with files
        Path subDir = Files.createDirectory(tempDir.resolve("subdir"));
        Path file1 = Files.createFile(tempDir.resolve("file1.txt"));
        Path file2 = Files.createFile(subDir.resolve("file2.txt"));
        Files.writeString(file1, "content1");
        Files.writeString(file2, "content2");

        assertThat(file1).exists();
        assertThat(file2).exists();

        // when
        cleanupActionHandler.cleanDirectory(tempDir);

        // then: files should be deleted, subdirectories should be deleted, but root directory remains
        assertThat(file1).doesNotExist();
        assertThat(file2).doesNotExist();
        assertThat(subDir).doesNotExist();
        assertThat(tempDir).exists();
    }

    @Test
    void givenDocumentStoreKeys_whenCleanDocumentStore_thenDeletesInBatches() {
        // given
        SubmissionBatch batch = new SubmissionBatch(0L, "env/batches/0");

        List<DocumentStoreResource> resources = List.of(
                new DocumentStoreResource("env/batches/0/manifest.xml", "manifest.xml", Instant.now()),
                new DocumentStoreResource("env/batches/0/submission.xml", "submission.xml", Instant.now()),
                new DocumentStoreResource("env/batches/0/userContext.json", "userContext.json", Instant.now()));

        when(documentStoreService.getObjectKeys(eq("env/batches/0"))).thenReturn(resources);

        // when
        cleanupActionHandler.cleanDocumentStore(batch);

        // then: deleteObjects should be called once since 3 keys < 1000 batch size
        verify(documentStoreService)
                .deleteObjects(List.of(
                        "env/batches/0/manifest.xml",
                        "env/batches/0/submission.xml",
                        "env/batches/0/userContext.json"));
    }
}
