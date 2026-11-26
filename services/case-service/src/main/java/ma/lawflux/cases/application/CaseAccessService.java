package ma.lawflux.cases.application;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import ma.lawflux.cases.domain.AccessKind;
import ma.lawflux.cases.domain.CaseAccessRepository;
import ma.lawflux.cases.domain.CaseGrant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CaseAccessService {
    private final CaseAccessRepository repository;

    public CaseAccessService(CaseAccessRepository repository) { this.repository = repository; }

    public boolean allowed(CaseCaller caller, UUID caseId, String requestedAction) {
        CaseAction action = CaseAction.parse(requestedAction);
        if (caller == null || caseId == null || action == null
                || !repository.caseExists(caller.cabinetId(), caseId)) return false;
        if (!action.isRead() && repository.caseArchived(caller.cabinetId(), caseId)) return false;

        if (caller.hasRole("cabinet_manager")) return true;
        if (caller.hasRole("client")) {
            return (action.isRead() || action == CaseAction.DOCUMENT_COMMENT) && repository.hasActiveGrant(
                    caller.cabinetId(), caseId, caller.userId(), AccessKind.CLIENT);
        }
        if (caller.hasRole("lawyer") || caller.hasRole("assistant")) {
            if (!repository.hasActiveGrant(caller.cabinetId(), caseId, caller.userId(), AccessKind.STAFF))
                return false;
            return switch (action) {
                case CASE_READ, DOCUMENT_READ, DOCUMENT_WRITE, DOCUMENT_COMMENT -> true;
                case DOCUMENT_SHARE -> caller.hasRole("lawyer");
                case CASE_ASSIGN, CLIENT_PORTAL_READ -> false;
            };
        }
        return false;
    }

    public List<CaseGrant> grants(CaseCaller caller, UUID caseId) {
        requireManager(caller, caseId);
        return repository.activeGrants(caller.cabinetId(), caseId);
    }

    @Transactional
    public void grant(CaseCaller caller, UUID caseId, UUID userId, AccessKind kind) {
        requireManager(caller, caseId);
        if (repository.caseArchived(caller.cabinetId(), caseId)) throw new SecurityException("Dossier archivé");
        repository.grant(caller.cabinetId(), caseId, userId, kind, caller.userId());
    }

    @Transactional
    public void revoke(CaseCaller caller, UUID caseId, UUID userId, AccessKind kind) {
        requireManager(caller, caseId);
        repository.revoke(caller.cabinetId(), caseId, userId, kind, caller.userId());
    }

    private void requireManager(CaseCaller caller, UUID caseId) {
        if (caller == null || !caller.hasRole("cabinet_manager")) throw new SecurityException("Accès refusé");
        if (caseId == null || !repository.caseExists(caller.cabinetId(), caseId))
            throw new NoSuchElementException("Dossier introuvable");
    }
}
