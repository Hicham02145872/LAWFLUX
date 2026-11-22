package ma.lawflux.documents.application;

import java.time.Instant;
import ma.lawflux.documents.domain.DocumentNotFoundException;
import ma.lawflux.documents.domain.DocumentType;
import ma.lawflux.documents.domain.DocumentVersion;
import ma.lawflux.documents.domain.DocumentVersionRepository;
import ma.lawflux.documents.domain.NewVersion;
import ma.lawflux.documents.domain.DocumentShareRepository;
import ma.lawflux.documents.domain.CaseAccessDeniedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;

@Service
@ConditionalOnProperty(prefix = "lawflux.documents", name = "business-enabled", havingValue = "true")
public class AddVersionUseCase {

    private final DocumentVersionRepository versions;
    private final DocumentStorage storage;
    private final DocumentShareRepository documents;
    private final CaseAccessChecker caseAccess;
    private final DataSize maxFileSize;

    public AddVersionUseCase(
            DocumentVersionRepository versions,
            DocumentStorage storage,
            DocumentShareRepository documents,
            CaseAccessChecker caseAccess,
            @Value("${lawflux.documents.max-file-size}") DataSize maxFileSize) {
        this.versions = versions;
        this.storage = storage;
        this.documents = documents;
        this.caseAccess = caseAccess;
        this.maxFileSize = maxFileSize;
    }

    public DocumentVersion execute(AddVersionCommand cmd) {
        String name = FileRules.validName(cmd.fileName());
        FileRules.checkSize(cmd.sizeBytes(), maxFileSize);
        DocumentType type = FileRules.typeOf(name);

        // Rien n'est écrit si le document n'existe pas dans le cabinet de l'appelant.
        var document = documents.findDocument(cmd.cabinetId(), cmd.documentId())
                .orElseThrow(() -> new DocumentNotFoundException("Document introuvable"));
        if (!caseAccess.canAccess(cmd.cabinetId(), document.caseId(), "DOCUMENT_WRITE"))
            throw new CaseAccessDeniedException("Accès au dossier refusé");

        String storageKey = storage.store(cmd.cabinetId(), name, cmd.content(), cmd.sizeBytes());
        try {
            return versions.addVersion(new NewVersion(
                    cmd.cabinetId(), cmd.documentId(), type, cmd.sizeBytes(), storageKey,
                    cmd.uploadedBy(), cmd.authorName(), Instant.now()));
        } catch (RuntimeException e) {
            // Nettoie uniquement le fichier qu'on vient d'écrire, jamais une version conservée.
            storage.delete(storageKey);
            throw e;
        }
    }
}
