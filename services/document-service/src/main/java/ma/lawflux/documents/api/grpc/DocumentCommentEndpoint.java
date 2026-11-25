package ma.lawflux.documents.api.grpc;

import io.grpc.stub.StreamObserver;
import java.util.UUID;
import ma.lawflux.contracts.documents.v1.AddCommentReply;
import ma.lawflux.contracts.documents.v1.AddCommentRequest;
import ma.lawflux.contracts.documents.v1.CommentSummary;
import ma.lawflux.contracts.documents.v1.CommentVisibility;
import ma.lawflux.contracts.documents.v1.DocumentCommentServiceGrpc;
import ma.lawflux.contracts.documents.v1.ListCommentsReply;
import ma.lawflux.contracts.documents.v1.ListCommentsRequest;
import ma.lawflux.documents.application.AddCommentUseCase;
import ma.lawflux.documents.application.CallerContext;
import ma.lawflux.documents.application.CallerContextProvider;
import ma.lawflux.documents.application.GetCommentOptionsUseCase;
import ma.lawflux.documents.application.ListCommentsUseCase;
import ma.lawflux.documents.domain.DocumentComment;
import ma.lawflux.documents.domain.InvalidDocumentException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "lawflux.documents", name = "business-enabled", havingValue = "true")
public class DocumentCommentEndpoint extends DocumentCommentServiceGrpc.DocumentCommentServiceImplBase {

    private final ListCommentsUseCase listComments;
    private final AddCommentUseCase addComment;
    private final GetCommentOptionsUseCase options;
    private final CallerContextProvider callerContext;

    public DocumentCommentEndpoint(
            ListCommentsUseCase listComments,
            AddCommentUseCase addComment,
            GetCommentOptionsUseCase options,
            CallerContextProvider callerContext) {
        this.listComments = listComments;
        this.addComment = addComment;
        this.options = options;
        this.callerContext = callerContext;
    }

    @Override
    public void listComments(ListCommentsRequest request, StreamObserver<ListCommentsReply> response) {
        try {
            CallerContext caller = callerContext.current();
            UUID documentId = GrpcErrors.parseUuid(request.getDocumentId(), "Identifiant de document invalide");
            ListCommentsReply.Builder reply = ListCommentsReply.newBuilder();
            listComments.execute(caller, documentId).forEach(c -> reply.addComments(toSummary(c)));
            options.execute(caller, documentId).stream().sorted()
                    .forEach(v -> reply.addWritableVisibilities(toProto(v)));
            response.onNext(reply.build());
            response.onCompleted();
        } catch (RuntimeException e) {
            response.onError(GrpcErrors.toStatus(e).asRuntimeException());
        }
    }

    @Override
    public void addComment(AddCommentRequest request, StreamObserver<AddCommentReply> response) {
        try {
            CallerContext caller = callerContext.current();
            UUID documentId = GrpcErrors.parseUuid(request.getDocumentId(), "Identifiant de document invalide");
            DocumentComment saved = addComment.execute(caller, documentId,
                    toDomain(request.getVisibility()), request.getBody());
            response.onNext(AddCommentReply.newBuilder().setComment(toSummary(saved)).build());
            response.onCompleted();
        } catch (RuntimeException e) {
            response.onError(GrpcErrors.toStatus(e).asRuntimeException());
        }
    }

    private static CommentSummary toSummary(DocumentComment c) {
        return CommentSummary.newBuilder()
                .setId(c.id().toString())
                .setAuthor(c.authorName())
                .setCreatedAt(c.createdAt().toString())
                .setVisibility(toProto(c.visibility()))
                .setBody(c.body())
                .build();
    }

    private static CommentVisibility toProto(ma.lawflux.documents.domain.CommentVisibility visibility) {
        return switch (visibility) {
            case INTERNAL -> CommentVisibility.COMMENT_VISIBILITY_INTERNAL;
            case SHARED -> CommentVisibility.COMMENT_VISIBILITY_SHARED;
        };
    }

    private static ma.lawflux.documents.domain.CommentVisibility toDomain(CommentVisibility visibility) {
        return switch (visibility) {
            case COMMENT_VISIBILITY_INTERNAL -> ma.lawflux.documents.domain.CommentVisibility.INTERNAL;
            case COMMENT_VISIBILITY_SHARED -> ma.lawflux.documents.domain.CommentVisibility.SHARED;
            default -> throw new InvalidDocumentException("Visibilité invalide");
        };
    }
}