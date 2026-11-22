CREATE TABLE documents (
    id            UUID         PRIMARY KEY,
    cabinet_id    UUID         NOT NULL,
    case_id       UUID         NOT NULL,
    name          VARCHAR(255) NOT NULL,
    media_type    VARCHAR(100) NOT NULL,
    size_bytes    BIGINT       NOT NULL CHECK (size_bytes > 0),
    storage_key   VARCHAR(255) NOT NULL UNIQUE,
    uploaded_by   UUID         NOT NULL,
    author_name   VARCHAR(150) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_documents_cabinet_case
    ON documents (cabinet_id, case_id, created_at DESC);