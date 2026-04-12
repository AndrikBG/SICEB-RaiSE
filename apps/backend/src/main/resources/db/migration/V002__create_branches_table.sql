-- V002: Create branches table — the anchor for multi-tenant isolation.
-- All tenant-scoped tables will have a branch_id FK pointing here.
-- This is a Platform table (Branch Management module), not a domain table.

CREATE TABLE branches (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    address     TEXT,
    is_active   BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE branches IS 'Branch/sucursal registry — tenant anchor for multi-branch isolation (CRN-29)';
COMMENT ON COLUMN branches.id IS 'UUID primary key — referenced as branch_id in all tenant-scoped tables';

-- Seed a default branch for local development
INSERT INTO branches (id, name, address)
VALUES ('00000000-0000-4000-a000-000000000001', 'Sucursal Principal (Dev)', 'Dirección de desarrollo local');
