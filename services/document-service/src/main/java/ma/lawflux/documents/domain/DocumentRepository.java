package ma.lawflux.documents.domain;

import java.util.List;
import java.util.UUID;

public interface DocumentRepository {
    Document save(Document document);
    List<Document> findByCase(UUID cabinetId, UUID caseId);
}