package ma.lawflux.documents.infrastructure;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import ma.lawflux.documents.domain.DocumentNotFoundException;
import ma.lawflux.documents.domain.DocumentType;
import ma.lawflux.documents.domain.DocumentVersion;
import ma.lawflux.documents.domain.DocumentVersionRepository;
import ma.lawflux.documents.domain.NewVersion;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
@ConditionalOnProperty(prefix = "lawflux.documents", name = "business-enabled", havingValue = "true")
class JdbcDocumentVersionRepository implements DocumentVersionRepository {

    private final JdbcClient jdbc;
    private final TransactionTemplate tx;

    JdbcDocumentVersionRepository(JdbcClient jdbc, PlatformTransactionManager txManager) {
        this.jdbc = jdbc;
        this.tx = new TransactionTemplate(txManager);
    }

    @Override
    public boolean documentExists(UUID cabinetId, UUID documentId) {
        Long count = jdbc.sql("SELECT COUNT(*) FROM documents WHERE id = ? AND cabinet_id = ?")
                .params(documentId, cabinetId)
                .query(Long.class)
                .single();
        return count > 0;
    }

    @Override
    public DocumentVersion addVersion(NewVersion nv) {
        DocumentVersion created = tx.execute(status -> {
            // Le verrou sur la ligne du document sérialise les ajouts concurrents.
            boolean found = !jdbc.sql(
                            "SELECT id FROM documents WHERE id = ? AND cabinet_id = ? FOR UPDATE")
                    .params(nv.documentId(), nv.cabinetId())
                    .query((rs, i) -> rs.getObject("id", UUID.class))
                    .list()
                    .isEmpty();
            if (!found) {
                throw new DocumentNotFoundException("Document introuvable");
            }

            int next = jdbc.sql("""
                            SELECT COALESCE(MAX(version_number), 0) + 1
                            FROM document_versions WHERE document_id = ?
                            """)
                    .params(nv.documentId())
                    .query(Integer.class)
                    .single();

            DocumentVersion version = new DocumentVersion(
                    UUID.randomUUID(), nv.documentId(), nv.cabinetId(), next, nv.type(),
                    nv.sizeBytes(), nv.storageKey(), nv.uploadedBy(), nv.authorName(), nv.createdAt());

            jdbc.sql("""
                    INSERT INTO document_versions
                      (id, document_id, cabinet_id, version_number, media_type, size_bytes,
                       storage_key, uploaded_by, author_name, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """)
                .params(version.id(), version.documentId(), version.cabinetId(), version.number(),
                        version.type().name(), version.sizeBytes(), version.storageKey(),
                        version.uploadedBy(), version.authorName(),
                        Timestamp.from(version.createdAt()))
                .update();
            return version;
        });
        return Objects.requireNonNull(created);
    }

    @Override
    public List<DocumentVersion> findByDocument(UUID cabinetId, UUID documentId) {
        return jdbc.sql("""
                SELECT id, document_id, cabinet_id, version_number, media_type, size_bytes,
                       storage_key, uploaded_by, author_name, created_at
                FROM document_versions
                WHERE cabinet_id = ? AND document_id = ?
                ORDER BY version_number DESC
                """)
            .params(cabinetId, documentId)
            .query((rs, i) -> new DocumentVersion(
                    rs.getObject("id", UUID.class),
                    rs.getObject("document_id", UUID.class),
                    rs.getObject("cabinet_id", UUID.class),
                    rs.getInt("version_number"),
                    DocumentType.valueOf(rs.getString("media_type")),
                    rs.getLong("size_bytes"),
                    rs.getString("storage_key"),
                    rs.getObject("uploaded_by", UUID.class),
                    rs.getString("author_name"),
                    rs.getTimestamp("created_at").toInstant()))
            .list();
    }
}