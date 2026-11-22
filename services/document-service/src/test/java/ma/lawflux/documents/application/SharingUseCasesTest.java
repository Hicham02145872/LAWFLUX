package ma.lawflux.documents.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import ma.lawflux.documents.domain.Document;
import ma.lawflux.documents.domain.DocumentAccessDeniedException;
import ma.lawflux.documents.domain.DocumentAction;
import ma.lawflux.documents.domain.DocumentNotFoundException;
import ma.lawflux.documents.domain.DocumentType;
import ma.lawflux.documents.domain.NewVersion;
import ma.lawflux.documents.support.InMemoryDocumentShareRepository;
import ma.lawflux.documents.support.InMemoryDocumentStorage;
import ma.lawflux.documents.support.InMemoryDocumentVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SharingUseCasesTest {

    private final UUID cabinetId = UUID.randomUUID();
    private final UUID otherCabinetId = UUID.randomUUID();
    private final UUID documentId = UUID.randomUUID();
    private final UUID lawyerId = UUID.randomUUID();
    private final UUID clientId = UUID.randomUUID();

    private final CallerContext lawyer = new CallerContext(lawyerId, cabinetId, "Karim");
    private final CallerContext client = new CallerContext(clientId, cabinetId, "Client SA");
    private final CallerContext foreign =
            new CallerContext(UUID.randomUUID(), otherCabinetId, "Autre cabinet");

    private final InMemoryDocumentShareRepository shares = new InMemoryDocumentShareRepository();
    private final InMemoryDocumentVersionRepository versions = new InMemoryDocumentVersionRepository();
    private final InMemoryDocumentStorage storage = new InMemoryDocumentStorage();
    private final List<String> opened = new ArrayList<>();

    // Avocat : tous les droits. Client : télécharger seulement si le document est partagé.
    private final DocumentAccessPolicy policy = (caller, document, shared) -> {
        if (caller.userId().equals(clientId)) {
            return shared ? EnumSet.of(DocumentAction.DOWNLOAD) : EnumSet.noneOf(DocumentAction.class);
        }
        return EnumSet.of(DocumentAction.SHARE, DocumentAction.UNSHARE, DocumentAction.DOWNLOAD);
    };

    private final DocumentContentReader reader = storageKey -> {
        opened.add(storageKey);
        byte[] content = storage.files.get(storageKey);
        if (content == null) {
            throw new IllegalStateException("contenu absent : " + storageKey);
        }
        return new ByteArrayInputStream(content);
    };

    private final GetSharingUseCase get = new GetSharingUseCase(shares, policy);
    private final ShareDocumentUseCase share = new ShareDocumentUseCase(shares, policy);
    private final UnshareDocumentUseCase unshare = new UnshareDocumentUseCase(shares, policy);
    private final DownloadDocumentUseCase download =
            new DownloadDocumentUseCase(shares, versions, policy, reader);

    @BeforeEach
    void setUp() {
        Document document = new Document(documentId, cabinetId, UUID.randomUUID(), "Contrat de bail.pdf",
                DocumentType.PDF, 10, "initial-" + documentId, lawyerId, "Karim",
                Instant.parse("2026-11-01T10:00:00Z"));
        shares.register(document);
        versions.registerDocument(cabinetId, documentId);
        storage.files.put("initial-" + documentId, "contenu v1".getBytes(StandardCharsets.UTF_8));
    }

    private static String text(DocumentDownload result) throws Exception {
        try (InputStream content = result.content()) {
            return new String(content.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void l_avocat_voit_un_document_non_partage_avec_partager_et_telecharger() {
        SharingStatus status = get.execute(lawyer, documentId);

        assertThat(status.shared()).isFalse();
        assertThat(status.allowedActions())
                .containsExactlyInAnyOrder(DocumentAction.SHARE, DocumentAction.DOWNLOAD);
    }

    @Test
    void partager_rend_le_document_partage_et_propose_de_retirer_le_partage() {
        SharingStatus status = share.execute(lawyer, documentId);

        assertThat(status.shared()).isTrue();
        assertThat(status.allowedActions())
                .containsExactlyInAnyOrder(DocumentAction.UNSHARE, DocumentAction.DOWNLOAD);
        assertThat(get.execute(lawyer, documentId).shared()).isTrue();
        assertThat(shares.shareCalls).isEqualTo(1);
    }

    @Test
    void partager_deux_fois_ne_cree_qu_un_partage() {
        share.execute(lawyer, documentId);
        share.execute(lawyer, documentId);

        assertThat(shares.shareCalls).isEqualTo(1);
    }

    @Test
    void retirer_le_partage_est_idempotent() {
        share.execute(lawyer, documentId);

        SharingStatus status = unshare.execute(lawyer, documentId);
        unshare.execute(lawyer, documentId);

        assertThat(status.shared()).isFalse();
        assertThat(status.allowedActions())
                .containsExactlyInAnyOrder(DocumentAction.SHARE, DocumentAction.DOWNLOAD);
        assertThat(shares.revokeCalls).isEqualTo(1);
    }

    @Test
    void un_client_ne_voit_pas_un_document_non_partage() {
        assertThatThrownBy(() -> get.execute(client, documentId))
                .isInstanceOf(DocumentNotFoundException.class);
        assertThatThrownBy(() -> share.execute(client, documentId))
                .isInstanceOf(DocumentNotFoundException.class);
        assertThatThrownBy(() -> download.execute(client, documentId))
                .isInstanceOf(DocumentNotFoundException.class);

        assertThat(opened).isEmpty();
        assertThat(shares.shareCalls).isZero();
    }

    @Test
    void un_client_telecharge_un_document_partage_mais_ne_peut_ni_partager_ni_retirer() throws Exception {
        share.execute(lawyer, documentId);

        SharingStatus status = get.execute(client, documentId);
        assertThat(status.shared()).isTrue();
        assertThat(status.allowedActions()).containsExactly(DocumentAction.DOWNLOAD);
        assertThatThrownBy(() -> share.execute(client, documentId))
                .isInstanceOf(DocumentAccessDeniedException.class);
        assertThatThrownBy(() -> unshare.execute(client, documentId))
                .isInstanceOf(DocumentAccessDeniedException.class);

        assertThat(text(download.execute(client, documentId))).isEqualTo("contenu v1");
        assertThat(shares.revokeCalls).isZero();
    }

    @Test
    void un_client_ne_peut_plus_telecharger_apres_le_retrait_du_partage() {
        share.execute(lawyer, documentId);
        unshare.execute(lawyer, documentId);

        assertThatThrownBy(() -> download.execute(client, documentId))
                .isInstanceOf(DocumentNotFoundException.class);
        assertThat(opened).isEmpty();
    }

    @Test
    void le_telechargement_renvoie_la_derniere_version_avec_le_bon_nom() throws Exception {
        storage.files.put("v2-key", "contenu v2".getBytes(StandardCharsets.UTF_8));
        versions.addVersion(new NewVersion(cabinetId, documentId, DocumentType.DOCX, 20, "v2-key",
                lawyerId, "Karim", Instant.parse("2026-11-08T10:00:00Z")));

        DocumentDownload result = download.execute(lawyer, documentId);

        assertThat(result.fileName()).isEqualTo("Contrat de bail.docx");
        assertThat(result.type()).isEqualTo(DocumentType.DOCX);
        assertThat(result.sizeBytes()).isEqualTo(20);
        assertThat(text(result)).isEqualTo("contenu v2");
        assertThat(opened).containsExactly("v2-key");
    }

    @Test
    void un_autre_cabinet_ne_voit_pas_le_document() {
        assertThatThrownBy(() -> get.execute(foreign, documentId))
                .isInstanceOf(DocumentNotFoundException.class);
        assertThatThrownBy(() -> share.execute(foreign, documentId))
                .isInstanceOf(DocumentNotFoundException.class);
        assertThatThrownBy(() -> unshare.execute(foreign, documentId))
                .isInstanceOf(DocumentNotFoundException.class);
        assertThatThrownBy(() -> download.execute(foreign, documentId))
                .isInstanceOf(DocumentNotFoundException.class);

        assertThat(opened).isEmpty();
        assertThat(shares.shareCalls).isZero();
        assertThat(shares.revokeCalls).isZero();
    }
}