package ma.lawflux.documents.infrastructure;

import java.sql.Timestamp;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import ma.lawflux.documents.domain.CommentVisibility;
import ma.lawflux.documents.domain.DocumentComment;
import ma.lawflux.documents.domain.DocumentCommentRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "lawflux.documents", name = "business-enabled", havingValue = "true")
class JdbcDocumentCommentRepository implements DocumentCommentRepository {

    private final JdbcClient jdbc;

    JdbcDocumentCommentRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public DocumentComment save(DocumentComment c) {
        jdbc.sql("""
                INSERT INTO document_comments
                  (id, document_id, cabinet_id, author_id, author_name, visibility, body, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """)
            .params(c.id(), c.documentId(), c.cabinetId(), c.authorId(), c.authorName(),
                    c.visibility().name(), c.body(), Timestamp.from(c.createdAt()))
            .update();
        return c;
    }

    @Override
    public List<DocumentComment> findByDocument(UUID cabinetId, UUID documentId,
                                                Set<CommentVisibility> visibilities) {
        if (visibilities.isEmpty()) {
            return List.of();
        }
        List<String> names = visibilities.stream().map(Enum::name).sorted().toList();
        return jdbc.sql("""
                SELECT id, document_id, cabinet_id, author_id, author_name, visibility, body, created_at
                FROM document_comments
                WHERE cabinet_id = :cabinet AND document_id = :document AND visibility IN (:visibilities)
                ORDER BY created_at, id
                """)
            .param("cabinet", cabinetId)
            .param("document", documentId)
            .param("visibilities", names)
            .query((rs, i) -> new DocumentComment(
                    rs.getObject("id", UUID.class),
                    rs.getObject("document_id", UUID.class),
                    rs.getObject("cabinet_id", UUID.class),
                    rs.getObject("author_id", UUID.class),
                    rs.getString("author_name"),
                    CommentVisibility.valueOf(rs.getString("visibility")),
                    rs.getString("body"),
                    rs.getTimestamp("created_at").toInstant()))
            .list();
    }
}