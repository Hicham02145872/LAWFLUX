package ma.lawflux.documents.domain;

import java.time.Instant;
import java.util.UUID;

public record Document(
        UUID id,
        UUID cabinetId,
        UUID caseId,
        String name,
        DocumentType type,
        long sizeBytes,
        String storageKey,
        UUID uploadedBy,
        String authorName,
        Instant createdAt) {
}