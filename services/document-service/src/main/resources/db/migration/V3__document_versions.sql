ALTER TABLE documents ADD CONSTRAINT uq_documents_id_cabinet UNIQUE (id, cabinet_id);

CREATE TABLE document_versions (
    id             UUID         PRIMARY KEY,
    document_id    UUID         NOT NULL,
    cabinet_id     UUID         NOT NULL,
    version_number INTEGER      NOT NULL CHECK (version_number >= 1),
    media_type     VARCHAR(100) NOT NULL,
    size_bytes     BIGINT       NOT NULL CHECK (size_bytes > 0),
    storage_key    VARCHAR(255) NOT NULL UNIQUE,
    uploaded_by    UUID         NOT NULL,
    author_name    VARCHAR(150) NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_document_version UNIQUE (document_id, version_number),
    CONSTRAINT fk_version_document FOREIGN KEY (document_id, cabinet_id)
        REFERENCES documents (id, cabinet_id) ON DELETE CASCADE
);

INSERT INTO document_versions
    (id, document_id, cabinet_id, version_number, media_type, size_bytes,
     storage_key, uploaded_by, author_name, created_at)
SELECT gen_random_uuid(), id, cabinet_id, 1, media_type, size_bytes,
       storage_key, uploaded_by, author_name, created_at
FROM documents;