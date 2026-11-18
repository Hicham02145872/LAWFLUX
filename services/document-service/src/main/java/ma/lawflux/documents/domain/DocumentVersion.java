package ma.lawflux.documents.domain;

import java.time.Instant;
import java.util.UUID;

public record DocumentVersion(
        UUID id,
        UUID documentId,
        UUID cabinetId,
        int number,
        DocumentType type,
        long sizeBytes,
        String storageKey,
        UUID uploadedBy,
        String authorName,
        Instant createdAt) {
}