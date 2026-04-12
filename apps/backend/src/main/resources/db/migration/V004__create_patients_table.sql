-- V004: Patient aggregate root table.
-- Global uniqueness constraint on patient_id across all branches (CRN-37).
-- Guardian fields mandatory for minors enforced at application layer (US-023).

CREATE TABLE patients (
    patient_id              UUID            PRIMARY KEY,
    first_name              VARCHAR(100)    NOT NULL,
    paternal_surname        VARCHAR(100)    NOT NULL,
    maternal_surname        VARCHAR(100),
    date_of_birth           DATE            NOT NULL,
    gender                  VARCHAR(20)     NOT NULL,
    phone                   VARCHAR(15),
    curp                    VARCHAR(18),
    patient_type            VARCHAR(20)     NOT NULL DEFAULT 'EXTERNAL',
    discount_percentage     DECIMAL(5,2)    NOT NULL DEFAULT 0,
    credential_number       VARCHAR(50),
    guardian_name           VARCHAR(200),
    guardian_relationship   VARCHAR(50),
    guardian_phone          VARCHAR(15),
    guardian_id_confirmed   BOOLEAN         NOT NULL DEFAULT FALSE,
    data_consent_given      BOOLEAN         NOT NULL DEFAULT FALSE,
    profile_photo_path      VARCHAR(500),
    profile_status          VARCHAR(20)     NOT NULL DEFAULT 'COMPLETE',
    special_case            BOOLEAN         NOT NULL DEFAULT FALSE,
    special_case_notes      TEXT,
    branch_id               UUID            NOT NULL REFERENCES branches(id),
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT now(),
    created_by_staff_id     UUID            NOT NULL,
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT ck_patients_type CHECK (patient_type IN ('STUDENT', 'WORKER', 'EXTERNAL')),
    CONSTRAINT ck_patients_gender CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),
    CONSTRAINT ck_patients_status CHECK (profile_status IN ('COMPLETE', 'INCOMPLETE')),
    CONSTRAINT ck_patients_discount CHECK (discount_percentage >= 0 AND discount_percentage <= 100)
);

CREATE INDEX ix_patients_branch_id ON patients(branch_id);
CREATE INDEX ix_patients_name_trgm ON patients USING gin (
    (first_name || ' ' || paternal_surname || ' ' || COALESCE(maternal_surname, '')) gin_trgm_ops
);
CREATE INDEX ix_patients_dob ON patients(date_of_birth);
CREATE INDEX ix_patients_curp ON patients(curp) WHERE curp IS NOT NULL;

COMMENT ON TABLE patients IS 'Patient aggregate root — global uniqueness via UUID PK (CRN-37)';
COMMENT ON COLUMN patients.patient_type IS 'STUDENT (30% discount), WORKER (20%), EXTERNAL (0%) per US-020';
COMMENT ON COLUMN patients.profile_status IS 'INCOMPLETE when CURP or other required data is missing (US-019)';
