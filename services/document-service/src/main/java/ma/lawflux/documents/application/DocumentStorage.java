package ma.lawflux.documents.application;

import java.io.InputStream;
import java.util.UUID;

/** À implémenter par le composant de stockage du chef (validation du contenu incluse). */
public interface DocumentStorage {
    String store(UUID cabinetId, String originalName, InputStream content, long sizeBytes);
}