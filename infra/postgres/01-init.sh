#!/bin/sh
set -eu
create_service_db() {
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres -v db_name="$1" -v db_password="$2" <<'SQL'
CREATE USER :"db_name" WITH PASSWORD :'db_password';
CREATE DATABASE :"db_name" OWNER :"db_name";
REVOKE CONNECT ON DATABASE :"db_name" FROM PUBLIC;
GRANT CONNECT ON DATABASE :"db_name" TO :"db_name";
SQL
}
create_service_db lawflux_identity "$IDENTITY_DB_PASSWORD"
create_service_db lawflux_cases "$CASES_DB_PASSWORD"
create_service_db lawflux_documents "$DOCUMENTS_DB_PASSWORD"
create_service_db lawflux_billing "$BILLING_DB_PASSWORD"
create_service_db lawflux_notifications "$NOTIFICATIONS_DB_PASSWORD"
