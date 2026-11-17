package ma.lawflux.documents.support;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import ma.lawflux.documents.domain.Document;
import ma.lawflux.documents.domain.DocumentRepository;

public class InMemoryDocumentRepository implements DocumentRepository {

    public final List<Document> documents = new ArrayList<>();

    @Override
    public Document save(Document document) {
        documents.add(document);
        return document;
    }

    @Override
    public List<Document> findByCase(UUID cabinetId, UUID caseId) {
        return documents.stream()
                .filter(d -> d.cabinetId().equals(cabinetId) && d.caseId().equals(caseId))
                .sorted(Comparator.comparing(Document::createdAt).reversed())
                .toList();
    }
}