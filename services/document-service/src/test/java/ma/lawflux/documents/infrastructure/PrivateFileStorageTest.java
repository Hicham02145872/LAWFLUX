package ma.lawflux.documents.infrastructure;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PrivateFileStorageTest {
    @TempDir Path directory;

    @Test
    void writesReadsAndDeletesPrivateContent() throws Exception {
        PrivateFileStorage storage = new PrivateFileStorage(directory.toString());
        byte[] content = "document prive".getBytes();
        String key = storage.store(UUID.randomUUID(), "../../nom.pdf", new ByteArrayInputStream(content), content.length);
        assertFalse(key.contains("nom.pdf"));
        try (var stream = storage.open(key)) { assertArrayEquals(content, stream.readAllBytes()); }
        storage.delete(key);
        assertFalse(Files.exists(directory.resolve(key)));
    }

    @Test
    void rejectsTraversalAndIncompleteUpload() throws Exception {
        PrivateFileStorage storage = new PrivateFileStorage(directory.toString());
        assertThrows(IllegalArgumentException.class, () -> storage.open("../outside"));
        assertThrows(IllegalStateException.class, () ->
                storage.store(UUID.randomUUID(), "x.pdf", new ByteArrayInputStream(new byte[1]), 2));
        try (var files = Files.walk(directory)) {
            assertEquals(0, files.filter(Files::isRegularFile).count());
        }
    }
}
