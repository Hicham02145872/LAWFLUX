package ma.lawflux.documents.api.grpc;

import com.google.protobuf.ByteString;
import io.grpc.Status;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import ma.lawflux.contracts.documents.v1.DownloadDocumentReply;
import ma.lawflux.contracts.documents.v1.DownloadHeader;
import ma.lawflux.documents.application.DocumentDownload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Envoi d'un fichier en server streaming : un en-tête, puis des morceaux de 256 Ko.
 * Avec un vrai appel gRPC, un morceau n'est envoyé que si le flux est prêt (contrôle de flux)
 * et un délai d'inactivité coupe l'envoi si le client ne lit plus. Le contenu est toujours
 * fermé et une seule réponse terminale est émise. Les méthodes sont synchronisées car le
 * chien de garde s'exécute sur un autre thread que les callbacks gRPC.
 */
final class ChunkedDownload {

    static final int CHUNK_SIZE = 256 * 1024;

    private static final Logger LOG = LoggerFactory.getLogger(ChunkedDownload.class);

    private final DocumentDownload download;
    private final StreamObserver<DownloadDocumentReply> response;
    private final ServerCallStreamObserver<DownloadDocumentReply> call;
    private final ScheduledExecutorService watchdog;
    private final Duration idleTimeout;
    private final byte[] buffer = new byte[CHUNK_SIZE];
    private boolean headerSent;
    private boolean finished;
    private ScheduledFuture<?> idleTask;

    private ChunkedDownload(DocumentDownload download, StreamObserver<DownloadDocumentReply> response,
                            ScheduledExecutorService watchdog, Duration idleTimeout) {
        this.download = download;
        this.response = response;
        this.call = response instanceof ServerCallStreamObserver<?>
                ? (ServerCallStreamObserver<DownloadDocumentReply>) response : null;
        this.watchdog = watchdog;
        this.idleTimeout = idleTimeout;
    }

    static void start(DocumentDownload download, StreamObserver<DownloadDocumentReply> response,
                      ScheduledExecutorService watchdog, Duration idleTimeout) {
        new ChunkedDownload(download, response, watchdog, idleTimeout).begin();
    }

    private void begin() {
        if (call == null) {
            // Observateur simple (tests) : pas de contrôle de flux, envoi immédiat.
            pump();
            return;
        }
        call.setOnCancelHandler(this::abort);
        armIdleTimer();
        call.setOnReadyHandler(this::pump);
    }

    private synchronized void pump() {
        if (finished) return;
        try {
            if (!headerSent) {
                response.onNext(DownloadDocumentReply.newBuilder()
                        .setHeader(DownloadHeader.newBuilder()
                                .setFileName(download.fileName())
                                .setType(download.type().name())
                                .setSizeBytes(download.sizeBytes()))
                        .build());
                headerSent = true;
            }
            while (call == null || (call.isReady() && !call.isCancelled())) {
                int read = download.content().read(buffer);
                if (read < 0) {
                    finished = true;
                    release();
                    response.onCompleted();
                    return;
                }
                response.onNext(DownloadDocumentReply.newBuilder()
                        .setChunk(ByteString.copyFrom(buffer, 0, read))
                        .build());
                armIdleTimer();
            }
        } catch (IOException | RuntimeException e) {
            fail(GrpcErrors.toStatus(e));
        }
    }

    private synchronized void armIdleTimer() {
        if (watchdog == null || call == null) return;
        if (idleTask != null) {
            idleTask.cancel(false);
        }
        idleTask = watchdog.schedule(this::expire, idleTimeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    private synchronized void expire() {
        if (finished) return;
        LOG.warn("Téléchargement interrompu : le client ne lit plus");
        fail(Status.DEADLINE_EXCEEDED.withDescription("Délai d'inactivité dépassé"));
    }

    private synchronized void abort() {
        finished = true;
        release();
    }

    private void fail(Status status) {
        boolean alreadyTerminated = finished;
        finished = true;
        release();
        if (alreadyTerminated) return;
        try {
            response.onError(status.asRuntimeException());
        } catch (RuntimeException e) {
            LOG.debug("Réponse d'erreur non délivrée (appel déjà fermé)", e);
        }
    }

    private void release() {
        if (idleTask != null) {
            idleTask.cancel(false);
        }
        try {
            download.content().close();
        } catch (IOException e) {
            LOG.warn("Fermeture du contenu en échec", e);
        }
    }
}