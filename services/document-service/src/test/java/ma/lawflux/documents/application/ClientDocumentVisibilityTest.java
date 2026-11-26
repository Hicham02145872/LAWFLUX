package ma.lawflux.documents.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.EnumSet;
import java.util.UUID;
import ma.lawflux.documents.domain.Document;
import ma.lawflux.documents.domain.DocumentAction;
import ma.lawflux.documents.domain.DocumentNotFoundException;
import ma.lawflux.documents.domain.DocumentType;
import ma.lawflux.documents.support.InMemoryDocumentRepository;
import ma.lawflux.documents.support.InMemoryDocumentShareRepository;
import ma.lawflux.documents.support.InMemoryDocumentVersionRepository;
import org.junit.jupiter.api.Test;

class ClientDocumentVisibilityTest {
    @Test
    void onlySharedDocumentsAndTheirVersionsAreVisibleAfterRevocation() {
        UUID cabinet = UUID.randomUUID();
        UUID caseId = UUID.randomUUID();
        UUID user = UUID.randomUUID();
        CallerContext client = new CallerContext(user, cabinet, "Client");
        Document privateDoc = document(cabinet, caseId);
        Document sharedDoc = document(cabinet, caseId);
        var documents = new InMemoryDocumentRepository();
        documents.save(privateDoc);
        documents.save(sharedDoc);
        var shares = new InMemoryDocumentShareRepository();
        shares.register(privateDoc);
        shares.register(sharedDoc);
        shares.share(cabinet, sharedDoc.id(), user, Instant.now());
        DocumentAccessPolicy clientPolicy = (caller, document, shared) -> shared
                ? EnumSet.of(DocumentAction.DOWNLOAD) : EnumSet.noneOf(DocumentAction.class);
        var list = new ListDocumentsUseCase(documents, (c, d) -> true, shares, clientPolicy);
        var versionStore = new InMemoryDocumentVersionRepository();
        versionStore.registerDocument(cabinet, sharedDoc.id());
        var versions = new ListVersionsUseCase(versionStore, shares,
                (c, d) -> true, clientPolicy);

        assertThat(list.execute(client, caseId)).extracting(Document::id).containsExactly(sharedDoc.id());
        assertThatThrownBy(() -> versions.execute(client, privateDoc.id()))
                .isInstanceOf(DocumentNotFoundException.class);
        assertThat(versions.execute(client, sharedDoc.id())).hasSize(1);

        shares.revoke(cabinet, sharedDoc.id(), user, Instant.now());
        assertThat(list.execute(client, caseId)).isEmpty();
        assertThatThrownBy(() -> versions.execute(client, sharedDoc.id()))
                .isInstanceOf(DocumentNotFoundException.class);
    }

    private static Document document(UUID cabinet, UUID caseId) {
        UUID id = UUID.randomUUID();
        return new Document(id, cabinet, caseId, "piece.pdf", DocumentType.PDF,
                10, "private-" + id, UUID.randomUUID(), "Karim", Instant.now());
    }
}
