package ma.lawflux.cases.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import ma.lawflux.cases.domain.AccessKind;
import ma.lawflux.cases.domain.CaseAccessRepository;
import ma.lawflux.cases.domain.CaseGrant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcCaseAccessRepository implements CaseAccessRepository {
    private final JdbcTemplate jdbc;

    public JdbcCaseAccessRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public boolean caseExists(UUID cabinetId, UUID caseId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM case_records WHERE cabinet_id = ? AND id = ?)",
                Boolean.class, cabinetId, caseId));
    }

    @Override
    public boolean caseArchived(UUID cabinetId, UUID caseId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM case_records WHERE cabinet_id = ? AND id = ? "
                        + "AND (status = 'ARCHIVED' OR archived_at IS NOT NULL))",
                Boolean.class, cabinetId, caseId));
    }

    @Override
    public boolean hasActiveGrant(UUID cabinetId, UUID caseId, UUID userId, AccessKind kind) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM case_access_grants WHERE cabinet_id = ? AND case_id = ? "
                        + "AND user_id = ? AND kind = ? AND revoked_at IS NULL)",
                Boolean.class, cabinetId, caseId, userId, kind.name()));
    }

    @Override
    public List<CaseGrant> activeGrants(UUID cabinetId, UUID caseId) {
        return jdbc.query("SELECT user_id, kind, granted_at FROM case_access_grants "
                        + "WHERE cabinet_id = ? AND case_id = ? AND revoked_at IS NULL "
                        + "ORDER BY granted_at, user_id",
                (rs, rowNum) -> mapGrant(rs), cabinetId, caseId);
    }

    @Override
    public void grant(UUID cabinetId, UUID caseId, UUID userId, AccessKind kind, UUID actorId) {
        jdbc.update("INSERT INTO case_access_grants "
                        + "(id, case_id, cabinet_id, user_id, kind, granted_by, granted_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, now()) "
                        + "ON CONFLICT (case_id, user_id, kind) WHERE revoked_at IS NULL DO NOTHING",
                UUID.randomUUID(), caseId, cabinetId, userId, kind.name(), actorId);
    }

    @Override
    public void revoke(UUID cabinetId, UUID caseId, UUID userId, AccessKind kind, UUID actorId) {
        jdbc.update("UPDATE case_access_grants SET revoked_by = ?, revoked_at = now() "
                        + "WHERE cabinet_id = ? AND case_id = ? AND user_id = ? AND kind = ? "
                        + "AND revoked_at IS NULL",
                actorId, cabinetId, caseId, userId, kind.name());
    }

    private static CaseGrant mapGrant(ResultSet rs) throws SQLException {
        return new CaseGrant(rs.getObject("user_id", UUID.class),
                AccessKind.valueOf(rs.getString("kind")),
                rs.getTimestamp("granted_at").toInstant());
    }
}
