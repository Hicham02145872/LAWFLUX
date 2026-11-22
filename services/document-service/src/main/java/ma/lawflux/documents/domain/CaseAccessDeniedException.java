package ma.lawflux.documents.domain;

public class CaseAccessDeniedException extends RuntimeException {
    public CaseAccessDeniedException(String message) {
        super(message);
    }
}