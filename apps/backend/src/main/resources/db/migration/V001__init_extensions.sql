-- V001: Enable required PostgreSQL extensions
-- pg_trgm: trigram matching for fuzzy text search (patient lookup, medication search)
-- pgcrypto: cryptographic functions for UUID generation and data hashing

CREATE EXTENSION IF NOT EXISTS "pg_trgm";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
