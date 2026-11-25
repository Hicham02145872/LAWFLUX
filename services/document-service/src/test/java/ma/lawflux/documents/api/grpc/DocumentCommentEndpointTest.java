package ma.lawflux.documents.api.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import ma.lawflux.contracts.documents.v1.AddCommentReply;
import ma.lawflux.contracts.documents.v1.AddCommentRequest;
import ma.lawflux.contracts.documents.v1.CommentSummary;
import ma.lawflux.contracts.documents.v1.CommentVisibility;
import ma.lawflux.contracts.documents.v1.ListCommentsReply;
import ma.lawflux.contracts.documents.v1.ListCommentsRequest;
import ma.lawflux.documents.application.AddCommentUseCase;
import ma.lawflux.documents.application.CallerContext;
import ma.lawflux.documents.application.CommentAccessPolicy;
import ma.lawflux.documents.application.CommentRight;
import ma.lawflux.documents.application.GetCommentOptionsUseCase;
import ma.lawflux.documents.application.ListCommentsUseCase;
import ma.lawflux.documents.domain.Document;
import ma.lawflux.documents.domain.DocumentType;
import ma.lawflux.documents.support.InMemoryDocumentCommentRepository;
import ma.lawflux.documents.support.InMemoryDocumentShareRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DocumentCommentEndpointTest {

    private final UUID cabinetId = UUID.randomUUID();
    private final UUID documentId = UUID.randomUUID();
    private final UUID lawyerId = UUID.randomUUID();
    private final UUID clientId = UUID.randomUUID();
    private final UUID readerId = UUID.randomUUID();

    private final CallerContext lawyer = new CallerContext(lawyerId, cabinetId, "Karim");
    private final CallerContext client = new CallerContext(clientId, cabinetId, "Client SA");
    private final CallerContext reader = new CallerContext(readerId, cabinetId, "Lecteur");
    private final CallerContext foreign =
            new CallerContext(UUID.randomUUID(), UUID.randomUUID(), "Autre cabinet");
    private final AtomicReference<CallerContext> current = new AtomicReference<>(lawyer);

    private final InMemoryDocumentShareRepository documents = new InMemoryDocumentShareRepository();
    private final InMemoryDocumentCommentRepository comments = new InMemoryDocumentCommentRepository();

    // Avocat : tout. Client : le partagé, tant que le document est partagé. Lecteur : lecture seule.
    private final CommentAccessPolicy policy = (caller, document, shared) -> {
        if (!caller.cabinetId().equals(document.cabinetId())) {
            return Set.of();
        }
        if (caller.userId().equals(clientId)) {
            return shared ? EnumSet.of(CommentRight.READ_SHARED, CommentRight.WRITE_SHARED)
                    : EnumSet.noneOf(CommentRight.class);
        }
        if (caller.userId().equals(readerId)) {
            return EnumSet.of(CommentRight.READ_INTERNAL, CommentRight.READ_SHARED);
        }
        return EnumSet.allOf(CommentRight.class);
    };

    private final AddCommentUseCase add = new AddCommentUseCase(documents, comments, policy);
    private final DocumentCommentEndpoint endpoint = new DocumentCommentEndpoint(
            new ListCommentsUseCase(documents, comments, policy),
            add,
            new GetCommentOptionsUseCase(documents, policy),
            current::get);

    private static class Recorder<T> implements StreamObserver<T> {
        final List<T> values = new CopyOnWriteArrayList<>();
        final AtomicInteger terminals = new AtomicInteger();
        volatile Throwable error;

        @Override public void onNext(T value) { values.add(value); }

        @Override public void onError(Throwable t) {
            error = t;
            terminals.incrementAndGet();
        }

        @Override public void onCompleted() { terminals.incrementAndGet(); }
    }

    @BeforeEach
    void setUp() {
        documents.register(new Document(documentId, cabinetId, UUID.randomUUID(), "Contrat de bail.pdf",
                DocumentType.PDF, 10, "key-" + documentId, lawyerId, "Karim",
                Instant.parse("2026-11-01T10:00:00Z")));
    }

    private void seed(ma.lawflux.documents.domain.CommentVisibility visibility, String body) {
        add.execute(lawyer, documentId, visibility, body);
    }

    private ListCommentsReply listAs(CallerContext caller) {
        current.set(caller);
        Recorder<ListCommentsReply> response = new Recorder<>();
        endpoint.listComments(
                ListCommentsRequest.newBuilder().setDocumentId(documentId.toString()).build(), response);
        assertThat(response.error).isNull();
        assertThat(response.terminals.get()).isEqualTo(1);
        return response.values.get(0);
    }

    private Recorder<AddCommentReply> addAs(CallerContext caller, String docId,
                                            CommentVisibility visibility, String body) {
        current.set(caller);
        Recorder<AddCommentReply> response = new Recorder<>();
        endpoint.addComment(AddCommentRequest.newBuilder()
                .setDocumentId(docId).setVisibility(visibility).setBody(body).build(), response);
        return response;
    }

    private static Status.Code code(Throwable t) {
        return Status.fromThrowable(t).getCode();
    }

    @Test
    void liste_avec_auteur_date_visibilite_et_options_d_ecriture_pour_l_avocat() {
        seed(ma.lawflux.documents.domain.CommentVisibility.INTERNAL, "note interne");
        seed(ma.lawflux.documents.domain.CommentVisibility.SHARED, "question au client");

        ListCommentsReply reply = listAs(lawyer);

        assertThat(reply.getCommentsList()).extracting(CommentSummary::getBody)
                .containsExactly("note interne", "question au client");
        assertThat(reply.getCommentsList()).extracting(CommentSummary::getVisibility)
                .containsExactly(CommentVisibility.COMMENT_VISIBILITY_INTERNAL,
                        CommentVisibility.COMMENT_VISIBILITY_SHARED);
        assertThat(reply.getCommentsList()).extracting(CommentSummary::getAuthor).containsOnly("Karim");
        assertThat(reply.getComments(0).getCreatedAt()).isNotBlank();
        assertThat(reply.getWritableVisibilitiesList()).containsExactly(
                CommentVisibility.COMMENT_VISIBILITY_INTERNAL, CommentVisibility.COMMENT_VISIBILITY_SHARED);
    }

    @Test
    void le_client_ne_voit_que_le_partage_et_n_a_que_l_option_partage() {
        documents.share(cabinetId, documentId, lawyerId, Instant.now());
        seed(ma.lawflux.documents.domain.CommentVisibility.INTERNAL, "stratégie confidentielle");
        seed(ma.lawflux.documents.domain.CommentVisibility.SHARED, "visible du client");

        ListCommentsReply reply = listAs(client);

        assertThat(reply.getCommentsList()).extracting(CommentSummary::getBody)
                .containsExactly("visible du client");
        assertThat(reply.getWritableVisibilitiesList())
                .containsExactly(CommentVisibility.COMMENT_VISIBILITY_SHARED);
    }

    @Test
    void un_lecteur_sans_droit_d_ecriture_lit_sans_aucune_option() {
        seed(ma.lawflux.documents.domain.CommentVisibility.INTERNAL, "interne");

        ListCommentsReply reply = listAs(reader);

        assertThat(reply.getCommentsList()).hasSize(1);
        assertThat(reply.getWritableVisibilitiesList()).isEmpty();
    }

    @Test
    void ajoute_un_commentaire_avec_l_identite_authentifiee() {
        Recorder<AddCommentReply> response = addAs(lawyer, documentId.toString(),
                CommentVisibility.COMMENT_VISIBILITY_SHARED, "  Merci de confirmer  ");

        assertThat(response.error).isNull();
        assertThat(response.terminals.get()).isEqualTo(1);
        CommentSummary created = response.values.get(0).getComment();
        assertThat(created.getBody()).isEqualTo("Merci de confirmer");
        assertThat(created.getAuthor()).isEqualTo("Karim");
        assertThat(created.getVisibility()).isEqualTo(CommentVisibility.COMMENT_VISIBILITY_SHARED);
        assertThat(comments.comments).hasSize(1);
        assertThat(comments.comments.get(0).authorId()).isEqualTo(lawyerId);
    }

    @Test
    void le_client_ne_peut_pas_ecrire_un_commentaire_interne() {
        documents.share(cabinetId, documentId, lawyerId, Instant.now());

        Recorder<AddCommentReply> response = addAs(client, documentId.toString(),
                CommentVisibility.COMMENT_VISIBILITY_INTERNAL, "tentative");

        assertThat(code(response.error)).isEqualTo(Status.Code.PERMISSION_DENIED);
        assertThat(response.values).isEmpty();
        assertThat(comments.comments).isEmpty();
    }

    @Test
    void un_autre_cabinet_recoit_not_found_en_lecture_et_en_ecriture() {
        current.set(foreign);
        Recorder<ListCommentsReply> list = new Recorder<>();
        endpoint.listComments(
                ListCommentsRequest.newBuilder().setDocumentId(documentId.toString()).build(), list);

        Recorder<AddCommentReply> added = addAs(foreign, documentId.toString(),
                CommentVisibility.COMMENT_VISIBILITY_SHARED, "intrusion");

        assertThat(code(list.error)).isEqualTo(Status.Code.NOT_FOUND);
        assertThat(code(added.error)).isEqualTo(Status.Code.NOT_FOUND);
        assertThat(comments.comments).isEmpty();
    }

    @Test
    void refuse_un_texte_vide() {
        Recorder<AddCommentReply> response = addAs(lawyer, documentId.toString(),
                CommentVisibility.COMMENT_VISIBILITY_SHARED, "   ");

        assertThat(code(response.error)).isEqualTo(Status.Code.INVALID_ARGUMENT);
        assertThat(comments.comments).isEmpty();
    }

    @Test
    void refuse_une_visibilite_non_precisee() {
        Recorder<AddCommentReply> response = addAs(lawyer, documentId.toString(),
                CommentVisibility.COMMENT_VISIBILITY_UNSPECIFIED, "bonjour");

        assertThat(code(response.error)).isEqualTo(Status.Code.INVALID_ARGUMENT);
        assertThat(comments.comments).isEmpty();
    }

    @Test
    void refuse_un_identifiant_de_document_invalide() {
        current.set(lawyer);
        Recorder<ListCommentsReply> list = new Recorder<>();
        endpoint.listComments(ListCommentsRequest.newBuilder().setDocumentId("pas-un-uuid").build(), list);

        Recorder<AddCommentReply> added = addAs(lawyer, "pas-un-uuid",
                CommentVisibility.COMMENT_VISIBILITY_SHARED, "bonjour");

        assertThat(code(list.error)).isEqualTo(Status.Code.INVALID_ARGUMENT);
        assertThat(code(added.error)).isEqualTo(Status.Code.INVALID_ARGUMENT);
    }
}