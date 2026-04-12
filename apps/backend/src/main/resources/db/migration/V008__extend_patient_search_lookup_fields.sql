-- V008: extend patient_search_view for broader lookup criteria (US-028).
ALTER TABLE patient_search_view
    ADD COLUMN IF NOT EXISTS curp VARCHAR(18),
    ADD COLUMN IF NOT EXISTS credential_number VARCHAR(50);

CREATE INDEX IF NOT EXISTS ix_patient_search_curp
    ON patient_search_view(curp)
    WHERE curp IS NOT NULL;

CREATE INDEX IF NOT EXISTS ix_patient_search_credential
    ON patient_search_view(credential_number)
    WHERE credential_number IS NOT NULL;
