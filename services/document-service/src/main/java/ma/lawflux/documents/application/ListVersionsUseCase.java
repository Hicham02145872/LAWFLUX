package ma.lawflux.documents.application;

import java.util.List;
import java.util.UUID;
import ma.lawflux.documents.domain.DocumentNotFoundException;
import ma.lawflux.documents.domain.DocumentVersion;
import ma.lawflux.documents.domain.DocumentVersionRepository;
import ma.lawflux.documents.domain.DocumentShareRepository;
import ma.lawflux.documents.domain.CaseAccessDeniedException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "lawflux.documents", name = "business-enabled", havingValue = "true")
public class ListVersionsUseCase {

    private final DocumentVersionRepository versions;
    private final DocumentShareRepository documents;
    private final CaseAccessChecker caseAccess;

    public ListVersionsUseCase(DocumentVersionRepository versions, DocumentShareRepository documents,
                               CaseAccessChecker caseAccess) {
        this.versions = versions;
        this.documents = documents;
        this.caseAccess = caseAccess;
    }

    public List<DocumentVersion> execute(UUID cabinetId, UUID documentId) {
        var document = documents.findDocument(cabinetId, documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document introuvable"));
        if (!caseAccess.canAccess(cabinetId, document.caseId()))
            throw new CaseAccessDeniedException("Accès au dossier refusé");
        return versions.findByDocument(cabinetId, documentId);
    }
}
