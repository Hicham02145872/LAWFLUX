package ma.lawflux.documents.application;

import java.util.Set;
import ma.lawflux.documents.domain.Document;
import ma.lawflux.documents.domain.DocumentAction;

public record SharingStatus(Document document, boolean shared, Set<DocumentAction> allowedActions) {
}