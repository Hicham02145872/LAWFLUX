package ma.lawflux.cases.application;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import ma.lawflux.cases.domain.AccessKind;
import ma.lawflux.cases.domain.CaseAccessRepository;
import ma.lawflux.cases.domain.CaseGrant;
import org.junit.jupiter.api.Test;

class CaseAccessServiceTest {
    private final UUID cabinet = UUID.randomUUID();
    private final UUID otherCabinet = UUID.randomUUID();
    private final UUID caseId = UUID.randomUUID();
    private final UUID managerId = UUID.randomUUID();
    private final UUID lawyerId = UUID.randomUUID();
    private final UUID clientId = UUID.randomUUID();
    private final MemoryRepository repository = new MemoryRepository(Map.of(caseId, cabinet));
    private final CaseAccessService access = new CaseAccessService(repository);

    private CaseCaller caller(UUID userId, UUID cabinetId, String role) {
        return new CaseCaller(userId, cabinetId, Set.of(role));
    }

    @Test
    void managerCanAccessOwnCaseButNeverAnotherCabinet() {
        assertTrue(access.allowed(caller(managerId, cabinet, "cabinet_manager"), caseId, "DOCUMENT_READ"));
        assertFalse(access.allowed(caller(managerId, otherCabinet, "cabinet_manager"), caseId, "DOCUMENT_READ"));
        assertFalse(access.allowed(caller(managerId, cabinet, "cabinet_manager"), UUID.randomUUID(), "DOCUMENT_READ"));
        assertFalse(access.allowed(caller(managerId, cabinet, "cabinet_manager"), caseId, "UNKNOWN"));
    }

    @Test
    void assignmentAndRevocationControlLawyerAndClientAccess() {
        CaseCaller manager = caller(managerId, cabinet, "cabinet_manager");
        CaseCaller lawyer = caller(lawyerId, cabinet, "lawyer");
        CaseCaller client = caller(clientId, cabinet, "client");
        assertFalse(access.allowed(lawyer, caseId, "DOCUMENT_READ"));
        assertFalse(access.allowed(client, caseId, "DOCUMENT_READ"));

        access.grant(manager, caseId, lawyerId, AccessKind.STAFF);
        access.grant(manager, caseId, clientId, AccessKind.CLIENT);
        assertTrue(access.allowed(lawyer, caseId, "DOCUMENT_READ"));
        assertTrue(access.allowed(lawyer, caseId, "DOCUMENT_SHARE"));
        assertTrue(access.allowed(client, caseId, "DOCUMENT_READ"));
        assertFalse(access.allowed(client, caseId, "DOCUMENT_WRITE"));
        assertFalse(access.allowed(caller(clientId, cabinet, "assistant"), caseId, "DOCUMENT_SHARE"));
        assertEquals(2, access.grants(manager, caseId).size());

        access.revoke(manager, caseId, lawyerId, AccessKind.STAFF);
        access.revoke(manager, caseId, clientId, AccessKind.CLIENT);
        assertFalse(access.allowed(lawyer, caseId, "DOCUMENT_READ"));
        assertFalse(access.allowed(client, caseId, "DOCUMENT_READ"));
    }

    @Test
    void onlyManagerCanAssignAndArchivedCaseIsReadOnly() {
        assertThrows(SecurityException.class, () -> access.grant(
                caller(lawyerId, cabinet, "lawyer"), caseId, clientId, AccessKind.CLIENT));
        repository.archived = true;
        CaseCaller manager = caller(managerId, cabinet, "cabinet_manager");
        assertTrue(access.allowed(manager, caseId, "DOCUMENT_READ"));
        assertFalse(access.allowed(manager, caseId, "DOCUMENT_WRITE"));
        assertThrows(SecurityException.class, () -> access.grant(manager, caseId, clientId, AccessKind.CLIENT));
    }

    private static class MemoryRepository implements CaseAccessRepository {
        private final Map<UUID, UUID> cases;
        private final Set<String> grants = new HashSet<>();
        boolean archived;

        MemoryRepository(Map<UUID, UUID> cases) { this.cases = cases; }
        private String key(UUID cabinet, UUID caseId, UUID user, AccessKind kind) {
            return cabinet + ":" + caseId + ":" + user + ":" + kind;
        }
        @Override public boolean caseExists(UUID cabinet, UUID caseId) { return cabinet.equals(cases.get(caseId)); }
        @Override public boolean caseArchived(UUID cabinet, UUID caseId) { return archived && caseExists(cabinet, caseId); }
        @Override public boolean hasActiveGrant(UUID cabinet, UUID caseId, UUID user, AccessKind kind) {
            return grants.contains(key(cabinet, caseId, user, kind));
        }
        @Override public List<CaseGrant> activeGrants(UUID cabinet, UUID caseId) {
            List<CaseGrant> found = new ArrayList<>();
            for (String grant : grants) {
                String[] values = grant.split(":");
                if (values[0].equals(cabinet.toString()) && values[1].equals(caseId.toString()))
                    found.add(new CaseGrant(UUID.fromString(values[2]), AccessKind.valueOf(values[3]), Instant.now()));
            }
            return found;
        }
        @Override public void grant(UUID cabinet, UUID caseId, UUID user, AccessKind kind, UUID actor) {
            grants.add(key(cabinet, caseId, user, kind));
        }
        @Override public void revoke(UUID cabinet, UUID caseId, UUID user, AccessKind kind, UUID actor) {
            grants.remove(key(cabinet, caseId, user, kind));
        }
    }
}
