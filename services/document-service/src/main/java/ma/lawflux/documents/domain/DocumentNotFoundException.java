package ma.lawflux.documents.domain;

/** Document inexistant OU appartenant à un autre cabinet : même réponse, pour ne rien révéler. */
public class DocumentNotFoundException extends RuntimeException {
    public DocumentNotFoundException(String message) {
        super(message);
    }
}