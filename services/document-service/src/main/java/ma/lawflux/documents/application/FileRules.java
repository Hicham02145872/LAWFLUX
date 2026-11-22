package ma.lawflux.documents.application;

import ma.lawflux.documents.domain.DocumentType;
import ma.lawflux.documents.domain.InvalidDocumentException;
import org.springframework.util.unit.DataSize;

final class FileRules {

    static final int MAX_NAME_LENGTH = 255;

    private FileRules() {
    }

    static String validName(String raw) {
        String name = raw == null ? "" : raw.trim();
        if (name.isEmpty() || name.contains("/") || name.contains("\\") || name.contains("..")) {
            throw new InvalidDocumentException("Nom de fichier invalide");
        }
        // PostgreSQL compte des caractères (points de code), pas des unités UTF-16.
        if (name.codePointCount(0, name.length()) > MAX_NAME_LENGTH) {
            throw new InvalidDocumentException("Nom de fichier trop long");
        }
        return name;
    }

    static void checkSize(long sizeBytes, DataSize max) {
        if (sizeBytes <= 0) {
            throw new InvalidDocumentException("Fichier vide");
        }
        if (sizeBytes > max.toBytes()) {
            throw new InvalidDocumentException("Fichier trop volumineux");
        }
    }

    static DocumentType typeOf(String name) {
        try {
            return DocumentType.fromFileName(name);
        } catch (IllegalArgumentException e) {
            throw new InvalidDocumentException(e.getMessage());
        }
    }
}