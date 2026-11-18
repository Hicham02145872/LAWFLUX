package ma.lawflux.documents.support;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import ma.lawflux.documents.domain.DocumentNotFoundException;
import ma.lawflux.documents.domain.DocumentType;
import ma.lawflux.documents.domain.DocumentVersion;
import ma.lawflux.documents.domain.DocumentVersionRepository;
import ma.lawflux.documents.domain.NewVersion;

public class InMemoryDocumentVersionRepository implements DocumentVersionRepository {

    private final Set<String> documents = new HashSet<>();
    public final List<DocumentVersion> versions = new ArrayList<>();
    public boolean failAdd = false;

    private static String key(UUID cabinetId, UUID documentId) {
        return cabinetId + "|" + documentId;
    }

    /** Enregistre un document avec sa version 1, comme le fait le dépôt JDBC. */
    public void registerDocument(UUID cabinetId, UUID documentId) {
        documents.add(key(cabinetId, documentId));
        versions.add(new DocumentVersion(UUID.randomUUID(), documentId, cabinetId, 1,
                DocumentType.PDF, 10, "initial-" + documentId, UUID.randomUUID(), "Karim",
                Instant.parse("2026-11-01T10:00:00Z")));
    }

    @Override
    public boolean documentExists(UUID cabinetId, UUID documentId) {
        return documents.contains(key(cabinetId, documentId));
    }

    @Override
    public synchronized DocumentVersion addVersion(NewVersion nv) {
        if (!documentExists(nv.cabinetId(), nv.documentId())) {
            throw new DocumentNotFoundException("Document introuvable");
        }
        if (failAdd) {
            throw new IllegalStateException("db indisponible");
        }
        int next = versions.stream()
                .filter(v -> v.documentId().equals(nv.documentId()))
                .mapToInt(DocumentVersion::number)
                .max().orElse(0) + 1;
        DocumentVersion version = new DocumentVersion(UUID.randomUUID(), nv.documentId(),
                nv.cabinetId(), next, nv.type(), nv.sizeBytes(), nv.storageKey(),
                nv.uploadedBy(), nv.authorName(), nv.createdAt());
        versions.add(version);
        return version;
    }

    @Override
    public List<DocumentVersion> findByDocument(UUID cabinetId, UUID documentId) {
        return versions.stream()
                .filter(v -> v.cabinetId().equals(cabinetId) && v.documentId().equals(documentId))
                .sorted(Comparator.comparingInt(DocumentVersion::number).reversed())
                .toList();
    }
}