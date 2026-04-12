-- V009: Security schema — users, roles, permissions, role_permissions, user_branch_assignments, medical_staff
-- Drivers: US-001, US-002, US-003, SEC-01, SEC-02, CRN-15, MNT-03

-- ============================================================
-- Permissions catalog — granular authorization units
-- ============================================================
CREATE TABLE permissions (
    permission_id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key             VARCHAR(100) NOT NULL UNIQUE,
    description     VARCHAR(500) NOT NULL,
    category        VARCHAR(100) NOT NULL,
    requires_residency_check BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_permissions_category ON permissions(category);

-- ============================================================
-- Roles — data-driven, MNT-03 compliant
-- ============================================================
CREATE TABLE roles (
    role_id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(100) NOT NULL UNIQUE,
    description     VARCHAR(500),
    is_system_role  BOOLEAN NOT NULL DEFAULT FALSE,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================
-- Role ↔ Permission mappings
-- ============================================================
CREATE TABLE role_permissions (
    role_id         UUID NOT NULL REFERENCES roles(role_id) ON DELETE CASCADE,
    permission_id   UUID NOT NULL REFERENCES permissions(permission_id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- ============================================================
-- Users — authentication and identity
-- ============================================================
CREATE TABLE users (
    user_id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username            VARCHAR(100) NOT NULL UNIQUE,
    email               VARCHAR(255) NOT NULL UNIQUE,
    full_name           VARCHAR(255) NOT NULL,
    password_hash       VARCHAR(255) NOT NULL,
    role_id             UUID NOT NULL REFERENCES roles(role_id),
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    must_change_password BOOLEAN NOT NULL DEFAULT TRUE,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until        TIMESTAMPTZ,
    branch_id           UUID NOT NULL REFERENCES branches(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_role ON users(role_id);
CREATE INDEX idx_users_branch ON users(branch_id);

-- ============================================================
-- User ↔ Branch assignments (multi-branch support)
-- ============================================================
CREATE TABLE user_branch_assignments (
    user_id     UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    branch_id   UUID NOT NULL REFERENCES branches(id) ON DELETE CASCADE,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, branch_id)
);

-- ============================================================
-- Medical staff — extends user for clinical personnel
-- ============================================================
CREATE TABLE medical_staff (
    staff_id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL UNIQUE REFERENCES users(user_id) ON DELETE CASCADE,
    specialty           VARCHAR(200) NOT NULL,
    residency_level     VARCHAR(10),  -- R1, R2, R3, R4, or NULL for attending
    can_prescribe_controlled BOOLEAN NOT NULL DEFAULT FALSE,
    supervisor_staff_id UUID REFERENCES medical_staff(staff_id),
    professional_license VARCHAR(100),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_residency_level CHECK (
        residency_level IS NULL OR residency_level IN ('R1', 'R2', 'R3', 'R4')
    ),
    CONSTRAINT chk_supervisor_required CHECK (
        residency_level IS NULL OR residency_level NOT IN ('R1', 'R2') OR supervisor_staff_id IS NOT NULL
    )
);

CREATE INDEX idx_medical_staff_user ON medical_staff(user_id);
CREATE INDEX idx_medical_staff_supervisor ON medical_staff(supervisor_staff_id);

-- ============================================================
-- Refresh tokens — server-side storage for token lifecycle
-- ============================================================
CREATE TABLE refresh_tokens (
    token_id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    token_hash      VARCHAR(255) NOT NULL UNIQUE,
    expires_at      TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    revoked_at      TIMESTAMPTZ,
    replaced_by     UUID REFERENCES refresh_tokens(token_id),
    ip_address      VARCHAR(45),
    user_agent      VARCHAR(500)
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens(expires_at) WHERE revoked_at IS NULL;

-- ============================================================
-- Token deny list — immediate JWT revocation
-- ============================================================
CREATE TABLE token_deny_list (
    jti             VARCHAR(255) PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    expires_at      TIMESTAMPTZ NOT NULL,
    reason          VARCHAR(100) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_token_deny_list_expires ON token_deny_list(expires_at);
CREATE INDEX idx_token_deny_list_user ON token_deny_list(user_id);
