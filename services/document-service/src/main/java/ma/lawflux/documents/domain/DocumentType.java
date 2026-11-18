package ma.lawflux.documents.domain;

import java.util.Locale;

public enum DocumentType {
    PDF, DOCX;

    public static DocumentType fromFileName(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) return PDF;
        if (lower.endsWith(".docx")) return DOCX;
        throw new IllegalArgumentException("Type de fichier non pris en charge");
    }
}