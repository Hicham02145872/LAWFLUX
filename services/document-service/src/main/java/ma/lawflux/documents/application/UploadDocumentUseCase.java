package ma.lawflux.documents.application;

import java.time.Instant;
import java.util.UUID;
import ma.lawflux.documents.domain.CaseAccessDeniedException;
import ma.lawflux.documents.domain.Document;
import ma.lawflux.documents.domain.DocumentRepository;
import ma.lawflux.documents.domain.DocumentType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;

@Service
@ConditionalOnProperty(prefix = "lawflux.documents", name = "business-enabled", havingValue = "true")
public class UploadDocumentUseCase {

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
        String name = FileRules.validName(cmd.fileName());
        FileRules.checkSize(cmd.sizeBytes(), maxFileSize);
        DocumentType type = FileRules.typeOf(name);

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