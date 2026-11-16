package ma.lawflux.documents.application;

import java.io.InputStream;
import java.util.UUID;

public record UploadCommand(
        UUID cabinetId,
        UUID caseId,
        UUID uploadedBy,
        String authorName,
        String fileName,
        long sizeBytes,
        InputStream content) {
}