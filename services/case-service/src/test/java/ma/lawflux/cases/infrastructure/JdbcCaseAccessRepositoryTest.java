package ma.lawflux.cases.infrastructure;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import ma.lawflux.cases.domain.AccessKind;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class JdbcCaseAccessRepositoryTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    static JdbcTemplate jdbc;
    static JdbcCaseAccessRepository repository;

    @BeforeAll
    static void migrate() {
        var source = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();
        jdbc = new JdbcTemplate(source);
        repository = new JdbcCaseAccessRepository(jdbc);
    }

    @BeforeEach
    void clean() { jdbc.update("DELETE FROM case_access_grants"); jdbc.update("DELETE FROM case_records"); }

    @Test
    void grantsAreTenantScopedIdempotentAndRevocable() {
        UUID cabinet = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        UUID caseId = UUID.randomUUID();
        UUID lawyer = UUID.randomUUID();
        UUID manager = UUID.randomUUID();
        jdbc.update("INSERT INTO case_records (id, cabinet_id, title, status) VALUES (?, ?, ?, ?)",
                caseId, cabinet, "Dossier commercial", "OPEN");

        assertTrue(repository.caseExists(cabinet, caseId));
        assertFalse(repository.caseExists(other, caseId));
        repository.grant(cabinet, caseId, lawyer, AccessKind.STAFF, manager);
        repository.grant(cabinet, caseId, lawyer, AccessKind.STAFF, manager);
        assertEquals(1, repository.activeGrants(cabinet, caseId).size());
        assertTrue(repository.hasActiveGrant(cabinet, caseId, lawyer, AccessKind.STAFF));
        assertFalse(repository.hasActiveGrant(other, caseId, lawyer, AccessKind.STAFF));
        assertFalse(repository.hasActiveGrant(cabinet, caseId, lawyer, AccessKind.CLIENT));

        repository.revoke(cabinet, caseId, lawyer, AccessKind.STAFF, manager);
        assertFalse(repository.hasActiveGrant(cabinet, caseId, lawyer, AccessKind.STAFF));
        repository.grant(cabinet, caseId, lawyer, AccessKind.STAFF, manager);
        assertTrue(repository.hasActiveGrant(cabinet, caseId, lawyer, AccessKind.STAFF));
        Integer history = jdbc.queryForObject("SELECT count(*) FROM case_access_grants WHERE case_id = ?",
                Integer.class, caseId);
        assertEquals(2, history);
    }
}
