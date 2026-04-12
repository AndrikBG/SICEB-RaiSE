-- PostgreSQL initialization script
-- Runs once when the container is first created (docker-entrypoint-initdb.d)

CREATE EXTENSION IF NOT EXISTS "pg_trgm";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
