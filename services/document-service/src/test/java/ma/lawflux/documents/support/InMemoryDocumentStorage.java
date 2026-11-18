package ma.lawflux.documents.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import ma.lawflux.documents.application.DocumentStorage;

public class InMemoryDocumentStorage implements DocumentStorage {

    public final Map<String, byte[]> files = new LinkedHashMap<>();
    public final List<String> deleted = new ArrayList<>();

    @Override
    public String store(UUID cabinetId, String originalName, InputStream content, long sizeBytes) {
        try {
            String key = cabinetId + "/" + UUID.randomUUID();
            files.put(key, content.readAllBytes());
            return key;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void delete(String storageKey) {
        files.remove(storageKey);
        deleted.add(storageKey);
    }
}