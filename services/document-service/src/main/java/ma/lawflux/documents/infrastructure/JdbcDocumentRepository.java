package ma.lawflux.documents.infrastructure;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import ma.lawflux.documents.domain.Document;
import ma.lawflux.documents.domain.DocumentRepository;
import ma.lawflux.documents.domain.DocumentType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
@ConditionalOnProperty(prefix = "lawflux.documents", name = "business-enabled", havingValue = "true")
class JdbcDocumentRepository implements DocumentRepository {

    private final JdbcClient jdbc;
    private final TransactionTemplate tx;

    JdbcDocumentRepository(JdbcClient jdbc, PlatformTransactionManager txManager) {
        this.jdbc = jdbc;
        this.tx = new TransactionTemplate(txManager);
    }

    @Override
    public Document save(Document d) {
        // Le document et sa version 1 sont enregistrés ensemble ou pas du tout.
        tx.executeWithoutResult(status -> {
            jdbc.sql("""
                    INSERT INTO documents
                      (id, cabinet_id, case_id, name, media_type, size_bytes,
                       storage_key, uploaded_by, author_name, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """)
                .params(d.id(), d.cabinetId(), d.caseId(), d.name(), d.type().name(),
                        d.sizeBytes(), d.storageKey(), d.uploadedBy(), d.authorName(),
                        Timestamp.from(d.createdAt()))
                .update();
            jdbc.sql("""
                    INSERT INTO document_versions
                      (id, document_id, cabinet_id, version_number, media_type, size_bytes,
                       storage_key, uploaded_by, author_name, created_at)
                    VALUES (?, ?, ?, 1, ?, ?, ?, ?, ?, ?)
                    """)
                .params(UUID.randomUUID(), d.id(), d.cabinetId(), d.type().name(),
                        d.sizeBytes(), d.storageKey(), d.uploadedBy(), d.authorName(),
                        Timestamp.from(d.createdAt()))
                .update();
        });
        return d;
    }

    @Override
    public List<Document> findByCase(UUID cabinetId, UUID caseId) {
        return jdbc.sql("""
                SELECT id, cabinet_id, case_id, name, media_type, size_bytes,
                       storage_key, uploaded_by, author_name, created_at
                FROM documents
                WHERE cabinet_id = ? AND case_id = ?
                ORDER BY created_at DESC
                """)
            .params(cabinetId, caseId)
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
            .list();
    }
}