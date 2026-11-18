package ma.lawflux.documents.application;

import java.util.List;
import java.util.UUID;
import ma.lawflux.documents.domain.DocumentNotFoundException;
import ma.lawflux.documents.domain.DocumentVersion;
import ma.lawflux.documents.domain.DocumentVersionRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "lawflux.documents", name = "business-enabled", havingValue = "true")
public class ListVersionsUseCase {

    private final DocumentVersionRepository versions;

    public ListVersionsUseCase(DocumentVersionRepository versions) {
        this.versions = versions;
    }

    public List<DocumentVersion> execute(UUID cabinetId, UUID documentId) {
        if (!versions.documentExists(cabinetId, documentId)) {
            throw new DocumentNotFoundException("Document introuvable");
        }
        return versions.findByDocument(cabinetId, documentId);
    }
}