package ma.lawflux.documents.application;

import java.util.Set;
import ma.lawflux.documents.domain.Document;

/**
 * Droits de l'appelant sur les commentaires de ce document. Un ensemble vide signifie :
 * l'appelant ne doit même pas savoir que le document existe.
 */
public interface CommentAccessPolicy {
    Set<CommentRight> rights(CallerContext caller, Document document, boolean shared);
}