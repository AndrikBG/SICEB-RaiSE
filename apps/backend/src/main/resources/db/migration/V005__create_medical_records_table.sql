-- V005: Medical record — exactly one per patient, append-only container.
-- The record itself is immutable once created; all clinical data lives in clinical_events.
-- NOM-004-SSA3-2012 permanent retention: no DELETE allowed (CRN-01, CRN-02).

CREATE TABLE medical_records (
    record_id           UUID            PRIMARY KEY,
    patient_id          UUID            NOT NULL UNIQUE REFERENCES patients(patient_id),
    branch_id           UUID            NOT NULL REFERENCES branches(id),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    created_by_staff_id UUID            NOT NULL
);

CREATE INDEX ix_medical_records_branch_id ON medical_records(branch_id);

COMMENT ON TABLE medical_records IS 'One medical record per patient — append-only container for clinical events (CRN-02, NOM-004)';
COMMENT ON COLUMN medical_records.patient_id IS 'Unique constraint enforces exactly one record per patient';
