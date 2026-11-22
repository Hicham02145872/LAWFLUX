package ma.lawflux.documents.application;

import java.util.Set;
import ma.lawflux.documents.domain.Document;
import ma.lawflux.documents.domain.DocumentAction;

/**
 * À implémenter avec la vérification des droits du chef (gRPC / case-service).
 * Renvoie les droits de l'appelant sur ce document. SHARE et UNSHARE ne dépendent pas
 * de l'état de partage ; DOWNLOAD peut en dépendre (un client ne télécharge que ce qui
 * lui est partagé). Un ensemble vide signifie : l'appelant ne doit même pas savoir
 * que le document existe.
 */
public interface DocumentAccessPolicy {
    Set<DocumentAction> allowedActions(CallerContext caller, Document document, boolean shared);
}