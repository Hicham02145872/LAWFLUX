-- Structure minimale du dossier pour US-09 ; les commandes métier seront ajoutées ensuite.
CREATE TABLE case_records (
    id UUID PRIMARY KEY,
    cabinet_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    archived_at TIMESTAMPTZ,
    CONSTRAINT uq_case_cabinet UNIQUE (id, cabinet_id),
    CONSTRAINT ck_case_title CHECK (length(trim(title)) > 0),
    CONSTRAINT ck_case_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'CLOSED', 'ARCHIVED'))
);
CREATE INDEX idx_case_records_cabinet ON case_records (cabinet_id, id);

CREATE TABLE case_access_grants (
    id UUID PRIMARY KEY,
    case_id UUID NOT NULL,
    cabinet_id UUID NOT NULL,
    user_id UUID NOT NULL,
    kind VARCHAR(16) NOT NULL,
    granted_by UUID NOT NULL,
    granted_at TIMESTAMPTZ NOT NULL,
    revoked_by UUID,
    revoked_at TIMESTAMPTZ,
    CONSTRAINT fk_case_access_case FOREIGN KEY (case_id, cabinet_id)
        REFERENCES case_records (id, cabinet_id),
    CONSTRAINT ck_case_access_kind CHECK (kind IN ('STAFF', 'CLIENT')),
    CONSTRAINT ck_case_access_revocation CHECK ((revoked_by IS NULL) = (revoked_at IS NULL))
);
CREATE UNIQUE INDEX uq_case_access_active
    ON case_access_grants (case_id, user_id, kind) WHERE revoked_at IS NULL;
CREATE INDEX idx_case_access_lookup
    ON case_access_grants (cabinet_id, case_id, user_id, kind) WHERE revoked_at IS NULL;
