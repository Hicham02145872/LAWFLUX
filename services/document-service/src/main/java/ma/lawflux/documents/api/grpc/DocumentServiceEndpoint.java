package ma.lawflux.documents.api.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
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
import ma.lawflux.documents.domain.Document;
import ma.lawflux.documents.domain.InvalidDocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;

@Service
public class DocumentServiceEndpoint extends DocumentServiceGrpc.DocumentServiceImplBase {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentServiceEndpoint.class);

    private final ListDocumentsUseCase listUseCase;
    private final UploadDocumentUseCase uploadUseCase;
    private final CallerContextProvider callerContext;
    private final DataSize maxFileSize;

    public DocumentServiceEndpoint(
            ListDocumentsUseCase listUseCase,
            UploadDocumentUseCase uploadUseCase,
            CallerContextProvider callerContext,
            @Value("${lawflux.documents.max-file-size}") DataSize maxFileSize) {
        this.listUseCase = listUseCase;
        this.uploadUseCase = uploadUseCase;
        this.callerContext = callerContext;
        this.maxFileSize = maxFileSize;
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

    private final class UploadStream implements StreamObserver<UploadDocumentRequest> {

        private final CallerContext caller;
        private final StreamObserver<UploadDocumentReply> response;
        private UploadMetadata metadata;
        private Path tempFile;
        private OutputStream out;
        private long received;
        private boolean finished;

        UploadStream(CallerContext caller, StreamObserver<UploadDocumentReply> response) {
            this.caller = caller;
            this.response = response;
        }

        @Override
        public void onNext(UploadDocumentRequest message) {
            if (finished) return;
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
        public void onError(Throwable t) {
            finished = true;
            cleanup();
        }

        @Override
        public void onCompleted() {
            if (finished) return;
            finished = true;
            try {
                if (metadata == null) {
                    throw new InvalidDocumentException("Aucun fichier reçu");
                }
                out.close();
                UUID caseId = parseUuid(metadata.getCaseId());
                try (InputStream content = Files.newInputStream(tempFile)) {
                    Document saved = uploadUseCase.execute(new UploadCommand(
                            caller.cabinetId(), caseId, caller.userId(), caller.authorName(),
                            metadata.getFileName(), received, content));
                    response.onNext(UploadDocumentReply.newBuilder()
                            .setDocument(toSummary(saved)).build());
                    response.onCompleted();
                }
            } catch (IOException | RuntimeException e) {
                response.onError(toStatus(e).asRuntimeException());
            } finally {
                cleanup();
            }
        }

        private void fail(Throwable e) {
            finished = true;
            cleanup();
            response.onError(toStatus(e).asRuntimeException());
        }

        private void cleanup() {
            try {
                if (out != null) out.close();
            } catch (IOException ignored) {
                // rien à faire
            }
            try {
                if (tempFile != null) Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {
                // rien à faire
            }
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
        LOG.error("Erreur interne document-service", e);
        return Status.INTERNAL.withDescription("Erreur interne");
    }
}