package ma.lawflux.documents.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import ma.lawflux.documents.domain.Document;
import ma.lawflux.documents.domain.DocumentNotFoundException;
import ma.lawflux.documents.domain.DocumentShare;
import ma.lawflux.documents.domain.DocumentType;
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
class JdbcDocumentShareRepositoryTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final Instant T0 = Instant.parse("2026-11-10T10:00:00Z");
    private static final Instant T1 = Instant.parse("2026-11-10T11:00:00Z");
    private static final Instant T2 = Instant.parse("2026-11-10T12:00:00Z");

    static JdbcClient jdbc;
    static JdbcDocumentRepository documents;
    static JdbcDocumentShareRepository shares;

    private final UUID cabinetA = UUID.randomUUID();
    private final UUID cabinetB = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeAll
    static void migrate() {
        DataSource ds = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        jdbc = JdbcClient.create(ds);
        DataSourceTransactionManager tx = new DataSourceTransactionManager(ds);
        documents = new JdbcDocumentRepository(jdbc, tx);
        shares = new JdbcDocumentShareRepository(jdbc, tx);
    }

    @BeforeEach
    void clean() {
        jdbc.sql("DELETE FROM documents").update();
    }

    private Document savedDocument(UUID cabinet, String storageKey) {
        return documents.save(new Document(UUID.randomUUID(), cabinet, UUID.randomUUID(), "a.pdf",
                DocumentType.PDF, 100, storageKey, userId, "Karim", T0));
    }

    private long countShares(UUID documentId) {
        return jdbc.sql("SELECT COUNT(*) FROM document_shares WHERE document_id = ?")
                .params(documentId)
                .query(Long.class)
                .single();
    }

    @Test
    void partage_puis_retrouve_le_partage_actif() {
        Document doc = savedDocument(cabinetA, "k1");

        DocumentShare share = shares.share(cabinetA, doc.id(), userId, T0);

        assertThat(shares.findActiveShare(cabinetA, doc.id())).contains(share);
    }

    @Test
    void partager_deux_fois_renvoie_le_meme_partage_sans_doublon() {
        Document doc = savedDocument(cabinetA, "k1");

        DocumentShare first = shares.share(cabinetA, doc.id(), userId, T0);
        DocumentShare second = shares.share(cabinetA, doc.id(), UUID.randomUUID(), T1);

        assertThat(second).isEqualTo(first);
        assertThat(countShares(doc.id())).isEqualTo(1L);
    }

    @Test
    void revoquer_ferme_le_partage_et_n_est_effectif_qu_une_fois() {
        Document doc = savedDocument(cabinetA, "k1");
        shares.share(cabinetA, doc.id(), userId, T0);

        assertThat(shares.revoke(cabinetA, doc.id(), userId, T1)).isTrue();
        assertThat(shares.findActiveShare(cabinetA, doc.id())).isEmpty();
        assertThat(shares.revoke(cabinetA, doc.id(), userId, T1)).isFalse();
    }

    @Test
    void repartager_apres_une_revocation_cree_un_nouveau_partage_et_conserve_l_historique() {
        Document doc = savedDocument(cabinetA, "k1");
        DocumentShare first = shares.share(cabinetA, doc.id(), userId, T0);
        shares.revoke(cabinetA, doc.id(), userId, T1);

        DocumentShare second = shares.share(cabinetA, doc.id(), userId, T2);

        assertThat(second.id()).isNotEqualTo(first.id());
        assertThat(shares.findActiveShare(cabinetA, doc.id())).contains(second);
        assertThat(countShares(doc.id())).isEqualTo(2L);
    }

    @Test
    void les_partages_concurrents_n_en_creent_qu_un() throws Exception {
        Document doc = savedDocument(cabinetA, "k1");
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<DocumentShare>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                start.await();
                return shares.share(cabinetA, doc.id(), userId, T0);
            }));
        }
        start.countDown();

        Set<UUID> ids = new HashSet<>();
        for (Future<DocumentShare> future : futures) {
            ids.add(future.get(30, TimeUnit.SECONDS).id());
        }
        pool.shutdown();

        assertThat(ids).hasSize(1);
        assertThat(countShares(doc.id())).isEqualTo(1L);
    }

    @Test
    void refuse_de_partager_un_document_d_un_autre_cabinet() {
        Document doc = savedDocument(cabinetA, "k1");

        assertThatThrownBy(() -> shares.share(cabinetB, doc.id(), userId, T0))
                .isInstanceOf(DocumentNotFoundException.class);

        assertThat(shares.findDocument(cabinetB, doc.id())).isEmpty();
        assertThat(shares.findDocument(cabinetA, doc.id())).isPresent();
        assertThat(countShares(doc.id())).isZero();
    }

    @Test
    void la_base_refuse_deux_partages_actifs_pour_le_meme_document() {
        Document doc = savedDocument(cabinetA, "k1");
        shares.share(cabinetA, doc.id(), userId, T0);

        assertThatThrownBy(() -> jdbc.sql("""
                INSERT INTO document_shares (id, document_id, cabinet_id, shared_by, shared_at)
                VALUES (?, ?, ?, ?, ?)
                """)
                .params(UUID.randomUUID(), doc.id(), cabinetA, userId, Timestamp.from(T1))
                .update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void la_base_refuse_une_revocation_sans_auteur() {
        Document doc = savedDocument(cabinetA, "k1");

        assertThatThrownBy(() -> jdbc.sql("""
                INSERT INTO document_shares (id, document_id, cabinet_id, shared_by, shared_at, revoked_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """)
                .params(UUID.randomUUID(), doc.id(), cabinetA, userId, Timestamp.from(T0), Timestamp.from(T1))
                .update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void la_base_refuse_une_revocation_anterieure_au_partage() {
        Document doc = savedDocument(cabinetA, "k1");

        assertThatThrownBy(() -> jdbc.sql("""
                INSERT INTO document_shares
                  (id, document_id, cabinet_id, shared_by, shared_at, revoked_by, revoked_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """)
                .params(UUID.randomUUID(), doc.id(), cabinetA, userId,
                        Timestamp.from(T1), userId, Timestamp.from(T0))
                .update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}