package ma.lawflux.documents.domain;

import java.time.Instant;
import java.util.UUID;

/** Données d'une nouvelle version : le numéro est attribué par le dépôt, de façon atomique. */
public record NewVersion(
        UUID cabinetId,
        UUID documentId,
        DocumentType type,
        long sizeBytes,
        String storageKey,
        UUID uploadedBy,
        String authorName,
        Instant createdAt) {
}