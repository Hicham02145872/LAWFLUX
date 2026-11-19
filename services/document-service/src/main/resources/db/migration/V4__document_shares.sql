CREATE TABLE document_shares (
    id          UUID        PRIMARY KEY,
    document_id UUID        NOT NULL,
    cabinet_id  UUID        NOT NULL,
    shared_by   UUID        NOT NULL,
    shared_at   TIMESTAMPTZ NOT NULL,
    revoked_by  UUID,
    revoked_at  TIMESTAMPTZ,
    CONSTRAINT fk_share_document FOREIGN KEY (document_id, cabinet_id)
        REFERENCES documents (id, cabinet_id) ON DELETE CASCADE,
    CONSTRAINT ck_share_revocation CHECK ((revoked_at IS NULL) = (revoked_by IS NULL)),
    CONSTRAINT ck_share_dates CHECK (revoked_at IS NULL OR revoked_at >= shared_at)
);

CREATE UNIQUE INDEX uq_document_active_share
    ON document_shares (document_id) WHERE revoked_at IS NULL;

CREATE INDEX idx_document_shares_document ON document_shares (cabinet_id, document_id);