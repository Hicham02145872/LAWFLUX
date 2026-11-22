package ma.lawflux.documents.application;

import java.io.InputStream;
import ma.lawflux.documents.domain.DocumentType;

/** Le flux doit être fermé par celui qui reçoit ce résultat. */
public record DocumentDownload(String fileName, DocumentType type, long sizeBytes, InputStream content) {
}