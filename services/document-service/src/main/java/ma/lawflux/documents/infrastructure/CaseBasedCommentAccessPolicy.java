package ma.lawflux.documents.infrastructure;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ma.lawflux.documents.application.CallerContext;
import ma.lawflux.documents.application.CaseAccessChecker;
import ma.lawflux.documents.application.CommentAccessPolicy;
import ma.lawflux.documents.application.CommentRight;
import ma.lawflux.documents.domain.Document;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "lawflux.documents", name = "business-enabled", havingValue = "true")
public class CaseBasedCommentAccessPolicy implements CommentAccessPolicy {

    private final CaseAccessChecker cases;
    private final DocumentJwtIdentity identity;

    public CaseBasedCommentAccessPolicy(CaseAccessChecker cases, DocumentJwtIdentity identity) {
        this.cases = cases;
        this.identity = identity;
    }

    @Override
    public Set<CommentRight> rights(CallerContext caller, Document document, boolean shared) {
        if (!caller.cabinetId().equals(document.cabinetId())
                || !cases.canAccess(caller.cabinetId(), document.caseId(), "DOCUMENT_READ")) {
            return Set.of();
        }
        List<?> roles = roles();
        if (roles.contains("cabinet_manager") || roles.contains("lawyer") || roles.contains("assistant")) {
            EnumSet<CommentRight> rights = EnumSet.of(CommentRight.READ_INTERNAL, CommentRight.READ_SHARED);
            if (cases.canAccess(caller.cabinetId(), document.caseId(), "DOCUMENT_WRITE")) {
                rights.add(CommentRight.WRITE_INTERNAL);
                rights.add(CommentRight.WRITE_SHARED);
            }
            return rights;
        }
        // Un client ne voit que les échanges partagés, sur un document qui lui est encore partagé.
        if (roles.contains("client") && shared) {
            EnumSet<CommentRight> rights = EnumSet.of(CommentRight.READ_SHARED);
            if (cases.canAccess(caller.cabinetId(), document.caseId(), "DOCUMENT_COMMENT")) {
                rights.add(CommentRight.WRITE_SHARED);
            }
            return rights;
        }
        return Set.of();
    }

    private List<?> roles() {
        Object access = identity.jwt().getClaim("realm_access");
        Object roles = access instanceof Map<?, ?> map ? map.get("roles") : null;
        return roles instanceof List<?> list ? list : List.of();
    }
}
