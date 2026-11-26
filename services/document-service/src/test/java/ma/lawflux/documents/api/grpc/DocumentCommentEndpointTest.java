package ma.lawflux.documents.api.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.time.Instant;
import java.util.EnumSet;
import java.util.UUID;
import ma.lawflux.contracts.documents.v1.AddDocumentCommentRequest;
import ma.lawflux.contracts.documents.v1.DocumentCommentReply;
import ma.lawflux.contracts.documents.v1.DocumentCommentsReply;
import ma.lawflux.contracts.documents.v1.DocumentCommentsRequest;
import ma.lawflux.documents.application.AddCommentUseCase;
import ma.lawflux.documents.application.CallerContext;
import ma.lawflux.documents.application.CommentRight;
import ma.lawflux.documents.application.ListCommentsUseCase;
import ma.lawflux.documents.domain.Document;
import ma.lawflux.documents.domain.DocumentType;
import ma.lawflux.documents.support.InMemoryDocumentCommentRepository;
import ma.lawflux.documents.support.InMemoryDocumentShareRepository;
import org.junit.jupiter.api.Test;

class DocumentCommentEndpointTest {
    @Test
    void exposesCommentsAndRejectsInvalidVisibility() {
        UUID cabinet = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        CallerContext caller = new CallerContext(UUID.randomUUID(), cabinet, "Karim");
        var documents = new InMemoryDocumentShareRepository();
        documents.register(new Document(documentId, cabinet, UUID.randomUUID(), "Contrat.pdf",
                DocumentType.PDF, 10, "private-key", caller.userId(), "Karim", Instant.now()));
        var comments = new InMemoryDocumentCommentRepository();
        var policy = (ma.lawflux.documents.application.CommentAccessPolicy)
                (user, document, shared) -> EnumSet.allOf(CommentRight.class);
        var endpoint = new DocumentCommentEndpoint(
                new ListCommentsUseCase(documents, comments, policy),
                new AddCommentUseCase(documents, comments, policy), () -> caller);

        Recorder<DocumentCommentReply> added = new Recorder<>();
        endpoint.addComment(AddDocumentCommentRequest.newBuilder().setDocumentId(documentId.toString())
                .setVisibility("INTERNAL").setBody("Note privée").build(), added);
        assertThat(added.value.getBody()).isEqualTo("Note privée");
        assertThat(added.value.getAuthor()).isEqualTo("Karim");

        Recorder<DocumentCommentsReply> listed = new Recorder<>();
        endpoint.listComments(DocumentCommentsRequest.newBuilder().setDocumentId(documentId.toString()).build(), listed);
        assertThat(listed.value.getCommentsList()).hasSize(1);

        Recorder<DocumentCommentReply> invalid = new Recorder<>();
        endpoint.addComment(AddDocumentCommentRequest.newBuilder().setDocumentId(documentId.toString())
                .setVisibility("PUBLIC").setBody("Oops").build(), invalid);
        assertThat(Status.fromThrowable(invalid.error).getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
    }

    private static final class Recorder<T> implements StreamObserver<T> {
        T value;
        Throwable error;
        @Override public void onNext(T result) { value = result; }
        @Override public void onError(Throwable cause) { error = cause; }
        @Override public void onCompleted() { }
    }
}
