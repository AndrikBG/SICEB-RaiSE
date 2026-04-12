-- V014: LFPDPPP compliance schema — consent lifecycle + ARCO workflows
-- Drivers: CRN-32, US-066
-- Tasks: T3.6.1, T3.6.2

-- ============================================================
-- Consent records — patient data consent lifecycle
-- ============================================================
CREATE TABLE consent_records (
    consent_id    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id    UUID NOT NULL REFERENCES patients(patient_id),
    branch_id     UUID NOT NULL REFERENCES branches(id),
    consent_type  VARCHAR(100) NOT NULL,
    purpose       VARCHAR(500) NOT NULL,
    granted_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    revoked_at    TIMESTAMPTZ,
    granted_by    UUID REFERENCES users(user_id),
    revoked_by    UUID REFERENCES users(user_id),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_consent_patient ON consent_records(patient_id);
CREATE INDEX idx_consent_branch ON consent_records(branch_id);
CREATE INDEX idx_consent_active ON consent_records(patient_id, consent_type)
    WHERE revoked_at IS NULL;

-- ============================================================
-- ARCO requests — formal data rights requests under LFPDPPP
-- A = Access, R = Rectification, C = Cancellation, O = Opposition
-- Legal deadline: 20 business days from request date
-- ============================================================
CREATE TABLE arco_requests (
    request_id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id       UUID NOT NULL REFERENCES patients(patient_id),
    branch_id        UUID NOT NULL REFERENCES branches(id),
    request_type     VARCHAR(20) NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    description      TEXT NOT NULL,
    requested_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deadline         DATE NOT NULL,
    resolved_at      TIMESTAMPTZ,
    resolution_notes TEXT,
    handled_by       UUID REFERENCES users(user_id),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_arco_request_type CHECK (
        request_type IN ('ACCESS', 'RECTIFICATION', 'CANCELLATION', 'OPPOSITION')
    ),
    CONSTRAINT chk_arco_status CHECK (
        status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'REJECTED')
    )
);

CREATE INDEX idx_arco_patient ON arco_requests(patient_id);
CREATE INDEX idx_arco_branch ON arco_requests(branch_id);
CREATE INDEX idx_arco_status ON arco_requests(status);
CREATE INDEX idx_arco_pending_deadline ON arco_requests(deadline)
    WHERE status IN ('PENDING', 'IN_PROGRESS');
