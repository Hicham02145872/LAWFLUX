package ma.lawflux.documents.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import ma.lawflux.documents.domain.Document;
import ma.lawflux.documents.domain.DocumentNotFoundException;
import ma.lawflux.documents.domain.DocumentType;
import ma.lawflux.documents.domain.DocumentVersion;
import ma.lawflux.documents.domain.NewVersion;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class JdbcDocumentVersionRepositoryTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static JdbcClient jdbc;
    static JdbcDocumentRepository documents;
    static JdbcDocumentVersionRepository versions;

    private final UUID cabinetA = UUID.randomUUID();
    private final UUID cabinetB = UUID.randomUUID();

    @BeforeAll
    static void migrate() {
        DataSource ds = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        jdbc = JdbcClient.create(ds);
        DataSourceTransactionManager tx = new DataSourceTransactionManager(ds);
        documents = new JdbcDocumentRepository(jdbc, tx);
        versions = new JdbcDocumentVersionRepository(jdbc, tx);
    }

    @BeforeEach
    void clean() {
        jdbc.sql("DELETE FROM documents").update();
    }

    private Document document(UUID cabinet, String name, String key) {
        return new Document(UUID.randomUUID(), cabinet, UUID.randomUUID(), name, DocumentType.PDF,
                100, key, UUID.randomUUID(), "Karim", Instant.parse("2026-11-01T10:00:00Z"));
    }

    private NewVersion newVersion(UUID cabinet, UUID documentId, String key) {
        return new NewVersion(cabinet, documentId, DocumentType.DOCX, 200, key,
                UUID.randomUUID(), "Sara", Instant.parse("2026-11-08T10:00:00Z"));
    }

    @Test
    void l_enregistrement_d_un_document_cree_sa_version_1() {
        Document doc = documents.save(document(cabinetA, "a.pdf", "k1"));

        assertThat(versions.findByDocument(cabinetA, doc.id()))
                .extracting(DocumentVersion::number, DocumentVersion::storageKey)
                .containsExactly(tuple(1, "k1"));
    }

    @Test
    void numerote_les_versions_sequentiellement() {
        Document doc = documents.save(document(cabinetA, "a.pdf", "k1"));

        assertThat(versions.addVersion(newVersion(cabinetA, doc.id(), "k2")).number()).isEqualTo(2);
        assertThat(versions.addVersion(newVersion(cabinetA, doc.id(), "k3")).number()).isEqualTo(3);
        assertThat(versions.findByDocument(cabinetA, doc.id()))
                .extracting(DocumentVersion::number)
                .containsExactly(3, 2, 1);
    }

    @Test
    void les_ajouts_concurrents_recoivent_des_numeros_distincts_sans_ecrasement() throws Exception {
        Document doc = documents.save(document(cabinetA, "a.pdf", "k-base"));
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<DocumentVersion>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            String key = "k-concurrent-" + i;
            futures.add(pool.submit(() -> {
                start.await();
                return versions.addVersion(newVersion(cabinetA, doc.id(), key));
            }));
        }
        start.countDown();

        List<Integer> numbers = new ArrayList<>();
        for (Future<DocumentVersion> future : futures) {
            numbers.add(future.get(30, TimeUnit.SECONDS).number());
        }
        pool.shutdown();

        assertThat(numbers).containsExactlyInAnyOrder(2, 3, 4, 5, 6, 7, 8, 9);
        assertThat(versions.findByDocument(cabinetA, doc.id())).hasSize(9);
    }

    @Test
    void refuse_d_ajouter_une_version_a_un_document_d_un_autre_cabinet() {
        Document doc = documents.save(document(cabinetA, "a.pdf", "k1"));

        assertThatThrownBy(() -> versions.addVersion(newVersion(cabinetB, doc.id(), "k2")))
                .isInstanceOf(DocumentNotFoundException.class);
        assertThat(versions.findByDocument(cabinetA, doc.id())).hasSize(1);
    }

    @Test
    void un_cabinet_ne_voit_pas_les_versions_d_un_autre_cabinet() {
        Document doc = documents.save(document(cabinetA, "a.pdf", "k1"));

        assertThat(versions.documentExists(cabinetB, doc.id())).isFalse();
        assertThat(versions.findByDocument(cabinetB, doc.id())).isEmpty();
    }

    private static final String RAW_INSERT = """
            INSERT INTO document_versions
              (id, document_id, cabinet_id, version_number, media_type, size_bytes,
               storage_key, uploaded_by, author_name, created_at)
            VALUES (?, ?, ?, ?, 'PDF', 10, ?, ?, 'Karim', now())
            """;

    @Test
    void la_base_refuse_un_numero_de_version_en_double() {
        Document doc = documents.save(document(cabinetA, "a.pdf", "k1"));

        assertThatThrownBy(() -> jdbc.sql(RAW_INSERT)
                .params(UUID.randomUUID(), doc.id(), cabinetA, 1, "autre-cle", UUID.randomUUID())
                .update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void la_base_refuse_une_version_dont_le_cabinet_differe_de_celui_du_document() {
        Document doc = documents.save(document(cabinetA, "a.pdf", "k1"));

        assertThatThrownBy(() -> jdbc.sql(RAW_INSERT)
                .params(UUID.randomUUID(), doc.id(), cabinetB, 2, "autre-cle", UUID.randomUUID())
                .update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}