package ma.lawflux.documents.infrastructure;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import ma.lawflux.documents.domain.Document;
import ma.lawflux.documents.domain.DocumentRepository;
import ma.lawflux.documents.domain.DocumentType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcDocumentRepository implements DocumentRepository {

    private final JdbcClient jdbc;

    JdbcDocumentRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Document save(Document d) {
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