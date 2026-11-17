package ma.lawflux.documents.api.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.ByteString;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import ma.lawflux.contracts.documents.v1.DocumentSummary;
import ma.lawflux.contracts.documents.v1.ListDocumentsReply;
import ma.lawflux.contracts.documents.v1.ListDocumentsRequest;
import ma.lawflux.contracts.documents.v1.UploadDocumentReply;
import ma.lawflux.contracts.documents.v1.UploadDocumentRequest;
import ma.lawflux.contracts.documents.v1.UploadMetadata;
import ma.lawflux.documents.application.CallerContext;
import ma.lawflux.documents.application.ListDocumentsUseCase;
import ma.lawflux.documents.application.UploadDocumentUseCase;
import ma.lawflux.documents.domain.Document;
import ma.lawflux.documents.domain.DocumentType;
import ma.lawflux.documents.support.InMemoryDocumentRepository;
import ma.lawflux.documents.support.InMemoryDocumentStorage;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

class DocumentServiceEndpointTest {

    private final UUID cabinetId = UUID.randomUUID();
    private final UUID otherCabinetId = UUID.randomUUID();
    private final UUID caseId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    private final InMemoryDocumentRepository repository = new InMemoryDocumentRepository();
    private final InMemoryDocumentStorage storage = new InMemoryDocumentStorage();
    private final DataSize maxSize = DataSize.ofBytes(100);

    private final DocumentServiceEndpoint endpoint = new DocumentServiceEndpoint(
            new ListDocumentsUseCase(repository),
            new UploadDocumentUseCase(repository, storage, maxSize),
            () -> new CallerContext(userId, cabinetId, "Karim"),
            maxSize);

    private static class Recorder<T> implements StreamObserver<T> {
        final List<T> values = new ArrayList<>();
        Throwable error;
        boolean completed;

        @Override public void onNext(T value) { values.add(value); }
        @Override public void onError(Throwable t) { error = t; }
        @Override public void onCompleted() { completed = true; }
    }

    private static UploadDocumentRequest metadata(UUID caseId, String fileName) {
        return UploadDocumentRequest.newBuilder()
                .setMetadata(UploadMetadata.newBuilder()
                        .setCaseId(caseId.toString())
                        .setFileName(fileName))
                .build();
    }

    private static UploadDocumentRequest chunk(int size) {
        return UploadDocumentRequest.newBuilder()
                .setChunk(ByteString.copyFrom(new byte[size]))
                .build();
    }

    private static Status.Code code(Throwable t) {
        return Status.fromThrowable(t).getCode();
    }

    private Document document(UUID cabinet, String name) {
        return new Document(UUID.randomUUID(), cabinet, caseId, name, DocumentType.PDF,
                10, "key-" + name, userId, "Karim", Instant.now());
    }

    @Test
    void importe_un_fichier_recu_en_plusieurs_morceaux_avec_le_contexte_authentifie() {
        Recorder<UploadDocumentReply> response = new Recorder<>();
        StreamObserver<UploadDocumentRequest> stream = endpoint.uploadDocument(response);

        stream.onNext(metadata(caseId, "Contrat de bail.pdf"));
        stream.onNext(chunk(40));
        stream.onNext(chunk(30));
        stream.onCompleted();

        assertThat(response.error).isNull();
        assertThat(response.completed).isTrue();
        assertThat(repository.documents).hasSize(1);
        Document saved = repository.documents.get(0);
        assertThat(saved.sizeBytes()).isEqualTo(70);
        assertThat(saved.cabinetId()).isEqualTo(cabinetId);
        assertThat(saved.uploadedBy()).isEqualTo(userId);
        assertThat(saved.authorName()).isEqualTo("Karim");
        assertThat(storage.files.values().iterator().next()).hasSize(70);
    }

    @Test
    void refuse_si_la_taille_cumulee_depasse_la_limite() {
        Recorder<UploadDocumentReply> response = new Recorder<>();
        StreamObserver<UploadDocumentRequest> stream = endpoint.uploadDocument(response);

        stream.onNext(metadata(caseId, "gros.pdf"));
        stream.onNext(chunk(60));
        stream.onNext(chunk(60));
        stream.onCompleted();

        assertThat(code(response.error)).isEqualTo(Status.Code.INVALID_ARGUMENT);
        assertThat(response.completed).isFalse();
        assertThat(repository.documents).isEmpty();
        assertThat(storage.files).isEmpty();
    }

    @Test
    void refuse_un_morceau_recu_avant_les_metadonnees() {
        Recorder<UploadDocumentReply> response = new Recorder<>();
        StreamObserver<UploadDocumentRequest> stream = endpoint.uploadDocument(response);

        stream.onNext(chunk(10));
        stream.onCompleted();

        assertThat(code(response.error)).isEqualTo(Status.Code.INVALID_ARGUMENT);
        assertThat(repository.documents).isEmpty();
    }

    @Test
    void refuse_un_type_de_fichier_non_pris_en_charge() {
        Recorder<UploadDocumentReply> response = new Recorder<>();
        StreamObserver<UploadDocumentRequest> stream = endpoint.uploadDocument(response);

        stream.onNext(metadata(caseId, "notes.txt"));
        stream.onNext(chunk(10));
        stream.onCompleted();

        assertThat(code(response.error)).isEqualTo(Status.Code.INVALID_ARGUMENT);
        assertThat(storage.files).isEmpty();
        assertThat(repository.documents).isEmpty();
    }

    @Test
    void ne_stocke_rien_si_le_client_annule_en_cours_de_route() {
        Recorder<UploadDocumentReply> response = new Recorder<>();
        StreamObserver<UploadDocumentRequest> stream = endpoint.uploadDocument(response);

        stream.onNext(metadata(caseId, "contrat.pdf"));
        stream.onNext(chunk(10));
        stream.onError(new RuntimeException("annulé par le client"));

        assertThat(response.completed).isFalse();
        assertThat(repository.documents).isEmpty();
        assertThat(storage.files).isEmpty();
    }

    @Test
    void liste_uniquement_les_documents_du_cabinet_appelant() {
        repository.save(document(cabinetId, "a.pdf"));
        repository.save(document(otherCabinetId, "secret.pdf"));
        Recorder<ListDocumentsReply> response = new Recorder<>();

        endpoint.listDocuments(
                ListDocumentsRequest.newBuilder().setCaseId(caseId.toString()).build(), response);

        assertThat(response.error).isNull();
        assertThat(response.values.get(0).getDocumentsList())
                .extracting(DocumentSummary::getName)
                .containsExactly("a.pdf");
    }

    @Test
    void refuse_un_identifiant_de_dossier_invalide() {
        Recorder<ListDocumentsReply> response = new Recorder<>();

        endpoint.listDocuments(
                ListDocumentsRequest.newBuilder().setCaseId("pas-un-uuid").build(), response);

        assertThat(code(response.error)).isEqualTo(Status.Code.INVALID_ARGUMENT);
    }
}