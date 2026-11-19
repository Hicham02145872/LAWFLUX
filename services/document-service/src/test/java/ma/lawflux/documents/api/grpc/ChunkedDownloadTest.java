package ma.lawflux.documents.api.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.grpc.Status;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import ma.lawflux.contracts.documents.v1.DownloadDocumentReply;
import ma.lawflux.documents.application.DocumentDownload;
import ma.lawflux.documents.domain.DocumentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ChunkedDownloadTest {

    private final ScheduledExecutorService watchdog = Executors.newSingleThreadScheduledExecutor();

    @AfterEach
    void stopWatchdog() {
        watchdog.shutdownNow();
    }

    private static class TrackingStream extends ByteArrayInputStream {
        volatile boolean closed;

        TrackingStream(byte[] data) {
            super(data);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }

    /** Renvoie 10 octets à la première lecture, puis échoue. */
    private static class FailingStream extends InputStream {
        volatile boolean closed;
        private int reads;

        @Override
        public int read() throws IOException {
            throw new IOException("disque en panne");
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (reads++ == 0) {
                return Math.min(10, len);
            }
            throw new IOException("disque en panne");
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    private static DocumentDownload download(InputStream content, long size) {
        return new DocumentDownload("a.pdf", DocumentType.PDF, size, content);
    }

    @SuppressWarnings("unchecked")
    private static ServerCallStreamObserver<DownloadDocumentReply> mockCall() {
        return mock(ServerCallStreamObserver.class);
    }

    @Test
    void n_envoie_des_morceaux_que_lorsque_le_flux_est_pret() {
        byte[] data = new byte[700_000];
        TrackingStream content = new TrackingStream(data);
        ServerCallStreamObserver<DownloadDocumentReply> call = mockCall();
        ArgumentCaptor<Runnable> onReady = ArgumentCaptor.forClass(Runnable.class);
        when(call.isReady()).thenReturn(true, false);

        ChunkedDownload.start(download(content, data.length), call, watchdog, Duration.ofSeconds(30));
        verify(call).setOnReadyHandler(onReady.capture());
        verify(call, never()).onNext(any());

        onReady.getValue().run();

        // En-tête + un morceau, puis le flux n'est plus prêt : l'envoi s'arrête.
        verify(call, times(2)).onNext(any());
        verify(call, never()).onCompleted();
        assertThat(content.closed).isFalse();

        when(call.isReady()).thenReturn(true);
        onReady.getValue().run();

        verify(call, times(1 + 3)).onNext(any());
        verify(call).onCompleted();
        assertThat(content.closed).isTrue();
    }

    @Test
    void ferme_le_contenu_et_cesse_d_envoyer_quand_le_client_annule() {
        TrackingStream content = new TrackingStream(new byte[700_000]);
        ServerCallStreamObserver<DownloadDocumentReply> call = mockCall();
        ArgumentCaptor<Runnable> onCancel = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Runnable> onReady = ArgumentCaptor.forClass(Runnable.class);
        when(call.isReady()).thenReturn(true);

        ChunkedDownload.start(download(content, 700_000), call, watchdog, Duration.ofSeconds(30));
        verify(call).setOnCancelHandler(onCancel.capture());
        verify(call).setOnReadyHandler(onReady.capture());

        onCancel.getValue().run();
        onReady.getValue().run();

        assertThat(content.closed).isTrue();
        verify(call, never()).onNext(any());
        verify(call, never()).onCompleted();
        verify(call, never()).onError(any());
    }

    @Test
    void coupe_un_telechargement_dont_le_client_ne_lit_plus() {
        TrackingStream content = new TrackingStream(new byte[700_000]);
        ServerCallStreamObserver<DownloadDocumentReply> call = mockCall();
        when(call.isReady()).thenReturn(false);

        ChunkedDownload.start(download(content, 700_000), call, watchdog, Duration.ofMillis(200));

        ArgumentCaptor<Throwable> error = ArgumentCaptor.forClass(Throwable.class);
        verify(call, timeout(3000)).onError(error.capture());
        assertThat(Status.fromThrowable(error.getValue()).getCode())
                .isEqualTo(Status.Code.DEADLINE_EXCEEDED);
        assertThat(content.closed).isTrue();
        verify(call, never()).onNext(any());
        verify(call, never()).onCompleted();
    }

    @Test
    @SuppressWarnings("unchecked")
    void signale_une_erreur_interne_unique_si_la_lecture_echoue_en_cours_de_route() {
        FailingStream content = new FailingStream();
        StreamObserver<DownloadDocumentReply> response = mock(StreamObserver.class);

        ChunkedDownload.start(download(content, 10), response, watchdog, Duration.ofSeconds(30));

        ArgumentCaptor<Throwable> error = ArgumentCaptor.forClass(Throwable.class);
        verify(response).onError(error.capture());
        assertThat(Status.fromThrowable(error.getValue()).getCode()).isEqualTo(Status.Code.INTERNAL);
        verify(response, times(2)).onNext(any());
        verify(response, never()).onCompleted();
        assertThat(content.closed).isTrue();
    }
}