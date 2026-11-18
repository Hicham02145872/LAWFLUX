package ma.lawflux.documents.api.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import ma.lawflux.contracts.documents.v1.DocumentServiceGrpc;
import ma.lawflux.contracts.documents.v1.DocumentSummary;
import ma.lawflux.contracts.documents.v1.ListDocumentsReply;
import ma.lawflux.contracts.documents.v1.ListDocumentsRequest;
import ma.lawflux.contracts.documents.v1.UploadDocumentReply;
import ma.lawflux.contracts.documents.v1.UploadDocumentRequest;
import ma.lawflux.contracts.documents.v1.UploadMetadata;
import ma.lawflux.documents.application.CallerContext;
import ma.lawflux.documents.application.CallerContextProvider;
import ma.lawflux.documents.application.ListDocumentsUseCase;
import ma.lawflux.documents.application.UploadCommand;
import ma.lawflux.documents.application.UploadDocumentUseCase;
import ma.lawflux.documents.domain.CaseAccessDeniedException;
import ma.lawflux.documents.domain.Document;
import ma.lawflux.documents.domain.InvalidDocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;

@Service
@ConditionalOnProperty(prefix = "lawflux.documents", name = "business-enabled", havingValue = "true")
public class DocumentServiceEndpoint extends DocumentServiceGrpc.DocumentServiceImplBase
        implements DisposableBean {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentServiceEndpoint.class);

    private final ListDocumentsUseCase listUseCase;
    private final UploadDocumentUseCase uploadUseCase;
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
            CallerContextProvider callerContext,
            @Value("${lawflux.documents.max-file-size}") DataSize maxFileSize,
            @Value("${lawflux.documents.upload-idle-timeout:30s}") Duration idleTimeout) {
        if (idleTimeout.isZero() || idleTimeout.isNegative()) {
            throw new IllegalArgumentException("upload-idle-timeout doit être positif");
        }
        this.listUseCase = listUseCase;
        this.uploadUseCase = uploadUseCase;
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
            UUID caseId = parseUuid(request.getCaseId());
            ListDocumentsReply.Builder reply = ListDocumentsReply.newBuilder();
            listUseCase.execute(caller.cabinetId(), caseId).forEach(d -> reply.addDocuments(toSummary(d)));
            responseObserver.onNext(reply.build());
            responseObserver.onCompleted();
        } catch (RuntimeException e) {
            responseObserver.onError(toStatus(e).asRuntimeException());
        }
    }

    @Override
    public StreamObserver<UploadDocumentRequest> uploadDocument(
            StreamObserver<UploadDocumentReply> responseObserver) {
        CallerContext caller;
        try {
            caller = callerContext.current();
        } catch (RuntimeException e) {
            responseObserver.onError(toStatus(e).asRuntimeException());
            return new StreamObserver<>() {
                @Override public void onNext(UploadDocumentRequest value) { }
                @Override public void onError(Throwable t) { }
                @Override public void onCompleted() { }
            };
        }
        return new UploadStream(caller, responseObserver);
    }

    /**
     * Les méthodes sont synchronisées : le chien de garde (idle timeout) s'exécute
     * sur un autre thread que les callbacks gRPC.
     */
    private final class UploadStream implements StreamObserver<UploadDocumentRequest> {

        private final CallerContext caller;
        private final StreamObserver<UploadDocumentReply> response;
        private UploadMetadata metadata;
        private Path tempFile;
        private OutputStream out;
        private long received;
        private boolean finished;
        private ScheduledFuture<?> idleTask;

        UploadStream(CallerContext caller, StreamObserver<UploadDocumentReply> response) {
            this.caller = caller;
            this.response = response;
            armIdleTimer();
        }

        private synchronized void armIdleTimer() {
            if (idleTask != null) {
                idleTask.cancel(false);
            }
            idleTask = watchdog.schedule(this::expire, idleTimeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        private synchronized void expire() {
            if (finished) return;
            LOG.warn("Import interrompu : délai d'inactivité dépassé");
            finished = true;
            cleanup();
            response.onError(Status.DEADLINE_EXCEEDED
                    .withDescription("Délai d'inactivité dépassé").asRuntimeException());
        }

        @Override
        public synchronized void onNext(UploadDocumentRequest message) {
            if (finished) return;
            armIdleTimer();
            try {
                switch (message.getPayloadCase()) {
                    case METADATA -> start(message.getMetadata());
                    case CHUNK -> append(message);
                    default -> throw new InvalidDocumentException("Message d'import vide");
                }
            } catch (IOException | RuntimeException e) {
                fail(e);
            }
        }

        private void start(UploadMetadata meta) throws IOException {
            if (metadata != null) {
                throw new InvalidDocumentException("Métadonnées déjà reçues");
            }
            metadata = meta;
            tempFile = Files.createTempFile("lawflux-upload-", ".part");
            out = Files.newOutputStream(tempFile);
        }

        private void append(UploadDocumentRequest message) throws IOException {
            if (metadata == null) {
                throw new InvalidDocumentException("Les métadonnées doivent arriver en premier");
            }
            received += message.getChunk().size();
            if (received > maxFileSize.toBytes()) {
                throw new InvalidDocumentException("Fichier trop volumineux");
            }
            message.getChunk().writeTo(out);
        }

        @Override
        public synchronized void onError(Throwable t) {
            finished = true;
            cleanup();
        }

        @Override
        public synchronized void onCompleted() {
            if (finished) return;
            finished = true;

            Document saved = null;
            Throwable failure = null;
            InputStream content = null;
            try {
                if (metadata == null) {
                    throw new InvalidDocumentException("Aucun fichier reçu");
                }
                out.close();
                UUID caseId = parseUuid(metadata.getCaseId());
                content = Files.newInputStream(tempFile);
                saved = uploadUseCase.execute(new UploadCommand(
                        caller.cabinetId(), caseId, caller.userId(), caller.authorName(),
                        metadata.getFileName(), received, content));
            } catch (IOException | RuntimeException e) {
                failure = e;
            } finally {
                // Un échec de fermeture après persistance ne change pas le résultat.
                closeQuietly(content);
                cleanup();
            }

            // Une seule réponse terminale, émise après le nettoyage.
            if (failure == null) {
                response.onNext(UploadDocumentReply.newBuilder().setDocument(toSummary(saved)).build());
                response.onCompleted();
            } else {
                response.onError(toStatus(failure).asRuntimeException());
            }
        }

        private void fail(Throwable e) {
            if (finished) return;
            finished = true;
            cleanup();
            response.onError(toStatus(e).asRuntimeException());
        }

        private void cleanup() {
            if (idleTask != null) {
                idleTask.cancel(false);
            }
            closeQuietly(out);
            try {
                if (tempFile != null) Files.deleteIfExists(tempFile);
            } catch (IOException e) {
                LOG.warn("Suppression du fichier temporaire impossible : {}", tempFile, e);
            }
        }
    }

    private static void closeQuietly(Closeable closeable) {
        if (closeable == null) return;
        try {
            closeable.close();
        } catch (IOException e) {
            LOG.warn("Fermeture d'une ressource d'import en échec", e);
        }
    }

    private static UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new InvalidDocumentException("Identifiant de dossier invalide");
        }
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

    private static Status toStatus(Throwable e) {
        if (e instanceof StatusRuntimeException s) {
            return s.getStatus();
        }
        if (e instanceof InvalidDocumentException) {
            return Status.INVALID_ARGUMENT.withDescription(e.getMessage());
        }
        if (e instanceof CaseAccessDeniedException) {
            // Même réponse que le dossier existe ou non, pour ne rien révéler.
            return Status.PERMISSION_DENIED.withDescription("Accès au dossier refusé");
        }
        LOG.error("Erreur interne document-service", e);
        return Status.INTERNAL.withDescription("Erreur interne");
    }
}