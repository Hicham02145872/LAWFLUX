package ma.lawflux.documents.domain;

import java.util.List;
import java.util.UUID;

public interface DocumentVersionRepository {

    boolean documentExists(UUID cabinetId, UUID documentId);

    /** Attribue le numéro suivant sans écrasement, même en cas d'ajouts concurrents. */
    DocumentVersion addVersion(NewVersion version);

    /** Du plus récent au plus ancien, limité au cabinet de l'appelant. */
    List<DocumentVersion> findByDocument(UUID cabinetId, UUID documentId);
}