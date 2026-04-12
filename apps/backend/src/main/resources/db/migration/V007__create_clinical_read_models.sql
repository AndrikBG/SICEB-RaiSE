-- V007: Read model tables for CQRS projections.
-- These are denormalized, indexed tables updated from clinical_events.
-- Optimized for query patterns: patient search (PER-03), timeline (US-027), NOM-004 (CRN-31).

-- Patient search read model (PER-03: sub-1s over 50,000+ records)
CREATE TABLE patient_search_view (
    patient_id          UUID            PRIMARY KEY REFERENCES patients(patient_id),
    full_name           VARCHAR(300)    NOT NULL,
    date_of_birth       DATE            NOT NULL,
    patient_type        VARCHAR(20)     NOT NULL,
    gender              VARCHAR(20)     NOT NULL,
    phone               VARCHAR(15),
    profile_status      VARCHAR(20)     NOT NULL DEFAULT 'COMPLETE',
    branch_id           UUID            NOT NULL REFERENCES branches(id),
    record_id           UUID            REFERENCES medical_records(record_id),
    last_visit_date     TIMESTAMPTZ,
    consultation_count  INTEGER         NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX ix_patient_search_branch_id ON patient_search_view(branch_id);
CREATE INDEX ix_patient_search_name_trgm ON patient_search_view USING gin(full_name gin_trgm_ops);
CREATE INDEX ix_patient_search_dob ON patient_search_view(date_of_birth);
CREATE INDEX ix_patient_search_type ON patient_search_view(branch_id, patient_type);
CREATE INDEX ix_patient_search_last_visit ON patient_search_view(branch_id, last_visit_date DESC NULLS LAST);

-- Pending lab studies read model (US-040)
CREATE TABLE pending_lab_studies_view (
    study_id            UUID            PRIMARY KEY,
    event_id            UUID            NOT NULL REFERENCES clinical_events(event_id),
    record_id           UUID            NOT NULL REFERENCES medical_records(record_id),
    patient_id          UUID            NOT NULL REFERENCES patients(patient_id),
    patient_name        VARCHAR(300)    NOT NULL,
    consultation_id     UUID,
    study_type          VARCHAR(100)    NOT NULL,
    priority            VARCHAR(20)     NOT NULL DEFAULT 'ROUTINE',
    status              VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
    instructions        TEXT,
    requested_at        TIMESTAMPTZ     NOT NULL,
    requested_by_staff  UUID            NOT NULL,
    result_text         TEXT,
    result_recorded_at  TIMESTAMPTZ,
    result_recorded_by  UUID,
    branch_id           UUID            NOT NULL REFERENCES branches(id)
);

CREATE INDEX ix_pending_lab_branch_status ON pending_lab_studies_view(branch_id, status);
CREATE INDEX ix_pending_lab_requested_at ON pending_lab_studies_view(branch_id, requested_at);
CREATE INDEX ix_pending_lab_patient ON pending_lab_studies_view(patient_id);

COMMENT ON TABLE patient_search_view IS 'Denormalized patient search projection — PER-03 sub-1s target';
COMMENT ON TABLE pending_lab_studies_view IS 'Lab technician work queue — branch-scoped pending studies (US-040)';
