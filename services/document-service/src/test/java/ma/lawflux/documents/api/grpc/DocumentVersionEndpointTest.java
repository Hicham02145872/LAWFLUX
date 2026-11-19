package ma.lawflux.documents.api.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.google.protobuf.ByteString;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import ma.lawflux.contracts.documents.v1.AddVersionMetadata;
import ma.lawflux.contracts.documents.v1.AddVersionReply;
import ma.lawflux.contracts.documents.v1.AddVersionRequest;
import ma.lawflux.contracts.documents.v1.ListVersionsReply;
import ma.lawflux.contracts.documents.v1.ListVersionsRequest;
import ma.lawflux.contracts.documents.v1.VersionSummary;
import ma.lawflux.documents.application.AddVersionUseCase;
import ma.lawflux.documents.application.CallerContext;
import ma.lawflux.documents.application.ListDocumentsUseCase;
import ma.lawflux.documents.application.ListVersionsUseCase;
import ma.lawflux.documents.application.UploadDocumentUseCase;
import ma.lawflux.documents.support.InMemoryDocumentRepository;
import ma.lawflux.documents.support.InMemoryDocumentStorage;
import ma.lawflux.documents.support.InMemoryDocumentVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

class DocumentVersionEndpointTest {

    private final UUID cabinetId = UUID.randomUUID();
    private final UUID otherCabinetId = UUID.randomUUID();
    private final UUID documentId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    private final InMemoryDocumentRepository documents = new InMemoryDocumentRepository();
    private final InMemoryDocumentStorage storage = new InMemoryDocumentStorage();
    private final InMemoryDocumentVersionRepository versions = new InMemoryDocumentVersionRepository();
    private final DataSize maxSize = DataSize.ofBytes(100);

    private DocumentServiceEndpoint endpointWith(UUID callerCabinet, Duration idleTimeout) {
        return new DocumentServiceEndpoint(
                new ListDocumentsUseCase(documents),
                new UploadDocumentUseCase(documents, storage, (cabinet, caze) -> true, maxSize),
                new ListVersionsUseCase(versions),
                new AddVersionUseCase(versions, storage, maxSize),
                () -> new CallerContext(userId, callerCabinet, "Sara"),
                maxSize,
                idleTimeout);
    }

    private final DocumentServiceEndpoint endpoint = endpointWith(cabinetId, Duration.ofSeconds(30));

    @BeforeEach
    void setUp() {
        versions.registerDocument(cabinetId, documentId);
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

    private static AddVersionRequest metadata(UUID documentId, String fileName) {
        return AddVersionRequest.newBuilder()
                .setMetadata(AddVersionMetadata.newBuilder()
                        .setDocumentId(documentId.toString())
                        .setFileName(fileName))
                .build();
    }

    private static AddVersionRequest chunk(int size) {
        return AddVersionRequest.newBuilder()
                .setChunk(ByteString.copyFrom(new byte[size]))
                .build();
    }

    private static Status.Code code(Throwable t) {
        return Status.fromThrowable(t).getCode();
    }

    @Test
    void liste_les_versions_du_plus_recent_au_plus_ancien() {
        Recorder<AddVersionReply> added = new Recorder<>();
        StreamObserver<AddVersionRequest> stream = endpoint.addVersion(added);
        stream.onNext(metadata(documentId, "v2.pdf"));
        stream.onNext(chunk(20));
        stream.onCompleted();

        Recorder<ListVersionsReply> response = new Recorder<>();
        endpoint.listVersions(
                ListVersionsRequest.newBuilder().setDocumentId(documentId.toString()).build(), response);

        assertThat(response.error).isNull();
        assertThat(response.values.get(0).getVersionsList())
                .extracting(VersionSummary::getNumber)
                .containsExactly(2, 1);
    }

    @Test
    void refuse_de_lister_les_versions_d_un_document_d_un_autre_cabinet() {
        Recorder<ListVersionsReply> response = new Recorder<>();

        endpointWith(otherCabinetId, Duration.ofSeconds(30)).listVersions(
                ListVersionsRequest.newBuilder().setDocumentId(documentId.toString()).build(), response);

        assertThat(code(response.error)).isEqualTo(Status.Code.NOT_FOUND);
        assertThat(response.values).isEmpty();
    }

    @Test
    void refuse_un_identifiant_de_document_invalide() {
        Recorder<ListVersionsReply> response = new Recorder<>();

        endpoint.listVersions(ListVersionsRequest.newBuilder().setDocumentId("pas-un-uuid").build(), response);

        assertThat(code(response.error)).isEqualTo(Status.Code.INVALID_ARGUMENT);
    }

    @Test
    void ajoute_une_version_recue_en_plusieurs_morceaux_avec_le_contexte_authentifie() {
        Recorder<AddVersionReply> response = new Recorder<>();
        StreamObserver<AddVersionRequest> stream = endpoint.addVersion(response);

        stream.onNext(metadata(documentId, "v2.docx"));
        stream.onNext(chunk(40));
        stream.onNext(chunk(30));
        stream.onCompleted();

        assertThat(response.error).isNull();
        assertThat(response.completed).isTrue();
        assertThat(response.terminals.get()).isEqualTo(1);
        VersionSummary version = response.values.get(0).getVersion();
        assertThat(version.getNumber()).isEqualTo(2);
        assertThat(version.getAuthor()).isEqualTo("Sara");
        assertThat(version.getType()).isEqualTo("DOCX");
        assertThat(version.getSizeBytes()).isEqualTo(70);
        assertThat(storage.files).hasSize(1);
    }

    @Test
    void refuse_d_ajouter_une_version_a_un_document_d_un_autre_cabinet_sans_rien_stocker() {
        Recorder<AddVersionReply> response = new Recorder<>();
        StreamObserver<AddVersionRequest> stream =
                endpointWith(otherCabinetId, Duration.ofSeconds(30)).addVersion(response);

        stream.onNext(metadata(documentId, "v2.pdf"));
        stream.onNext(chunk(10));
        stream.onCompleted();

        assertThat(code(response.error)).isEqualTo(Status.Code.NOT_FOUND);
        assertThat(response.terminals.get()).isEqualTo(1);
        assertThat(storage.files).isEmpty();
        assertThat(versions.findByDocument(cabinetId, documentId)).hasSize(1);
    }

    @Test
    void refuse_si_la_taille_cumulee_depasse_la_limite() {
        Recorder<AddVersionReply> response = new Recorder<>();
        StreamObserver<AddVersionRequest> stream = endpoint.addVersion(response);

        stream.onNext(metadata(documentId, "gros.pdf"));
        stream.onNext(chunk(60));
        stream.onNext(chunk(60));
        stream.onCompleted();

        assertThat(code(response.error)).isEqualTo(Status.Code.INVALID_ARGUMENT);
        assertThat(response.terminals.get()).isEqualTo(1);
        assertThat(storage.files).isEmpty();
        assertThat(versions.findByDocument(cabinetId, documentId)).hasSize(1);
    }

    @Test
    void coupe_un_ajout_inactif_et_nettoie() {
        DocumentServiceEndpoint shortIdle = endpointWith(cabinetId, Duration.ofMillis(200));
        Recorder<AddVersionReply> response = new Recorder<>();
        StreamObserver<AddVersionRequest> stream = shortIdle.addVersion(response);

        stream.onNext(metadata(documentId, "v2.pdf"));
        stream.onNext(chunk(10));

        await().atMost(Duration.ofSeconds(3)).until(() -> response.error != null);
        assertThat(code(response.error)).isEqualTo(Status.Code.DEADLINE_EXCEEDED);

        stream.onNext(chunk(10));
        stream.onCompleted();
        assertThat(response.terminals.get()).isEqualTo(1);
        assertThat(storage.files).isEmpty();
        assertThat(versions.findByDocument(cabinetId, documentId)).hasSize(1);
    }
}