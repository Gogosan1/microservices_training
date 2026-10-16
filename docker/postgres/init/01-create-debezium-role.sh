#!/usr/bin/env bash
set -euo pipefail

: "${DEBEZIUM_PASSWORD:?DEBEZIUM_PASSWORD must be passed to the postgres container}"

psql \
  --username "$POSTGRES_USER" \
  --dbname "$POSTGRES_DB" \
  --set=debezium_password="$DEBEZIUM_PASSWORD" \
  --set=ON_ERROR_STOP=1 <<'SQL'
SELECT format(
    'CREATE ROLE debezium WITH LOGIN REPLICATION PASSWORD %L',
    :'debezium_password'
)
WHERE NOT EXISTS (
    SELECT 1
    FROM pg_roles
    WHERE rolname = 'debezium'
)
\gexec
SQL
