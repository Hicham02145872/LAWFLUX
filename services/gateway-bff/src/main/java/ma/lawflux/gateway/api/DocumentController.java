package ma.lawflux.gateway.api;

import com.google.protobuf.ByteString;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import ma.lawflux.contracts.documents.v1.AddVersionMetadata;
import ma.lawflux.contracts.documents.v1.AddVersionReply;
import ma.lawflux.contracts.documents.v1.AddVersionRequest;
import ma.lawflux.contracts.documents.v1.DocumentServiceGrpc;
import ma.lawflux.contracts.documents.v1.DocumentSharingServiceGrpc;
import ma.lawflux.contracts.documents.v1.DocumentSummary;
import ma.lawflux.contracts.documents.v1.DownloadDocumentReply;
import ma.lawflux.contracts.documents.v1.ListDocumentsRequest;
import ma.lawflux.contracts.documents.v1.ListVersionsRequest;
import ma.lawflux.contracts.documents.v1.SharingReply;
import ma.lawflux.contracts.documents.v1.SharingRequest;
import ma.lawflux.contracts.documents.v1.UploadDocumentReply;
import ma.lawflux.contracts.documents.v1.UploadDocumentRequest;
import ma.lawflux.contracts.documents.v1.UploadMetadata;
import ma.lawflux.contracts.documents.v1.VersionSummary;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api")
public class DocumentController {
    private static final int CHUNK = 256 * 1024;
    private final DocumentServiceGrpc.DocumentServiceBlockingStub documents;
    private final DocumentServiceGrpc.DocumentServiceStub documentStream;
    private final DocumentSharingServiceGrpc.DocumentSharingServiceBlockingStub sharing;

    public DocumentController(GrpcChannelFactory channels) {
        var channel = channels.createChannel("documents");
        documents = DocumentServiceGrpc.newBlockingStub(channel);
        documentStream = DocumentServiceGrpc.newStub(channel);
        sharing = DocumentSharingServiceGrpc.newBlockingStub(channel);
    }

    @GetMapping("/dossiers/{caseId}/documents")
    public List<DocumentItem> list(@PathVariable String caseId, @AuthenticationPrincipal Jwt jwt) {
        try {
            return documents.withInterceptors(auth(jwt)).withDeadlineAfter(5, TimeUnit.SECONDS)
                    .listDocuments(ListDocumentsRequest.newBuilder().setCaseId(caseId).build())
                    .getDocumentsList().stream().map(DocumentController::item).toList();
        } catch (StatusRuntimeException e) { throw httpError(e); }
    }

    @PostMapping(value = "/dossiers/{caseId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentItem upload(@PathVariable String caseId, @RequestParam("file") MultipartFile file,
                               @AuthenticationPrincipal Jwt jwt) {
        requireFile(file);
        CompletableFuture<UploadDocumentReply> result = new CompletableFuture<>();
        StreamObserver<UploadDocumentRequest> request = documentStream.withInterceptors(auth(jwt))
                .withDeadlineAfter(60, TimeUnit.SECONDS).uploadDocument(observer(result));
        try {
            request.onNext(UploadDocumentRequest.newBuilder().setMetadata(UploadMetadata.newBuilder()
                    .setCaseId(caseId).setFileName(file.getOriginalFilename()).build()).build());
            try (var input = file.getInputStream()) {
                byte[] buffer = new byte[CHUNK];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    request.onNext(UploadDocumentRequest.newBuilder()
                            .setChunk(ByteString.copyFrom(buffer, 0, count)).build());
                }
            }
            request.onCompleted();
            return item(result.get(65, TimeUnit.SECONDS).getDocument());
        } catch (Exception e) { request.onError(e); throw httpError(e); }
    }

    @GetMapping("/documents/{id}/versions")
    public List<VersionItem> versions(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        try {
            return documents.withInterceptors(auth(jwt)).withDeadlineAfter(5, TimeUnit.SECONDS)
                    .listVersions(ListVersionsRequest.newBuilder().setDocumentId(id).build())
                    .getVersionsList().stream().map(DocumentController::version).toList();
        } catch (StatusRuntimeException e) { throw httpError(e); }
    }

    @PostMapping(value = "/documents/{id}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public VersionItem addVersion(@PathVariable String id, @RequestParam("file") MultipartFile file,
                                  @AuthenticationPrincipal Jwt jwt) {
        requireFile(file);
        CompletableFuture<AddVersionReply> result = new CompletableFuture<>();
        StreamObserver<AddVersionRequest> request = documentStream.withInterceptors(auth(jwt))
                .withDeadlineAfter(60, TimeUnit.SECONDS).addVersion(observer(result));
        try {
            request.onNext(AddVersionRequest.newBuilder().setMetadata(AddVersionMetadata.newBuilder()
                    .setDocumentId(id).setFileName(file.getOriginalFilename()).build()).build());
            try (var input = file.getInputStream()) {
                byte[] buffer = new byte[CHUNK];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    request.onNext(AddVersionRequest.newBuilder()
                            .setChunk(ByteString.copyFrom(buffer, 0, count)).build());
                }
            }
            request.onCompleted();
            return version(result.get(65, TimeUnit.SECONDS).getVersion());
        } catch (Exception e) { request.onError(e); throw httpError(e); }
    }

    @GetMapping("/documents/{id}/partage")
    public SharingItem getSharing(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        try { return sharing(sharing.withInterceptors(auth(jwt)).withDeadlineAfter(5, TimeUnit.SECONDS)
                .getSharing(sharingRequest(id))); }
        catch (StatusRuntimeException e) { throw httpError(e); }
    }

    @PostMapping("/documents/{id}/partage")
    public SharingItem share(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        try { return sharing(sharing.withInterceptors(auth(jwt)).withDeadlineAfter(5, TimeUnit.SECONDS)
                .shareDocument(sharingRequest(id))); }
        catch (StatusRuntimeException e) { throw httpError(e); }
    }

    @DeleteMapping("/documents/{id}/partage")
    public SharingItem unshare(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        try { return sharing(sharing.withInterceptors(auth(jwt)).withDeadlineAfter(5, TimeUnit.SECONDS)
                .unshareDocument(sharingRequest(id))); }
        catch (StatusRuntimeException e) { throw httpError(e); }
    }

    @GetMapping("/documents/{id}/telechargement")
    public ResponseEntity<StreamingResponseBody> download(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        try {
            Iterator<DownloadDocumentReply> chunks = sharing.withInterceptors(auth(jwt))
                    .withDeadlineAfter(60, TimeUnit.SECONDS).downloadDocument(sharingRequest(id));
            if (!chunks.hasNext()) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY);
            DownloadDocumentReply first = chunks.next();
            if (!first.hasHeader()) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY);
            String filename = first.getHeader().getFileName().replaceAll("[\\r\\n\\\"]", "_");
            StreamingResponseBody body = output -> {
                while (chunks.hasNext()) {
                    DownloadDocumentReply part = chunks.next();
                    if (part.hasChunk()) part.getChunk().writeTo(output);
                }
            };
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentLength(first.getHeader().getSizeBytes())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM).body(body);
        } catch (StatusRuntimeException e) { throw httpError(e); }
    }

    private static void requireFile(MultipartFile file) {
        if (file.isEmpty() || file.getOriginalFilename() == null || file.getSize() > 20L * 1024 * 1024)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fichier invalide ou trop grand");
    }

    private static io.grpc.ClientInterceptor auth(Jwt jwt) {
        Metadata headers = new Metadata();
        headers.put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer " + jwt.getTokenValue());
        return MetadataUtils.newAttachHeadersInterceptor(headers);
    }

    private static <T> StreamObserver<T> observer(CompletableFuture<T> result) {
        return new StreamObserver<>() {
            @Override public void onNext(T value) { result.complete(value); }
            @Override public void onError(Throwable error) { result.completeExceptionally(error); }
            @Override public void onCompleted() { }
        };
    }

    private static SharingRequest sharingRequest(String id) {
        return SharingRequest.newBuilder().setDocumentId(id).build();
    }
    private static DocumentItem item(DocumentSummary d) {
        return new DocumentItem(d.getId(), d.getName(), d.getType(), d.getAuthor(), d.getCreatedAt());
    }
    private static VersionItem version(VersionSummary v) {
        return new VersionItem(v.getNumber(), v.getAuthor(), v.getCreatedAt(), v.getType(), v.getSizeBytes());
    }
    private static SharingItem sharing(SharingReply s) {
        return new SharingItem(s.getDocumentId(), s.getName(), s.getShared(),
                s.getAllowedActionsList().stream().map(a -> a.name().replace("SHARING_ACTION_", "")).toList());
    }
    private static ResponseStatusException httpError(Throwable error) {
        if (error instanceof java.util.concurrent.ExecutionException e) error = e.getCause();
        if (error instanceof StatusRuntimeException e) {
            Status.Code code = e.getStatus().getCode();
            HttpStatus status = switch (code) {
                case INVALID_ARGUMENT -> HttpStatus.BAD_REQUEST;
                case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;
                case PERMISSION_DENIED -> HttpStatus.FORBIDDEN;
                case NOT_FOUND -> HttpStatus.NOT_FOUND;
                case UNAVAILABLE, DEADLINE_EXCEEDED -> HttpStatus.SERVICE_UNAVAILABLE;
                default -> HttpStatus.BAD_GATEWAY;
            };
            return new ResponseStatusException(status, e.getStatus().getDescription());
        }
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Service de documents indisponible", error);
    }

    public record DocumentItem(String id, String name, String type, String author, String createdAt) {}
    public record VersionItem(int number, String author, String createdAt, String type, long sizeBytes) {}
    public record SharingItem(String documentId, String name, boolean shared, List<String> allowedActions) {}
}
