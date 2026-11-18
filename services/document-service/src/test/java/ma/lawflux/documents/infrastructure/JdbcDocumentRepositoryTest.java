package ma.lawflux.documents.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import javax.sql.DataSource;
import ma.lawflux.documents.domain.Document;
import ma.lawflux.documents.domain.DocumentType;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

@Testcontainers(disabledWithoutDocker = true)
class JdbcDocumentRepositoryTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static JdbcClient jdbc;
    static JdbcDocumentRepository repository;

    private final UUID cabinetA = UUID.randomUUID();
    private final UUID cabinetB = UUID.randomUUID();
    private final UUID caseId = UUID.randomUUID();

    @BeforeAll
    static void migrate() {
        DataSource ds = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        jdbc = JdbcClient.create(ds);
        repository = new JdbcDocumentRepository(jdbc, new DataSourceTransactionManager(ds));
    }

    @BeforeEach
    void clean() {
        jdbc.sql("DELETE FROM documents").update();
    }

    private Document document(UUID cabinet, UUID caze, String name, String key, String createdAt, long size) {
        return new Document(UUID.randomUUID(), cabinet, caze, name, DocumentType.PDF, size,
                key, UUID.randomUUID(), "Karim", Instant.parse(createdAt));
    }

    @Test
    void enregistre_puis_relit_toutes_les_valeurs() {
        Document saved = repository.save(
                document(cabinetA, caseId, "bail.pdf", "k1", "2026-11-01T10:00:00Z", 2048));

        Document read = repository.findByCase(cabinetA, caseId).get(0);

        assertThat(read).isEqualTo(saved);
    }

    @Test
    void un_cabinet_ne_voit_pas_les_documents_d_un_autre_cabinet() {
        repository.save(document(cabinetA, caseId, "a.pdf", "k1", "2026-11-01T10:00:00Z", 10));
        repository.save(document(cabinetB, caseId, "secret.pdf", "k2", "2026-11-01T11:00:00Z", 10));

        assertThat(repository.findByCase(cabinetA, caseId))
                .extracting(Document::name).containsExactly("a.pdf");
        assertThat(repository.findByCase(cabinetB, caseId))
                .extracting(Document::name).containsExactly("secret.pdf");
    }

    @Test
    void ne_renvoie_que_les_documents_du_dossier_demande() {
        UUID otherCase = UUID.randomUUID();
        repository.save(document(cabinetA, caseId, "a.pdf", "k1", "2026-11-01T10:00:00Z", 10));
        repository.save(document(cabinetA, otherCase, "b.pdf", "k2", "2026-11-01T11:00:00Z", 10));

        assertThat(repository.findByCase(cabinetA, caseId))
                .extracting(Document::name).containsExactly("a.pdf");
    }

    @Test
    void trie_du_plus_recent_au_plus_ancien() {
        repository.save(document(cabinetA, caseId, "ancien.pdf", "k1", "2026-11-01T10:00:00Z", 10));
        repository.save(document(cabinetA, caseId, "recent.pdf", "k2", "2026-11-05T10:00:00Z", 10));

        assertThat(repository.findByCase(cabinetA, caseId))
                .extracting(Document::name).containsExactly("recent.pdf", "ancien.pdf");
    }

    @Test
    void refuse_une_cle_de_stockage_en_double_et_une_taille_nulle() {
        repository.save(document(cabinetA, caseId, "a.pdf", "meme-cle", "2026-11-01T10:00:00Z", 10));

        assertThatThrownBy(() -> repository.save(
                document(cabinetA, caseId, "b.pdf", "meme-cle", "2026-11-02T10:00:00Z", 10)))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> repository.save(
                document(cabinetA, caseId, "c.pdf", "autre-cle", "2026-11-03T10:00:00Z", 0)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}