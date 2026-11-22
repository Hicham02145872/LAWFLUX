package ma.lawflux.documents.application;

import java.util.List;
import java.util.UUID;
import ma.lawflux.documents.domain.Document;
import ma.lawflux.documents.domain.DocumentRepository;
import ma.lawflux.documents.domain.CaseAccessDeniedException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "lawflux.documents", name = "business-enabled", havingValue = "true")
public class ListDocumentsUseCase {

    private final DocumentRepository repository;
    private final CaseAccessChecker caseAccess;

    public ListDocumentsUseCase(DocumentRepository repository, CaseAccessChecker caseAccess) {
        this.repository = repository;
        this.caseAccess = caseAccess;
    }

    public List<Document> execute(UUID cabinetId, UUID caseId) {
        if (!caseAccess.canAccess(cabinetId, caseId)) {
            throw new CaseAccessDeniedException("Accès au dossier refusé");
        }
        return repository.findByCase(cabinetId, caseId);
    }
}
