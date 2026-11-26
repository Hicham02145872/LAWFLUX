package ma.lawflux.cases.application;

public enum CaseAction {
    CASE_READ,
    DOCUMENT_READ,
    DOCUMENT_WRITE,
    DOCUMENT_SHARE,
    DOCUMENT_COMMENT,
    CASE_ASSIGN,
    CLIENT_PORTAL_READ;

    public static CaseAction parse(String value) {
        try { return CaseAction.valueOf(value); }
        catch (IllegalArgumentException | NullPointerException e) { return null; }
    }

    public boolean isRead() {
        return this == CASE_READ || this == DOCUMENT_READ || this == CLIENT_PORTAL_READ;
    }
}
