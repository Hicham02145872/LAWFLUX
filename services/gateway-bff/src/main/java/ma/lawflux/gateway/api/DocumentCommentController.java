package ma.lawflux.gateway.api;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;
import java.util.List;
import java.util.concurrent.TimeUnit;
import ma.lawflux.contracts.documents.v1.AddDocumentCommentRequest;
import ma.lawflux.contracts.documents.v1.DocumentCommentReply;
import ma.lawflux.contracts.documents.v1.DocumentCommentServiceGrpc;
import ma.lawflux.contracts.documents.v1.DocumentCommentsRequest;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/documents/{id}/commentaires")
public class DocumentCommentController {
    private final DocumentCommentServiceGrpc.DocumentCommentServiceBlockingStub comments;

    public DocumentCommentController(GrpcChannelFactory channels) {
        comments = DocumentCommentServiceGrpc.newBlockingStub(channels.createChannel("documents"));
    }

    @GetMapping
    public List<CommentItem> list(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        try {
            return comments.withInterceptors(auth(jwt)).withDeadlineAfter(5, TimeUnit.SECONDS)
                    .listComments(DocumentCommentsRequest.newBuilder().setDocumentId(id).build())
                    .getCommentsList().stream().map(DocumentCommentController::item).toList();
        } catch (StatusRuntimeException e) { throw httpError(e); }
    }

    @PostMapping
    public CommentItem add(@PathVariable String id, @RequestBody AddComment body,
                           @AuthenticationPrincipal Jwt jwt) {
        if (body == null || body.visibility() == null || body.body() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Commentaire incomplet");
        try {
            return item(comments.withInterceptors(auth(jwt)).withDeadlineAfter(5, TimeUnit.SECONDS)
                    .addComment(AddDocumentCommentRequest.newBuilder()
                            .setDocumentId(id).setVisibility(body.visibility()).setBody(body.body()).build()));
        } catch (StatusRuntimeException e) { throw httpError(e); }
    }

    private static io.grpc.ClientInterceptor auth(Jwt jwt) {
        Metadata headers = new Metadata();
        headers.put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER),
                "Bearer " + jwt.getTokenValue());
        return MetadataUtils.newAttachHeadersInterceptor(headers);
    }

    private static CommentItem item(DocumentCommentReply c) {
        return new CommentItem(c.getId(), c.getDocumentId(), c.getAuthor(), c.getVisibility(),
                c.getBody(), c.getCreatedAt());
    }

    private static ResponseStatusException httpError(StatusRuntimeException e) {
        HttpStatus status = switch (e.getStatus().getCode()) {
            case INVALID_ARGUMENT -> HttpStatus.BAD_REQUEST;
            case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;
            case PERMISSION_DENIED -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case UNAVAILABLE, DEADLINE_EXCEEDED -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.BAD_GATEWAY;
        };
        return new ResponseStatusException(status, e.getStatus().getDescription());
    }

    public record AddComment(String visibility, String body) {}
    public record CommentItem(String id, String documentId, String author, String visibility,
                              String body, String createdAt) {}
}
