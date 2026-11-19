package ma.lawflux.documents.application;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import ma.lawflux.documents.domain.Document;
import ma.lawflux.documents.domain.DocumentAccessDeniedException;
import ma.lawflux.documents.domain.DocumentAction;
import ma.lawflux.documents.domain.DocumentNotFoundException;
import ma.lawflux.documents.domain.DocumentShareRepository;

final class SharingRules {

    record Loaded(Document document, boolean shared, Set<DocumentAction> rights) {
    }

    private SharingRules() {
    }

    static Loaded load(DocumentShareRepository shares, DocumentAccessPolicy policy,
                       CallerContext caller, UUID documentId) {
        Document document = shares.findDocument(caller.cabinetId(), documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document introuvable"));
        boolean shared = shares.findActiveShare(caller.cabinetId(), documentId).isPresent();
        Set<DocumentAction> rights = policy.allowedActions(caller, document, shared);
        if (rights.isEmpty()) {
            // Aucun droit : on ne révèle pas l'existence du document.
            throw new DocumentNotFoundException("Document introuvable");
        }
        return new Loaded(document, shared, rights);
    }

    static void require(Loaded loaded, DocumentAction action) {
        if (!loaded.rights().contains(action)) {
            throw new DocumentAccessDeniedException("Action non autorisée");
        }
    }

    /** Actions proposées à l'écran : on ne propose pas Partager si c'est déjà partagé, ni l'inverse. */
    static Set<DocumentAction> applicable(Set<DocumentAction> rights, boolean shared) {
        EnumSet<DocumentAction> result = EnumSet.noneOf(DocumentAction.class);
        result.addAll(rights);
        result.remove(shared ? DocumentAction.SHARE : DocumentAction.UNSHARE);
        return result;
    }

    static SharingStatus statusAfter(DocumentAccessPolicy policy, CallerContext caller,
                                     Document document, boolean shared) {
        return new SharingStatus(document, shared,
                applicable(policy.allowedActions(caller, document, shared), shared));
    }
}