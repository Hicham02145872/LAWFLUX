package ma.lawflux.documents.api.grpc;

import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import java.io.InputStream;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import ma.lawflux.contracts.documents.v1.AddVersionMetadata;
import ma.lawflux.contracts.documents.v1.AddVersionReply;
import ma.lawflux.contracts.documents.v1.AddVersionRequest;
import ma.lawflux.contracts.documents.v1.DocumentServiceGrpc;
import ma.lawflux.contracts.documents.v1.DocumentSummary;
import ma.lawflux.contracts.documents.v1.ListDocumentsReply;
import ma.lawflux.contracts.documents.v1.ListDocumentsRequest;
import ma.lawflux.contracts.documents.v1.ListVersionsReply;
import ma.lawflux.contracts.documents.v1.ListVersionsRequest;
import ma.lawflux.contracts.documents.v1.UploadDocumentReply;
import ma.lawflux.contracts.documents.v1.UploadDocumentRequest;
import ma.lawflux.contracts.documents.v1.UploadMetadata;
import ma.lawflux.contracts.documents.v1.VersionSummary;
import ma.lawflux.documents.application.AddVersionCommand;
import ma.lawflux.documents.application.AddVersionUseCase;
import ma.lawflux.documents.application.CallerContext;
import ma.lawflux.documents.application.CallerContextProvider;
import ma.lawflux.documents.application.ListDocumentsUseCase;
import ma.lawflux.documents.application.ListVersionsUseCase;
import ma.lawflux.documents.application.UploadCommand;
import ma.lawflux.documents.application.UploadDocumentUseCase;
import ma.lawflux.documents.domain.Document;
import ma.lawflux.documents.domain.DocumentVersion;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;

@Service
@ConditionalOnProperty(prefix = "lawflux.documents", name = "business-enabled", havingValue = "true")
public class DocumentServiceEndpoint extends DocumentServiceGrpc.DocumentServiceImplBase
        implements DisposableBean {

    private final ListDocumentsUseCase listUseCase;
    private final UploadDocumentUseCase uploadUseCase;
    private final ListVersionsUseCase listVersionsUseCase;
    private final AddVersionUseCase addVersionUseCase;
    private final CallerContextProvider callerContext;
    private final DataSize maxFileSize;
    private final Duration idleTimeout;
    private final ScheduledExecutorService watchdog = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "document-upload-watchdog");
        thread.setDaemon(true);
        return thread;
    });

    public DocumentServiceEndpoint(
            ListDocumentsUseCase listUseCase,
            UploadDocumentUseCase uploadUseCase,
            ListVersionsUseCase listVersionsUseCase,
            AddVersionUseCase addVersionUseCase,
            CallerContextProvider callerContext,
            @Value("${lawflux.documents.max-file-size}") DataSize maxFileSize,
            @Value("${lawflux.documents.upload-idle-timeout:30s}") Duration idleTimeout) {
        if (idleTimeout.isZero() || idleTimeout.isNegative()) {
            throw new IllegalArgumentException("upload-idle-timeout doit être positif");
        }
        this.listUseCase = listUseCase;
        this.uploadUseCase = uploadUseCase;
        this.listVersionsUseCase = listVersionsUseCase;
        this.addVersionUseCase = addVersionUseCase;
        this.callerContext = callerContext;
        this.maxFileSize = maxFileSize;
        this.idleTimeout = idleTimeout;
    }

    @Override
    public void destroy() {
        watchdog.shutdownNow();
    }

    @Override
    public void listDocuments(ListDocumentsRequest request,
                              StreamObserver<ListDocumentsReply> responseObserver) {
        try {
            CallerContext caller = callerContext.current();
            UUID caseId = GrpcErrors.parseUuid(request.getCaseId(), "Identifiant de dossier invalide");
            ListDocumentsReply.Builder reply = ListDocumentsReply.newBuilder();
            listUseCase.execute(caller.cabinetId(), caseId).forEach(d -> reply.addDocuments(toSummary(d)));
            responseObserver.onNext(reply.build());
            responseObserver.onCompleted();
        } catch (RuntimeException e) {
            responseObserver.onError(GrpcErrors.toStatus(e).asRuntimeException());
        }
    }

    @Override
    public void listVersions(ListVersionsRequest request,
                             StreamObserver<ListVersionsReply> responseObserver) {
        try {
            CallerContext caller = callerContext.current();
            UUID documentId = GrpcErrors.parseUuid(request.getDocumentId(), "Identifiant de document invalide");
            ListVersionsReply.Builder reply = ListVersionsReply.newBuilder();
            listVersionsUseCase.execute(caller.cabinetId(), documentId)
                    .forEach(v -> reply.addVersions(toSummary(v)));
            responseObserver.onNext(reply.build());
            responseObserver.onCompleted();
        } catch (RuntimeException e) {
            responseObserver.onError(GrpcErrors.toStatus(e).asRuntimeException());
        }
    }

    @Override
    public StreamObserver<UploadDocumentRequest> uploadDocument(
            StreamObserver<UploadDocumentReply> responseObserver) {
        CallerContext caller = authenticate(responseObserver);
        if (caller == null) {
            return ignoring();
        }
        return new ChunkedUpload<UploadDocumentRequest, UploadDocumentReply, UploadMetadata>(
                responseObserver, watchdog, idleTimeout, maxFileSize.toBytes()) {

            @Override
            protected UploadMetadata metadataOf(UploadDocumentRequest message) {
                return message.getPayloadCase() == UploadDocumentRequest.PayloadCase.METADATA
                        ? message.getMetadata() : null;
            }

            @Override
            protected ByteString chunkOf(UploadDocumentRequest message) {
                return message.getPayloadCase() == UploadDocumentRequest.PayloadCase.CHUNK
                        ? message.getChunk() : null;
            }

            @Override
            protected UploadDocumentReply complete(UploadMetadata meta, InputStream content, long size) {
                UUID caseId = GrpcErrors.parseUuid(meta.getCaseId(), "Identifiant de dossier invalide");
                Document saved = uploadUseCase.execute(new UploadCommand(
                        caller.cabinetId(), caseId, caller.userId(), caller.authorName(),
                        meta.getFileName(), size, content));
                return UploadDocumentReply.newBuilder().setDocument(toSummary(saved)).build();
            }
        };
    }

    @Override
    public StreamObserver<AddVersionRequest> addVersion(StreamObserver<AddVersionReply> responseObserver) {
        CallerContext caller = authenticate(responseObserver);
        if (caller == null) {
            return ignoring();
        }
        return new ChunkedUpload<AddVersionRequest, AddVersionReply, AddVersionMetadata>(
                responseObserver, watchdog, idleTimeout, maxFileSize.toBytes()) {

            @Override
            protected AddVersionMetadata metadataOf(AddVersionRequest message) {
                return message.getPayloadCase() == AddVersionRequest.PayloadCase.METADATA
                        ? message.getMetadata() : null;
            }

            @Override
            protected ByteString chunkOf(AddVersionRequest message) {
                return message.getPayloadCase() == AddVersionRequest.PayloadCase.CHUNK
                        ? message.getChunk() : null;
            }

            @Override
            protected AddVersionReply complete(AddVersionMetadata meta, InputStream content, long size) {
                UUID documentId = GrpcErrors.parseUuid(meta.getDocumentId(), "Identifiant de document invalide");
                DocumentVersion created = addVersionUseCase.execute(new AddVersionCommand(
                        caller.cabinetId(), documentId, caller.userId(), caller.authorName(),
                        meta.getFileName(), size, content));
                return AddVersionReply.newBuilder().setVersion(toSummary(created)).build();
            }
        };
    }

    /** Renvoie l'appelant, ou null après avoir terminé l'appel en erreur. */
    private <R> CallerContext authenticate(StreamObserver<R> response) {
        try {
            return callerContext.current();
        } catch (RuntimeException e) {
            response.onError(GrpcErrors.toStatus(e).asRuntimeException());
            return null;
        }
    }

    private static <T> StreamObserver<T> ignoring() {
        return new StreamObserver<>() {
            @Override public void onNext(T value) { }
            @Override public void onError(Throwable t) { }
            @Override public void onCompleted() { }
        };
    }

    private static DocumentSummary toSummary(Document d) {
        return DocumentSummary.newBuilder()
                .setId(d.id().toString())
                .setName(d.name())
                .setType(d.type().name())
                .setAuthor(d.authorName())
                .setCreatedAt(d.createdAt().toString())
                .build();
    }

    private static VersionSummary toSummary(DocumentVersion v) {
        return VersionSummary.newBuilder()
                .setNumber(v.number())
                .setAuthor(v.authorName())
                .setCreatedAt(v.createdAt().toString())
                .setType(v.type().name())
                .setSizeBytes(v.sizeBytes())
                .build();
    }
}