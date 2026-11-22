package ma.lawflux.documents.api.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import ma.lawflux.contracts.documents.v1.DownloadDocumentReply;
import ma.lawflux.contracts.documents.v1.DownloadHeader;
import ma.lawflux.contracts.documents.v1.SharingAction;
import ma.lawflux.contracts.documents.v1.SharingReply;
import ma.lawflux.contracts.documents.v1.SharingRequest;
import ma.lawflux.documents.application.CallerContext;
import ma.lawflux.documents.application.DocumentAccessPolicy;
import ma.lawflux.documents.application.DocumentContentReader;
import ma.lawflux.documents.application.DownloadDocumentUseCase;
import ma.lawflux.documents.application.GetSharingUseCase;
import ma.lawflux.documents.application.ShareDocumentUseCase;
import ma.lawflux.documents.application.UnshareDocumentUseCase;
import ma.lawflux.documents.domain.Document;
import ma.lawflux.documents.domain.DocumentAction;
import ma.lawflux.documents.domain.DocumentType;
import ma.lawflux.documents.domain.NewVersion;
import ma.lawflux.documents.support.InMemoryDocumentShareRepository;
import ma.lawflux.documents.support.InMemoryDocumentStorage;
import ma.lawflux.documents.support.InMemoryDocumentVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DocumentSharingEndpointTest {

    private final UUID cabinetId = UUID.randomUUID();
    private final UUID otherCabinetId = UUID.randomUUID();
    private final UUID documentId = UUID.randomUUID();
    private final UUID lawyerId = UUID.randomUUID();
    private final UUID clientId = UUID.randomUUID();

    private final CallerContext lawyer = new CallerContext(lawyerId, cabinetId, "Karim");
    private final CallerContext client = new CallerContext(clientId, cabinetId, "Client SA");
    private final CallerContext foreign =
            new CallerContext(UUID.randomUUID(), otherCabinetId, "Autre cabinet");
    private final AtomicReference<CallerContext> current = new AtomicReference<>(lawyer);

    private final InMemoryDocumentShareRepository shares = new InMemoryDocumentShareRepository();
    private final InMemoryDocumentVersionRepository versions = new InMemoryDocumentVersionRepository();
    private final InMemoryDocumentStorage storage = new InMemoryDocumentStorage();
    private final AtomicReference<TrackingStream> opened = new AtomicReference<>();

    // Avocat : tous les droits. Client : télécharger seulement si le document est partagé.
    private final DocumentAccessPolicy policy = (caller, document, shared) -> {
        if (caller.userId().equals(clientId)) {
            return shared ? EnumSet.of(DocumentAction.DOWNLOAD) : EnumSet.noneOf(DocumentAction.class);
        }
        return EnumSet.of(DocumentAction.SHARE, DocumentAction.UNSHARE, DocumentAction.DOWNLOAD);
    };

    private final DocumentContentReader reader = storageKey -> {
        byte[] content = storage.files.get(storageKey);
        if (content == null) {
            throw new IllegalStateException("contenu absent : " + storageKey);
        }
        TrackingStream stream = new TrackingStream(content);
        opened.set(stream);
        return stream;
    };

    private final DocumentSharingEndpoint endpoint = new DocumentSharingEndpoint(
            new GetSharingUseCase(shares, policy),
            new ShareDocumentUseCase(shares, policy),
            new UnshareDocumentUseCase(shares, policy),
            new DownloadDocumentUseCase(shares, versions, policy, reader),
            current::get,
            Duration.ofSeconds(30));

    private static class TrackingStream extends ByteArrayInputStream {
        volatile boolean closed;

        TrackingStream(byte[] data) {
            super(data);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }

    private static class Recorder<T> implements StreamObserver<T> {
        final List<T> values = new CopyOnWriteArrayList<>();
        final AtomicInteger terminals = new AtomicInteger();
        volatile Throwable error;
        volatile boolean completed;

        @Override public void onNext(T value) { values.add(value); }

        @Override public void onError(Throwable t) {
            error = t;
            terminals.incrementAndGet();
        }

        @Override public void onCompleted() {
            completed = true;
            terminals.incrementAndGet();
        }
    }

    @BeforeEach
    void setUp() {
        shares.register(new Document(documentId, cabinetId, UUID.randomUUID(), "Contrat de bail.pdf",
                DocumentType.PDF, 10, "initial-" + documentId, lawyerId, "Karim",
                Instant.parse("2026-11-01T10:00:00Z")));
        versions.registerDocument(cabinetId, documentId);
        storage.files.put("initial-" + documentId, "contenu v1".getBytes(StandardCharsets.UTF_8));
    }

    private static SharingRequest request(UUID id) {
        return SharingRequest.newBuilder().setDocumentId(id.toString()).build();
    }

    private static Status.Code code(Throwable t) {
        return Status.fromThrowable(t).getCode();
    }

    private SharingReply getAs(CallerContext caller) {
        current.set(caller);
        Recorder<SharingReply> response = new Recorder<>();
        endpoint.getSharing(request(documentId), response);
        assertThat(response.error).isNull();
        return response.values.get(0);
    }

    @Test
    void renvoie_l_etat_de_partage_avec_les_actions_autorisees() {
        SharingReply reply = getAs(lawyer);

        assertThat(reply.getDocumentId()).isEqualTo(documentId.toString());
        assertThat(reply.getName()).isEqualTo("Contrat de bail.pdf");
        assertThat(reply.getShared()).isFalse();
        assertThat(reply.getAllowedActionsList()).containsExactlyInAnyOrder(
                SharingAction.SHARING_ACTION_SHARE, SharingAction.SHARING_ACTION_DOWNLOAD);
    }

    @Test
    void partage_puis_retire_le_partage() {
        Recorder<SharingReply> shared = new Recorder<>();
        endpoint.shareDocument(request(documentId), shared);

        assertThat(shared.error).isNull();
        assertThat(shared.terminals.get()).isEqualTo(1);
        assertThat(shared.values.get(0).getShared()).isTrue();
        assertThat(shared.values.get(0).getAllowedActionsList()).containsExactlyInAnyOrder(
                SharingAction.SHARING_ACTION_UNSHARE, SharingAction.SHARING_ACTION_DOWNLOAD);

        Recorder<SharingReply> unshared = new Recorder<>();
        endpoint.unshareDocument(request(documentId), unshared);

        assertThat(unshared.error).isNull();
        assertThat(unshared.values.get(0).getShared()).isFalse();
        assertThat(unshared.values.get(0).getAllowedActionsList()).containsExactlyInAnyOrder(
                SharingAction.SHARING_ACTION_SHARE, SharingAction.SHARING_ACTION_DOWNLOAD);
    }

    @Test
    void un_client_ne_peut_pas_partager() {
        endpoint.shareDocument(request(documentId), new Recorder<>());
        current.set(client);
        Recorder<SharingReply> response = new Recorder<>();

        endpoint.shareDocument(request(documentId), response);

        assertThat(code(response.error)).isEqualTo(Status.Code.PERMISSION_DENIED);
        assertThat(response.terminals.get()).isEqualTo(1);
    }

    @Test
    void un_client_ne_voit_pas_un_document_non_partage() {
        current.set(client);
        Recorder<SharingReply> response = new Recorder<>();

        endpoint.getSharing(request(documentId), response);

        assertThat(code(response.error)).isEqualTo(Status.Code.NOT_FOUND);
        assertThat(response.values).isEmpty();
    }

    @Test
    void un_autre_cabinet_recoit_not_found_pour_toutes_les_operations() {
        current.set(foreign);
        Recorder<SharingReply> get = new Recorder<>();
        Recorder<SharingReply> share = new Recorder<>();
        Recorder<SharingReply> unshare = new Recorder<>();
        Recorder<DownloadDocumentReply> download = new Recorder<>();

        endpoint.getSharing(request(documentId), get);
        endpoint.shareDocument(request(documentId), share);
        endpoint.unshareDocument(request(documentId), unshare);
        endpoint.downloadDocument(request(documentId), download);

        assertThat(code(get.error)).isEqualTo(Status.Code.NOT_FOUND);
        assertThat(code(share.error)).isEqualTo(Status.Code.NOT_FOUND);
        assertThat(code(unshare.error)).isEqualTo(Status.Code.NOT_FOUND);
        assertThat(code(download.error)).isEqualTo(Status.Code.NOT_FOUND);
        assertThat(download.values).isEmpty();
        assertThat(opened.get()).isNull();
    }

    @Test
    void refuse_un_identifiant_de_document_invalide() {
        Recorder<SharingReply> get = new Recorder<>();
        Recorder<DownloadDocumentReply> download = new Recorder<>();
        SharingRequest invalid = SharingRequest.newBuilder().setDocumentId("pas-un-uuid").build();

        endpoint.getSharing(invalid, get);
        endpoint.downloadDocument(invalid, download);

        assertThat(code(get.error)).isEqualTo(Status.Code.INVALID_ARGUMENT);
        assertThat(code(download.error)).isEqualTo(Status.Code.INVALID_ARGUMENT);
    }

    @Test
    void telecharge_la_derniere_version_en_morceaux_sans_cle_de_stockage() throws Exception {
        byte[] content = new byte[600_000];
        new Random(42).nextBytes(content);
        storage.files.put("v2-key", content);
        versions.addVersion(new NewVersion(cabinetId, documentId, DocumentType.DOCX, content.length,
                "v2-key", lawyerId, "Karim", Instant.parse("2026-11-08T10:00:00Z")));
        Recorder<DownloadDocumentReply> response = new Recorder<>();

        endpoint.downloadDocument(request(documentId), response);

        assertThat(response.error).isNull();
        assertThat(response.completed).isTrue();
        assertThat(response.terminals.get()).isEqualTo(1);
        assertThat(response.values.get(0).getPayloadCase())
                .isEqualTo(DownloadDocumentReply.PayloadCase.HEADER);
        DownloadHeader header = response.values.get(0).getHeader();
        assertThat(header.getFileName()).isEqualTo("Contrat de bail.docx");
        assertThat(header.getType()).isEqualTo("DOCX");
        assertThat(header.getSizeBytes()).isEqualTo(content.length);

        ByteArrayOutputStream received = new ByteArrayOutputStream();
        for (DownloadDocumentReply reply : response.values.subList(1, response.values.size())) {
            assertThat(reply.getPayloadCase()).isEqualTo(DownloadDocumentReply.PayloadCase.CHUNK);
            assertThat(reply.getChunk().size()).isLessThanOrEqualTo(ChunkedDownload.CHUNK_SIZE);
            reply.getChunk().writeTo(received);
        }
        assertThat(response.values).hasSize(1 + 3);
        assertThat(received.toByteArray()).isEqualTo(content);
        assertThat(opened.get().closed).isTrue();
    }

    @Test
    void un_client_ne_telecharge_pas_un_document_non_partage() {
        current.set(client);
        Recorder<DownloadDocumentReply> response = new Recorder<>();

        endpoint.downloadDocument(request(documentId), response);

        assertThat(code(response.error)).isEqualTo(Status.Code.NOT_FOUND);
        assertThat(response.values).isEmpty();
        assertThat(opened.get()).isNull();
    }

    @Test
    void un_client_telecharge_un_document_partage() throws Exception {
        endpoint.shareDocument(request(documentId), new Recorder<>());
        current.set(client);
        Recorder<DownloadDocumentReply> response = new Recorder<>();

        endpoint.downloadDocument(request(documentId), response);

        assertThat(response.error).isNull();
        assertThat(response.completed).isTrue();
        ByteArrayOutputStream received = new ByteArrayOutputStream();
        for (DownloadDocumentReply reply : response.values.subList(1, response.values.size())) {
            reply.getChunk().writeTo(received);
        }
        assertThat(received.toString(StandardCharsets.UTF_8)).isEqualTo("contenu v1");
    }

    @Test
    void ne_revele_pas_les_details_d_une_erreur_interne() {
        storage.files.remove("initial-" + documentId);
        Recorder<DownloadDocumentReply> response = new Recorder<>();

        endpoint.downloadDocument(request(documentId), response);

        Status status = Status.fromThrowable(response.error);
        assertThat(status.getCode()).isEqualTo(Status.Code.INTERNAL);
        assertThat(status.getDescription()).isEqualTo("Erreur interne");
        assertThat(response.values).isEmpty();
    }
}