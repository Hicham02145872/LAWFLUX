package ma.lawflux.documents.domain;

/** L'appelant voit le document mais n'a pas le droit de faire cette action. */
public class DocumentAccessDeniedException extends RuntimeException {
    public DocumentAccessDeniedException(String message) {
        super(message);
    }
}