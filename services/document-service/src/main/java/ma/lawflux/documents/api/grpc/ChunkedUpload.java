package ma.lawflux.documents.api.grpc;

import com.google.protobuf.ByteString;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import ma.lawflux.documents.domain.InvalidDocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Réception d'un fichier en client streaming : un message de métadonnées, puis des morceaux.
 * Taille cumulée limitée, délai d'inactivité, fichier temporaire toujours supprimé,
 * une seule réponse terminale. Les méthodes sont synchronisées car le chien de garde
 * s'exécute sur un autre thread que les callbacks gRPC.
 */
abstract class ChunkedUpload<Q, R, M> implements StreamObserver<Q> {

    private static final Logger LOG = LoggerFactory.getLogger(ChunkedUpload.class);

    private final StreamObserver<R> response;
    private final ScheduledExecutorService watchdog;
    private final Duration idleTimeout;
    private final long maxBytes;

    private M metadata;
    private Path tempFile;
    private OutputStream out;
    private long received;
    private boolean finished;
    private ScheduledFuture<?> idleTask;

    ChunkedUpload(StreamObserver<R> response, ScheduledExecutorService watchdog,
                  Duration idleTimeout, long maxBytes) {
        this.response = response;
        this.watchdog = watchdog;
        this.idleTimeout = idleTimeout;
        this.maxBytes = maxBytes;
        armIdleTimer();
    }

    /** Métadonnées portées par ce message, ou null s'il ne s'agit pas du message de métadonnées. */
    protected abstract M metadataOf(Q message);

    /** Morceau porté par ce message, ou null s'il ne s'agit pas d'un morceau. */
    protected abstract ByteString chunkOf(Q message);

    /** Appelé une fois tous les octets reçus ; peut lever une exception métier. */
    protected abstract R complete(M metadata, InputStream content, long sizeBytes);

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
    public synchronized void onNext(Q message) {
        if (finished) return;
        armIdleTimer();
        try {
            M meta = metadataOf(message);
            if (meta != null) {
                start(meta);
                return;
            }
            ByteString chunk = chunkOf(message);
            if (chunk == null) {
                throw new InvalidDocumentException("Message d'import vide");
            }
            append(chunk);
        } catch (IOException | RuntimeException e) {
            fail(e);
        }
    }

    private void start(M meta) throws IOException {
        if (metadata != null) {
            throw new InvalidDocumentException("Métadonnées déjà reçues");
        }
        metadata = meta;
        tempFile = Files.createTempFile("lawflux-upload-", ".part");
        out = Files.newOutputStream(tempFile);
    }

    private void append(ByteString chunk) throws IOException {
        if (metadata == null) {
            throw new InvalidDocumentException("Les métadonnées doivent arriver en premier");
        }
        received += chunk.size();
        if (received > maxBytes) {
            throw new InvalidDocumentException("Fichier trop volumineux");
        }
        chunk.writeTo(out);
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

        R result = null;
        Throwable failure = null;
        InputStream content = null;
        try {
            if (metadata == null) {
                throw new InvalidDocumentException("Aucun fichier reçu");
            }
            out.close();
            content = Files.newInputStream(tempFile);
            result = complete(metadata, content, received);
        } catch (IOException | RuntimeException e) {
            failure = e;
        } finally {
            // Un échec de fermeture après persistance ne change pas le résultat.
            closeQuietly(content);
            cleanup();
        }

        // Une seule réponse terminale, émise après le nettoyage.
        if (failure == null) {
            response.onNext(result);
            response.onCompleted();
        } else {
            response.onError(GrpcErrors.toStatus(failure).asRuntimeException());
        }
    }

    private void fail(Throwable e) {
        if (finished) return;
        finished = true;
        cleanup();
        response.onError(GrpcErrors.toStatus(e).asRuntimeException());
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

    private static void closeQuietly(Closeable closeable) {
        if (closeable == null) return;
        try {
            closeable.close();
        } catch (IOException e) {
            LOG.warn("Fermeture d'une ressource d'import en échec", e);
        }
    }
}