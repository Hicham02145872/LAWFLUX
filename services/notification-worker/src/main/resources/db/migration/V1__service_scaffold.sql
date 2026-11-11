-- Données techniques uniquement. Les migrations métier appartiennent à ce service.
CREATE TABLE service_metadata (
  metadata_key VARCHAR(80) PRIMARY KEY,
  metadata_value VARCHAR(255) NOT NULL
);
INSERT INTO service_metadata VALUES ('service', 'notification-worker');
