package ma.lawflux.documents.application;

import java.util.List;
import java.util.UUID;
import ma.lawflux.documents.domain.Document;
import ma.lawflux.documents.domain.DocumentRepository;
import ma.lawflux.documents.domain.DocumentShareRepository;
import ma.lawflux.documents.domain.DocumentAction;
import ma.lawflux.documents.domain.CaseAccessDeniedException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "lawflux.documents", name = "business-enabled", havingValue = "true")
public class ListDocumentsUseCase {

    private final DocumentRepository repository;
    private final CaseAccessChecker caseAccess;
    private final DocumentShareRepository shares;
    private final DocumentAccessPolicy policy;

    public ListDocumentsUseCase(DocumentRepository repository, CaseAccessChecker caseAccess,
                                DocumentShareRepository shares, DocumentAccessPolicy policy) {
        this.repository = repository;
        this.caseAccess = caseAccess;
        this.shares = shares;
        this.policy = policy;
    }

    public List<Document> execute(CallerContext caller, UUID caseId) {
        UUID cabinetId = caller.cabinetId();
        if (!caseAccess.canAccess(cabinetId, caseId)) {
            throw new CaseAccessDeniedException("Accès au dossier refusé");
        }
        return repository.findByCase(cabinetId, caseId).stream()
                .filter(document -> policy.allowedActions(caller, document,
                        shares.findActiveShare(cabinetId, document.id()).isPresent())
                        .contains(DocumentAction.DOWNLOAD))
                .toList();
    }
}
