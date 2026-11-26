package ma.lawflux.documents.api.grpc;

import io.grpc.stub.StreamObserver;
import java.util.UUID;
import ma.lawflux.contracts.documents.v1.AddDocumentCommentRequest;
import ma.lawflux.contracts.documents.v1.DocumentCommentReply;
import ma.lawflux.contracts.documents.v1.DocumentCommentServiceGrpc;
import ma.lawflux.contracts.documents.v1.DocumentCommentsReply;
import ma.lawflux.contracts.documents.v1.DocumentCommentsRequest;
import ma.lawflux.documents.application.AddCommentUseCase;
import ma.lawflux.documents.application.CallerContextProvider;
import ma.lawflux.documents.application.ListCommentsUseCase;
import ma.lawflux.documents.domain.CommentVisibility;
import ma.lawflux.documents.domain.DocumentComment;
import ma.lawflux.documents.domain.InvalidDocumentException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "lawflux.documents", name = "business-enabled", havingValue = "true")
public class DocumentCommentEndpoint extends DocumentCommentServiceGrpc.DocumentCommentServiceImplBase {
    private final ListCommentsUseCase list;
    private final AddCommentUseCase add;
    private final CallerContextProvider caller;

    public DocumentCommentEndpoint(ListCommentsUseCase list, AddCommentUseCase add,
                                   CallerContextProvider caller) {
        this.list = list;
        this.add = add;
        this.caller = caller;
    }

    @Override
    public void listComments(DocumentCommentsRequest request, StreamObserver<DocumentCommentsReply> response) {
        try {
            UUID id = GrpcErrors.parseUuid(request.getDocumentId(), "Identifiant de document invalide");
            DocumentCommentsReply.Builder reply = DocumentCommentsReply.newBuilder();
            list.execute(caller.current(), id).forEach(comment -> reply.addComments(toReply(comment)));
            response.onNext(reply.build());
            response.onCompleted();
        } catch (RuntimeException e) {
            response.onError(GrpcErrors.toStatus(e).asRuntimeException());
        }
    }

    @Override
    public void addComment(AddDocumentCommentRequest request, StreamObserver<DocumentCommentReply> response) {
        try {
            UUID id = GrpcErrors.parseUuid(request.getDocumentId(), "Identifiant de document invalide");
            CommentVisibility visibility;
            try { visibility = CommentVisibility.valueOf(request.getVisibility()); }
            catch (IllegalArgumentException e) { throw new InvalidDocumentException("Visibilité invalide"); }
            response.onNext(toReply(add.execute(caller.current(), id, visibility, request.getBody())));
            response.onCompleted();
        } catch (RuntimeException e) {
            response.onError(GrpcErrors.toStatus(e).asRuntimeException());
        }
    }

    private static DocumentCommentReply toReply(DocumentComment c) {
        return DocumentCommentReply.newBuilder()
                .setId(c.id().toString()).setDocumentId(c.documentId().toString())
                .setAuthor(c.authorName()).setVisibility(c.visibility().name())
                .setBody(c.body()).setCreatedAt(c.createdAt().toString()).build();
    }
}
