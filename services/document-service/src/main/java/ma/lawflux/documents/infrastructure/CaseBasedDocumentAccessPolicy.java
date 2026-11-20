package ma.lawflux.documents.infrastructure;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ma.lawflux.documents.application.CallerContext;
import ma.lawflux.documents.application.CaseAccessChecker;
import ma.lawflux.documents.application.DocumentAccessPolicy;
import ma.lawflux.documents.domain.Document;
import ma.lawflux.documents.domain.DocumentAction;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "lawflux.documents", name = "business-enabled", havingValue = "true")
public class CaseBasedDocumentAccessPolicy implements DocumentAccessPolicy {
    private final CaseAccessChecker cases;
    private final DocumentJwtIdentity identity;

    public CaseBasedDocumentAccessPolicy(CaseAccessChecker cases, DocumentJwtIdentity identity) {
        this.cases = cases;
        this.identity = identity;
    }

    @Override
    public Set<DocumentAction> allowedActions(CallerContext caller, Document document, boolean shared) {
        if (!caller.cabinetId().equals(document.cabinetId())
                || !cases.canAccess(caller.cabinetId(), document.caseId())) return Set.of();
        Object access = identity.jwt().getClaim("realm_access");
        Object roles = access instanceof Map<?, ?> map ? map.get("roles") : null;
        if (!(roles instanceof List<?> list)) return Set.of();
        if (list.contains("cabinet_manager") || list.contains("lawyer")) {
            return EnumSet.allOf(DocumentAction.class);
        }
        if (list.contains("assistant")) return EnumSet.of(DocumentAction.DOWNLOAD);
        if (list.contains("client") && shared) return EnumSet.of(DocumentAction.DOWNLOAD);
        return Set.of();
    }
}
