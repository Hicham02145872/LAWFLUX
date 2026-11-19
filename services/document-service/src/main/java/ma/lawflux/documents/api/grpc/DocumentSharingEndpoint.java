package ma.lawflux.documents.api.grpc;

import io.grpc.stub.StreamObserver;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiFunction;
import ma.lawflux.contracts.documents.v1.DocumentSharingServiceGrpc;
import ma.lawflux.contracts.documents.v1.DownloadDocumentReply;
import ma.lawflux.contracts.documents.v1.SharingAction;
import ma.lawflux.contracts.documents.v1.SharingReply;
import ma.lawflux.contracts.documents.v1.SharingRequest;
import ma.lawflux.documents.application.CallerContext;
import ma.lawflux.documents.application.CallerContextProvider;
import ma.lawflux.documents.application.DocumentDownload;
import ma.lawflux.documents.application.DownloadDocumentUseCase;
import ma.lawflux.documents.application.GetSharingUseCase;
import ma.lawflux.documents.application.ShareDocumentUseCase;
import ma.lawflux.documents.application.SharingStatus;
import ma.lawflux.documents.application.UnshareDocumentUseCase;
import ma.lawflux.documents.domain.DocumentAction;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "lawflux.documents", name = "business-enabled", havingValue = "true")
public class DocumentSharingEndpoint extends DocumentSharingServiceGrpc.DocumentSharingServiceImplBase
        implements DisposableBean {

    private final GetSharingUseCase getSharing;
    private final ShareDocumentUseCase shareDocument;
    private final UnshareDocumentUseCase unshareDocument;
    private final DownloadDocumentUseCase downloadDocument;
    private final CallerContextProvider callerContext;
    private final Duration idleTimeout;
    private final ScheduledExecutorService watchdog = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "document-download-watchdog");
        thread.setDaemon(true);
        return thread;
    });

    public DocumentSharingEndpoint(
            GetSharingUseCase getSharing,
            ShareDocumentUseCase shareDocument,
            UnshareDocumentUseCase unshareDocument,
            DownloadDocumentUseCase downloadDocument,
            CallerContextProvider callerContext,
            @Value("${lawflux.documents.download-idle-timeout:30s}") Duration idleTimeout) {
        if (idleTimeout.isZero() || idleTimeout.isNegative()) {
            throw new IllegalArgumentException("download-idle-timeout doit être positif");
        }
        this.getSharing = getSharing;
        this.shareDocument = shareDocument;
        this.unshareDocument = unshareDocument;
        this.downloadDocument = downloadDocument;
        this.callerContext = callerContext;
        this.idleTimeout = idleTimeout;
    }

    @Override
    public void destroy() {
        watchdog.shutdownNow();
    }

    @Override
    public void getSharing(SharingRequest request, StreamObserver<SharingReply> responseObserver) {
        respond(request, responseObserver, getSharing::execute);
    }

    @Override
    public void shareDocument(SharingRequest request, StreamObserver<SharingReply> responseObserver) {
        respond(request, responseObserver, shareDocument::execute);
    }

    @Override
    public void unshareDocument(SharingRequest request, StreamObserver<SharingReply> responseObserver) {
        respond(request, responseObserver, unshareDocument::execute);
    }

    @Override
    public void downloadDocument(SharingRequest request,
                                 StreamObserver<DownloadDocumentReply> responseObserver) {
        DocumentDownload download;
        try {
            CallerContext caller = callerContext.current();
            UUID documentId = GrpcErrors.parseUuid(request.getDocumentId(), "Identifiant de document invalide");
            download = downloadDocument.execute(caller, documentId);
        } catch (RuntimeException e) {
            responseObserver.onError(GrpcErrors.toStatus(e).asRuntimeException());
            return;
        }
        ChunkedDownload.start(download, responseObserver, watchdog, idleTimeout);
    }

    private void respond(SharingRequest request, StreamObserver<SharingReply> response,
                         BiFunction<CallerContext, UUID, SharingStatus> action) {
        try {
            CallerContext caller = callerContext.current();
            UUID documentId = GrpcErrors.parseUuid(request.getDocumentId(), "Identifiant de document invalide");
            response.onNext(toReply(action.apply(caller, documentId)));
            response.onCompleted();
        } catch (RuntimeException e) {
            response.onError(GrpcErrors.toStatus(e).asRuntimeException());
        }
    }

    private static SharingReply toReply(SharingStatus status) {
        SharingReply.Builder reply = SharingReply.newBuilder()
                .setDocumentId(status.document().id().toString())
                .setName(status.document().name())
                .setShared(status.shared());
        status.allowedActions().stream().sorted().forEach(a -> reply.addAllowedActions(toProto(a)));
        return reply.build();
    }

    private static SharingAction toProto(DocumentAction action) {
        return switch (action) {
            case SHARE -> SharingAction.SHARING_ACTION_SHARE;
            case UNSHARE -> SharingAction.SHARING_ACTION_UNSHARE;
            case DOWNLOAD -> SharingAction.SHARING_ACTION_DOWNLOAD;
        };
    }
}