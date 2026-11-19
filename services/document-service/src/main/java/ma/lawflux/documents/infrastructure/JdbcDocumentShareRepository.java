package ma.lawflux.documents.infrastructure;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import ma.lawflux.documents.domain.Document;
import ma.lawflux.documents.domain.DocumentNotFoundException;
import ma.lawflux.documents.domain.DocumentShare;
import ma.lawflux.documents.domain.DocumentShareRepository;
import ma.lawflux.documents.domain.DocumentType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
@ConditionalOnProperty(prefix = "lawflux.documents", name = "business-enabled", havingValue = "true")
class JdbcDocumentShareRepository implements DocumentShareRepository {

    private final JdbcClient jdbc;
    private final TransactionTemplate tx;

    JdbcDocumentShareRepository(JdbcClient jdbc, PlatformTransactionManager txManager) {
        this.jdbc = jdbc;
        this.tx = new TransactionTemplate(txManager);
    }

    @Override
    public Optional<Document> findDocument(UUID cabinetId, UUID documentId) {
        return jdbc.sql("""
                SELECT id, cabinet_id, case_id, name, media_type, size_bytes,
                       storage_key, uploaded_by, author_name, created_at
                FROM documents
                WHERE id = ? AND cabinet_id = ?
                """)
            .params(documentId, cabinetId)
            .query((rs, i) -> new Document(
                    rs.getObject("id", UUID.class),
                    rs.getObject("cabinet_id", UUID.class),
                    rs.getObject("case_id", UUID.class),
                    rs.getString("name"),
                    DocumentType.valueOf(rs.getString("media_type")),
                    rs.getLong("size_bytes"),
                    rs.getString("storage_key"),
                    rs.getObject("uploaded_by", UUID.class),
                    rs.getString("author_name"),
                    rs.getTimestamp("created_at").toInstant()))
            .optional();
    }

    @Override
    public Optional<DocumentShare> findActiveShare(UUID cabinetId, UUID documentId) {
        return jdbc.sql("""
                SELECT id, document_id, cabinet_id, shared_by, shared_at
                FROM document_shares
                WHERE cabinet_id = ? AND document_id = ? AND revoked_at IS NULL
                """)
            .params(cabinetId, documentId)
            .query((rs, i) -> new DocumentShare(
                    rs.getObject("id", UUID.class),
                    rs.getObject("document_id", UUID.class),
                    rs.getObject("cabinet_id", UUID.class),
                    rs.getObject("shared_by", UUID.class),
                    rs.getTimestamp("shared_at").toInstant()))
            .optional();
    }

    @Override
    public DocumentShare share(UUID cabinetId, UUID documentId, UUID sharedBy, Instant at) {
        DocumentShare result = tx.execute(status -> {
            // Le verrou sur la ligne du document sérialise les partages concurrents.
            boolean found = !jdbc.sql(
                            "SELECT id FROM documents WHERE id = ? AND cabinet_id = ? FOR UPDATE")
                    .params(documentId, cabinetId)
                    .query((rs, i) -> rs.getObject("id", UUID.class))
                    .list()
                    .isEmpty();
            if (!found) {
                throw new DocumentNotFoundException("Document introuvable");
            }

            Optional<DocumentShare> existing = findActiveShare(cabinetId, documentId);
            if (existing.isPresent()) {
                return existing.get();
            }

            DocumentShare created =
                    new DocumentShare(UUID.randomUUID(), documentId, cabinetId, sharedBy, at);
            jdbc.sql("""
                    INSERT INTO document_shares (id, document_id, cabinet_id, shared_by, shared_at)
                    VALUES (?, ?, ?, ?, ?)
                    """)
                .params(created.id(), documentId, cabinetId, sharedBy, Timestamp.from(at))
                .update();
            return created;
        });
        return Objects.requireNonNull(result);
    }

    @Override
    public boolean revoke(UUID cabinetId, UUID documentId, UUID revokedBy, Instant at) {
        int updated = jdbc.sql("""
                UPDATE document_shares
                SET revoked_by = ?, revoked_at = ?
                WHERE document_id = ? AND cabinet_id = ? AND revoked_at IS NULL
                """)
            .params(revokedBy, Timestamp.from(at), documentId, cabinetId)
            .update();
        return updated > 0;
    }
}