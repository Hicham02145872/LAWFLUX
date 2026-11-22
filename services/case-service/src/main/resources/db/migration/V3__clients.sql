CREATE TABLE clients (
    id UUID NOT NULL,
    cabinet_id UUID NOT NULL,
    name VARCHAR(200) NOT NULL,
    email VARCHAR(320),
    phone VARCHAR(40),
    address VARCHAR(500),
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    archived_at TIMESTAMPTZ,

    CONSTRAINT pk_clients PRIMARY KEY (id),
    CONSTRAINT uq_clients_id_cabinet UNIQUE (id, cabinet_id),
    CONSTRAINT ck_clients_name CHECK (length(trim(name)) > 0),
    CONSTRAINT ck_clients_status CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    CONSTRAINT ck_clients_archive_state
        CHECK ((status = 'ARCHIVED') = (archived_at IS NOT NULL))
);

CREATE INDEX idx_clients_cabinet_status_created
    ON clients (cabinet_id, status, created_at DESC, id DESC);