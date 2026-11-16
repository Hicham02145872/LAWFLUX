package ma.lawflux.documents.application;

import java.time.Instant;
import java.util.UUID;
import ma.lawflux.documents.domain.Document;
import ma.lawflux.documents.domain.DocumentRepository;
import ma.lawflux.documents.domain.DocumentType;
import ma.lawflux.documents.domain.InvalidDocumentException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;

@Service
public class UploadDocumentUseCase {

    private final DocumentRepository repository;
    private final DocumentStorage storage;
    private final DataSize maxFileSize;

    public UploadDocumentUseCase(
            DocumentRepository repository,
            DocumentStorage storage,
            @Value("${lawflux.documents.max-file-size}") DataSize maxFileSize) {
        this.repository = repository;
        this.storage = storage;
        this.maxFileSize = maxFileSize;
    }

    public Document execute(UploadCommand cmd) {
        String name = cmd.fileName() == null ? "" : cmd.fileName().trim();
        if (name.isEmpty() || name.contains("/") || name.contains("\\") || name.contains("..")) {
            throw new InvalidDocumentException("Nom de fichier invalide");
        }
        if (cmd.sizeBytes() <= 0) {
            throw new InvalidDocumentException("Fichier vide");
        }
        if (cmd.sizeBytes() > maxFileSize.toBytes()) {
            throw new InvalidDocumentException("Fichier trop volumineux");
        }
        DocumentType type;
        try {
            type = DocumentType.fromFileName(name);
        } catch (IllegalArgumentException e) {
            throw new InvalidDocumentException(e.getMessage());
        }

        String storageKey = storage.store(cmd.cabinetId(), name, cmd.content(), cmd.sizeBytes());
        Document document = new Document(
                UUID.randomUUID(), cmd.cabinetId(), cmd.caseId(), name, type,
                cmd.sizeBytes(), storageKey, cmd.uploadedBy(), cmd.authorName(), Instant.now());
        try {
            return repository.save(document);
        } catch (RuntimeException e) {
            storage.delete(storageKey);
            throw e;
        }
    }
}