package ma.lawflux.documents.support;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import ma.lawflux.documents.domain.Document;
import ma.lawflux.documents.domain.DocumentNotFoundException;
import ma.lawflux.documents.domain.DocumentShare;
import ma.lawflux.documents.domain.DocumentShareRepository;

public class InMemoryDocumentShareRepository implements DocumentShareRepository {

    private final Map<String, Document> documents = new HashMap<>();
    private final Map<String, DocumentShare> active = new HashMap<>();
    public int shareCalls;
    public int revokeCalls;

    private static String key(UUID cabinetId, UUID documentId) {
        return cabinetId + "|" + documentId;
    }

    public void register(Document document) {
        documents.put(key(document.cabinetId(), document.id()), document);
    }

    @Override
    public Optional<Document> findDocument(UUID cabinetId, UUID documentId) {
        return Optional.ofNullable(documents.get(key(cabinetId, documentId)));
    }

    @Override
    public Optional<DocumentShare> findActiveShare(UUID cabinetId, UUID documentId) {
        return Optional.ofNullable(active.get(key(cabinetId, documentId)));
    }

    @Override
    public DocumentShare share(UUID cabinetId, UUID documentId, UUID sharedBy, Instant at) {
        String key = key(cabinetId, documentId);
        if (!documents.containsKey(key)) {
            throw new DocumentNotFoundException("Document introuvable");
        }
        shareCalls++;
        return active.computeIfAbsent(key,
                k -> new DocumentShare(UUID.randomUUID(), documentId, cabinetId, sharedBy, at));
    }

    @Override
    public boolean revoke(UUID cabinetId, UUID documentId, UUID revokedBy, Instant at) {
        revokeCalls++;
        return active.remove(key(cabinetId, documentId)) != null;
    }
}