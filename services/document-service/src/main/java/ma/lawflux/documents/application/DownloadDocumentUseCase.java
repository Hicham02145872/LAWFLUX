package ma.lawflux.documents.application;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import ma.lawflux.documents.domain.DocumentAction;
import ma.lawflux.documents.domain.DocumentNotFoundException;
import ma.lawflux.documents.domain.DocumentShareRepository;
import ma.lawflux.documents.domain.DocumentType;
import ma.lawflux.documents.domain.DocumentVersion;
import ma.lawflux.documents.domain.DocumentVersionRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "lawflux.documents", name = "business-enabled", havingValue = "true")
public class DownloadDocumentUseCase {

    private final DocumentShareRepository shares;
    private final DocumentVersionRepository versions;
    private final DocumentAccessPolicy policy;
    private final DocumentContentReader reader;

    public DownloadDocumentUseCase(
            DocumentShareRepository shares,
            DocumentVersionRepository versions,
            DocumentAccessPolicy policy,
            DocumentContentReader reader) {
        this.shares = shares;
        this.versions = versions;
        this.policy = policy;
        this.reader = reader;
    }

    public DocumentDownload execute(CallerContext caller, UUID documentId) {
        SharingRules.Loaded loaded = SharingRules.load(shares, policy, caller, documentId);
        SharingRules.require(loaded, DocumentAction.DOWNLOAD);

        // Les versions sont triées de la plus récente à la plus ancienne.
        List<DocumentVersion> all = versions.findByDocument(caller.cabinetId(), documentId);
        if (all.isEmpty()) {
            throw new DocumentNotFoundException("Document introuvable");
        }
        DocumentVersion latest = all.get(0);

        InputStream content = reader.open(latest.storageKey());
        return new DocumentDownload(
                downloadName(loaded.document().name(), latest.type()),
                latest.type(), latest.sizeBytes(), content);
    }

    /** L'extension suit la dernière version, qui peut avoir changé de format. */
    private static String downloadName(String documentName, DocumentType type) {
        int dot = documentName.lastIndexOf('.');
        String base = dot > 0 ? documentName.substring(0, dot) : documentName;
        return base + "." + type.name().toLowerCase(Locale.ROOT);
    }
}