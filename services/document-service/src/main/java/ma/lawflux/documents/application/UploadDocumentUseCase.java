package ma.lawflux.documents.application;

import java.time.Instant;
import java.util.UUID;
import ma.lawflux.documents.domain.CaseAccessDeniedException;
import ma.lawflux.documents.domain.Document;
import ma.lawflux.documents.domain.DocumentRepository;
import ma.lawflux.documents.domain.DocumentType;
import ma.lawflux.documents.domain.InvalidDocumentException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;

@Service
@ConditionalOnProperty(prefix = "lawflux.documents", name = "business-enabled", havingValue = "true")
public class UploadDocumentUseCase {

    private static final int MAX_NAME_LENGTH = 255;

    private final DocumentRepository repository;
    private final DocumentStorage storage;
    private final CaseAccessChecker caseAccess;
    private final DataSize maxFileSize;

    public UploadDocumentUseCase(
            DocumentRepository repository,
            DocumentStorage storage,
            CaseAccessChecker caseAccess,
            @Value("${lawflux.documents.max-file-size}") DataSize maxFileSize) {
        this.repository = repository;
        this.storage = storage;
        this.caseAccess = caseAccess;
        this.maxFileSize = maxFileSize;
    }

    public Document execute(UploadCommand cmd) {
        String name = cmd.fileName() == null ? "" : cmd.fileName().trim();
        if (name.isEmpty() || name.contains("/") || name.contains("\\") || name.contains("..")) {
            throw new InvalidDocumentException("Nom de fichier invalide");
        }
        // PostgreSQL compte des caractères (points de code), pas des unités UTF-16.
        if (name.codePointCount(0, name.length()) > MAX_NAME_LENGTH) {
            throw new InvalidDocumentException("Nom de fichier trop long");
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

        // Le dossier doit appartenir au cabinet de l'appelant, avant toute écriture.
        if (!caseAccess.canAccess(cmd.cabinetId(), cmd.caseId())) {
            throw new CaseAccessDeniedException("Accès au dossier refusé");
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