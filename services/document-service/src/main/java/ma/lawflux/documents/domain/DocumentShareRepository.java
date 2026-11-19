package ma.lawflux.documents.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface DocumentShareRepository {

    /** Toujours limité au cabinet de l'appelant. */
    Optional<Document> findDocument(UUID cabinetId, UUID documentId);

    Optional<DocumentShare> findActiveShare(UUID cabinetId, UUID documentId);

    /** Idempotent : renvoie le partage actif existant, sinon en crée un. */
    DocumentShare share(UUID cabinetId, UUID documentId, UUID sharedBy, Instant at);

    /** Renvoie true si un partage actif a été fermé. L'historique est conservé. */
    boolean revoke(UUID cabinetId, UUID documentId, UUID revokedBy, Instant at);
}