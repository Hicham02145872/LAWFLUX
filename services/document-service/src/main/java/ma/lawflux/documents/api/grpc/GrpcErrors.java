package ma.lawflux.documents.api.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.UUID;
import ma.lawflux.documents.domain.CaseAccessDeniedException;
import ma.lawflux.documents.domain.DocumentNotFoundException;
import ma.lawflux.documents.domain.InvalidDocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class GrpcErrors {

    private static final Logger LOG = LoggerFactory.getLogger(GrpcErrors.class);

    private GrpcErrors() {
    }

    static Status toStatus(Throwable e) {
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
        if (e instanceof DocumentNotFoundException) {
            // Document inexistant ou d'un autre cabinet : même réponse dans les deux cas.
            return Status.NOT_FOUND.withDescription("Document introuvable");
        }
        LOG.error("Erreur interne document-service", e);
        return Status.INTERNAL.withDescription("Erreur interne");
    }

    static UUID parseUuid(String value, String message) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new InvalidDocumentException(message);
        }
    }
}