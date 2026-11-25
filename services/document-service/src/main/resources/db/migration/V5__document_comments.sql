CREATE TABLE document_comments (
    id          UUID         PRIMARY KEY,
    document_id UUID         NOT NULL,
    cabinet_id  UUID         NOT NULL,
    author_id   UUID         NOT NULL,
    author_name VARCHAR(150) NOT NULL,
    visibility  VARCHAR(16)  NOT NULL CHECK (visibility IN ('INTERNAL', 'SHARED')),
    body        TEXT         NOT NULL CHECK (char_length(btrim(body)) BETWEEN 1 AND 5000),
    created_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_comment_document FOREIGN KEY (document_id, cabinet_id)
        REFERENCES documents (id, cabinet_id) ON DELETE CASCADE
);

CREATE INDEX idx_document_comments_document
    ON document_comments (cabinet_id, document_id, created_at);