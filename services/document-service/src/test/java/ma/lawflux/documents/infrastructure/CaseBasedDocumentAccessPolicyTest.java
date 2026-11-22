package ma.lawflux.documents.infrastructure;

import static org.junit.jupiter.api.Assertions.*;

import io.grpc.Context;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import ma.lawflux.documents.application.CallerContext;
import ma.lawflux.documents.application.CaseAccessChecker;
import ma.lawflux.documents.domain.Document;
import ma.lawflux.documents.domain.DocumentAction;
import ma.lawflux.documents.domain.DocumentType;
import ma.lawflux.platform.grpc.ScaffoldGrpcBoundary;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class CaseBasedDocumentAccessPolicyTest {
    @Test
    void archivedCaseAllowsDownloadButNotShare() {
        UUID cabinet = UUID.randomUUID();
        UUID caseId = UUID.randomUUID();
        UUID user = UUID.randomUUID();
        Jwt token = Jwt.withTokenValue("valid").header("alg", "RS256")
                .subject(user.toString()).claim("cabinet_id", cabinet.toString())
                .claim("realm_access", Map.of("roles", List.of("lawyer"))).build();
        CaseAccessChecker cases = new CaseAccessChecker() {
            @Override public boolean canAccess(UUID c, UUID id) { return canAccess(c, id, "DOCUMENT_READ"); }
            @Override public boolean canAccess(UUID c, UUID id, String action) {
                return cabinet.equals(c) && caseId.equals(id) && "DOCUMENT_READ".equals(action);
            }
        };
        var policy = new CaseBasedDocumentAccessPolicy(cases, new DocumentJwtIdentity(value -> token));
        var document = new Document(UUID.randomUUID(), cabinet, caseId, "contrat.pdf", DocumentType.PDF,
                100, "private-key", user, "Sara", Instant.now());
        Context context = Context.current().withValue(ScaffoldGrpcBoundary.AUTHORIZATION_CONTEXT, "Bearer valid");
        Context previous = context.attach();
        try {
            assertEquals(Set.of(DocumentAction.DOWNLOAD),
                    policy.allowedActions(new CallerContext(user, cabinet, "Sara"), document, true));
        } finally { context.detach(previous); }
    }
}
