-- V015: Extend branch schema for S4.1 (Branch Management and Context Switching)
-- Drivers: US-071, US-074, D-045

-- Extend branches table with operational fields
ALTER TABLE branches ADD COLUMN phone VARCHAR(20);
ALTER TABLE branches ADD COLUMN email VARCHAR(200);
ALTER TABLE branches ADD COLUMN opening_time TIME;
ALTER TABLE branches ADD COLUMN closing_time TIME;
ALTER TABLE branches ADD COLUMN branch_code VARCHAR(20);
ALTER TABLE branches ADD COLUMN onboarding_complete BOOLEAN NOT NULL DEFAULT false;

-- Add last_active_branch_id to users (D-045: remember last branch)
ALTER TABLE users ADD COLUMN last_active_branch_id UUID REFERENCES branches(id);

-- Onboarding progress tracking
CREATE TABLE branch_onboarding_status (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id    UUID NOT NULL REFERENCES branches(id),
    step_name    VARCHAR(50) NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    completed_at TIMESTAMPTZ,
    error_message TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_onboarding_step UNIQUE (branch_id, step_name),
    CONSTRAINT chk_onboarding_status CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED'))
);
CREATE INDEX ix_onboarding_branch ON branch_onboarding_status(branch_id);

-- Service catalog per branch
CREATE TABLE branch_service_catalog (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id    UUID NOT NULL REFERENCES branches(id),
    service_name VARCHAR(200) NOT NULL,
    service_code VARCHAR(50),
    is_active    BOOLEAN NOT NULL DEFAULT true,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_service_branch UNIQUE (branch_id, service_name)
);
CREATE INDEX ix_service_catalog_branch ON branch_service_catalog(branch_id);
