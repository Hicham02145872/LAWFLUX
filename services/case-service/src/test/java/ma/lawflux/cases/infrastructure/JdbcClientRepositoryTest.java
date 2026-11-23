package ma.lawflux.cases.infrastructure;

import java.util.UUID;

import org.flywaydb.core.Flyway;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import ma.lawflux.cases.domain.ClientStatus;

@Testcontainers(disabledWithoutDocker = true)
class JdbcClientRepositoryTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    static JdbcTemplate jdbc;
    static JdbcClientRepository repository;

    @BeforeAll
    static void migrate() {
        var source = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword()
        );

        Flyway.configure()
                .dataSource(source)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        jdbc = new JdbcTemplate(source);
        repository = new JdbcClientRepository(jdbc);
    }

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM clients");
    }

    @Test
    void clientCrudIsTenantScopedAndSupportsStatusFilters() {
        UUID cabinet = UUID.randomUUID();
        UUID otherCabinet = UUID.randomUUID();

        var first = repository.create(
                cabinet, "Client A", "a@example.com", "0600000000", "Casa"
        );
        var second = repository.create(
                cabinet, "Client B", "b@example.com", null, null
        );
        var foreign = repository.create(
                otherCabinet, "Foreign client", null, null, null
        );

        assertEquals("Client A", repository.findById(cabinet, first.id()).orElseThrow().name());
        assertTrue(repository.findById(otherCabinet, first.id()).isEmpty());

        assertTrue(repository.update(
                cabinet, first.id(), "Client A updated",
                "new@example.com", "0611111111", "Rabat"
        ));
        var updated = repository.findById(cabinet, first.id()).orElseThrow();
        assertEquals("Client A updated", updated.name());
        assertEquals("new@example.com", updated.email());

        assertTrue(repository.archive(cabinet, second.id()));
        var archived = repository.findById(cabinet, second.id()).orElseThrow();
        assertEquals(ClientStatus.ARCHIVED, archived.status());
        assertNotNull(archived.archivedAt());

        assertFalse(repository.update(
                otherCabinet, first.id(), "Wrong tenant", null, null, null
        ));
        assertFalse(repository.archive(otherCabinet, first.id()));

        assertEquals(1L, repository.count(cabinet, ClientStatus.ACTIVE));
        assertEquals(1L, repository.count(cabinet, ClientStatus.ARCHIVED));
        assertEquals(1L, repository.count(otherCabinet, ClientStatus.ACTIVE));

        assertEquals(1, repository.findPage(
                cabinet, ClientStatus.ACTIVE, 10, 0
        ).size());
        assertEquals(1, repository.findPage(
                cabinet, ClientStatus.ARCHIVED, 10, 0
        ).size());

        assertTrue(repository.findById(otherCabinet, foreign.id()).isPresent());
    }
}