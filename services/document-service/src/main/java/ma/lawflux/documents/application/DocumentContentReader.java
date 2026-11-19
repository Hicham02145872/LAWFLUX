package ma.lawflux.documents.application;

import java.io.InputStream;

/**
 * À implémenter par le stockage privé du chef. L'appelant ferme le flux.
 * Le contenu ne sort jamais par une URL publique ni avec sa clé de stockage.
 */
public interface DocumentContentReader {
    InputStream open(String storageKey);
}