package ma.lawflux.documents.application;

import java.io.InputStream;
import java.util.UUID;

public record AddVersionCommand(
        UUID cabinetId,
        UUID documentId,
        UUID uploadedBy,
        String authorName,
        String fileName,
        long sizeBytes,
        InputStream content) {
}