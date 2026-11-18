package ma.lawflux.documents.application;

import java.io.InputStream;
import java.util.UUID;

/** À implémenter par le composant de stockage privé du chef (validation du contenu incluse). */
public interface DocumentStorage {

    String store(UUID cabinetId, String originalName, InputStream content, long sizeBytes);

    /**
     * Nettoyage technique uniquement (upload incomplet ou échoué).
     * Ne jamais l'utiliser pour supprimer une version conservée sans passer par les règles métier.
     */
    void delete(String storageKey);
}