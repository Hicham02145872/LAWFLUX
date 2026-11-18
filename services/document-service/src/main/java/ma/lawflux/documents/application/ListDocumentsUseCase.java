package ma.lawflux.documents.application;

import java.util.List;
import java.util.UUID;
import ma.lawflux.documents.domain.Document;
import ma.lawflux.documents.domain.DocumentRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "lawflux.documents", name = "business-enabled", havingValue = "true")
public class ListDocumentsUseCase {

    private final DocumentRepository repository;

    public ListDocumentsUseCase(DocumentRepository repository) {
        this.repository = repository;
    }

    public List<Document> execute(UUID cabinetId, UUID caseId) {
        return repository.findByCase(cabinetId, caseId);
    }
}