package ma.lawflux.documents.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import ma.lawflux.documents.domain.CommentVisibility;
import ma.lawflux.documents.domain.Document;
import ma.lawflux.documents.domain.DocumentComment;
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
class JdbcDocumentCommentRepositoryTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final Instant T0 = Instant.parse("2026-11-10T10:00:00Z");

    static JdbcClient jdbc;
    static JdbcDocumentRepository documents;
    static JdbcDocumentCommentRepository comments;

    private final UUID cabinetA = UUID.randomUUID();
    private final UUID cabinetB = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeAll
    static void migrate() {
        DataSource ds = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        jdbc = JdbcClient.create(ds);
        documents = new JdbcDocumentRepository(jdbc, new DataSourceTransactionManager(ds));
        comments = new JdbcDocumentCommentRepository(jdbc);
    }

    @BeforeEach
    void clean() {
        jdbc.sql("DELETE FROM documents").update();
    }

    private Document savedDocument(UUID cabinet, String key) {
        return documents.save(new Document(UUID.randomUUID(), cabinet, UUID.randomUUID(), "a.pdf",
                DocumentType.PDF, 100, key, userId, "Karim", T0));
    }

    private DocumentComment comment(Document doc, CommentVisibility visibility, String body, Instant at) {
        return new DocumentComment(UUID.randomUUID(), doc.id(), doc.cabinetId(), userId, "Karim",
                visibility, body, at);
    }

    @Test
    void enregistre_puis_relit_toutes_les_valeurs() {
        Document doc = savedDocument(cabinetA, "k1");
        DocumentComment saved = comments.save(comment(doc, CommentVisibility.SHARED, "bonjour", T0));

        assertThat(comments.findByDocument(cabinetA, doc.id(), EnumSet.allOf(CommentVisibility.class)))
                .containsExactly(saved);
    }

    @Test
    void ne_renvoie_que_les_visibilites_demandees() {
        Document doc = savedDocument(cabinetA, "k1");
        comments.save(comment(doc, CommentVisibility.INTERNAL, "interne", T0));
        comments.save(comment(doc, CommentVisibility.SHARED, "partagé", T0.plusSeconds(1)));

        assertThat(comments.findByDocument(cabinetA, doc.id(), Set.of(CommentVisibility.SHARED)))
                .extracting(DocumentComment::body).containsExactly("partagé");
        assertThat(comments.findByDocument(cabinetA, doc.id(), Set.of(CommentVisibility.INTERNAL)))
                .extracting(DocumentComment::body).containsExactly("interne");
        assertThat(comments.findByDocument(cabinetA, doc.id(), Set.of())).isEmpty();
    }

    @Test
    void trie_du_plus_ancien_au_plus_recent() {
        Document doc = savedDocument(cabinetA, "k1");
        comments.save(comment(doc, CommentVisibility.SHARED, "second", T0.plusSeconds(10)));
        comments.save(comment(doc, CommentVisibility.SHARED, "premier", T0));

        assertThat(comments.findByDocument(cabinetA, doc.id(), EnumSet.allOf(CommentVisibility.class)))
                .extracting(DocumentComment::body).containsExactly("premier", "second");
    }

    @Test
    void un_cabinet_ne_voit_pas_les_commentaires_d_un_autre_cabinet() {
        Document doc = savedDocument(cabinetA, "k1");
        comments.save(comment(doc, CommentVisibility.SHARED, "confidentiel", T0));

        assertThat(comments.findByDocument(cabinetB, doc.id(), EnumSet.allOf(CommentVisibility.class)))
                .isEmpty();
    }

    @Test
    void conserve_le_texte_avec_apostrophes_accents_et_sauts_de_ligne() {
        Document doc = savedDocument(cabinetA, "k1");
        String body = "L'article 5.3 : « responsabilité à 100 % »\nÀ revoir d'urgence — clause abusive ?";
        comments.save(comment(doc, CommentVisibility.SHARED, body, T0));

        assertThat(comments.findByDocument(cabinetA, doc.id(), Set.of(CommentVisibility.SHARED)).get(0).body())
                .isEqualTo(body);
    }

    @Test
    void la_base_refuse_une_visibilite_inconnue_un_texte_vide_et_un_texte_trop_long() {
        Document doc = savedDocument(cabinetA, "k1");
        String sql = """
                INSERT INTO document_comments
                  (id, document_id, cabinet_id, author_id, author_name, visibility, body, created_at)
                VALUES (?, ?, ?, ?, 'Karim', ?, ?, ?)
                """;

        assertThatThrownBy(() -> jdbc.sql(sql)
                .params(UUID.randomUUID(), doc.id(), cabinetA, userId, "PUBLIC", "x", Timestamp.from(T0))
                .update()).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.sql(sql)
                .params(UUID.randomUUID(), doc.id(), cabinetA, userId, "SHARED", "   ", Timestamp.from(T0))
                .update()).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.sql(sql)
                .params(UUID.randomUUID(), doc.id(), cabinetA, userId, "SHARED", "a".repeat(5001),
                        Timestamp.from(T0))
                .update()).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void la_base_refuse_un_commentaire_dont_le_cabinet_differe_de_celui_du_document() {
        Document doc = savedDocument(cabinetA, "k1");

        assertThatThrownBy(() -> jdbc.sql("""
                INSERT INTO document_comments
                  (id, document_id, cabinet_id, author_id, author_name, visibility, body, created_at)
                VALUES (?, ?, ?, ?, 'Karim', 'SHARED', 'x', ?)
                """)
                .params(UUID.randomUUID(), doc.id(), cabinetB, userId, Timestamp.from(T0))
                .update()).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void supprimer_le_document_supprime_ses_commentaires() {
        Document doc = savedDocument(cabinetA, "k1");
        comments.save(comment(doc, CommentVisibility.SHARED, "x", T0));

        jdbc.sql("DELETE FROM documents WHERE id = ?").params(doc.id()).update();

        assertThat(jdbc.sql("SELECT COUNT(*) FROM document_comments").query(Long.class).single()).isZero();
    }
}