package ma.lawflux.cases.domain;

import java.util.List;
import java.util.UUID;

public interface CaseAccessRepository {
    boolean caseExists(UUID cabinetId, UUID caseId);
    boolean caseArchived(UUID cabinetId, UUID caseId);
    boolean hasActiveGrant(UUID cabinetId, UUID caseId, UUID userId, AccessKind kind);
    List<CaseGrant> activeGrants(UUID cabinetId, UUID caseId);
    void grant(UUID cabinetId, UUID caseId, UUID userId, AccessKind kind, UUID actorId);
    void revoke(UUID cabinetId, UUID caseId, UUID userId, AccessKind kind, UUID actorId);
}
