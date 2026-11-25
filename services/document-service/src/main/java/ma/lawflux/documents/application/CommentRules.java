package ma.lawflux.documents.application;

import java.util.UUID;
import ma.lawflux.documents.domain.Document;
import ma.lawflux.documents.domain.DocumentNotFoundException;
import ma.lawflux.documents.domain.DocumentShareRepository;
import ma.lawflux.documents.domain.InvalidDocumentException;

final class CommentRules {

    static final int MAX_BODY_LENGTH = 5000;

    record Loaded(Document document, java.util.Set<CommentRight> rights) {
    }

    private CommentRules() {
    }

    static Loaded load(DocumentShareRepository documents, CommentAccessPolicy policy,
                       CallerContext caller, UUID documentId) {
        Document document = documents.findDocument(caller.cabinetId(), documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document introuvable"));
        boolean shared = documents.findActiveShare(caller.cabinetId(), documentId).isPresent();
        java.util.Set<CommentRight> rights = policy.rights(caller, document, shared);
        if (rights.isEmpty()) {
            // Aucun droit : on ne révèle pas l'existence du document.
            throw new DocumentNotFoundException("Document introuvable");
        }
        return new Loaded(document, rights);
    }

    static String validBody(String raw) {
        String body = raw == null ? "" : raw.trim();
        if (body.isEmpty()) {
            throw new InvalidDocumentException("Commentaire vide");
        }
        // PostgreSQL compte des caractères (points de code), pas des unités UTF-16.
        if (body.codePointCount(0, body.length()) > MAX_BODY_LENGTH) {
            throw new InvalidDocumentException("Commentaire trop long");
        }
        return body;
    }
}