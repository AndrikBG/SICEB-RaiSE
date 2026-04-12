-- V006: Clinical event store — IC-02 hybrid JSONB schema.
-- Append-only: INSERT only, no UPDATE or DELETE.
-- Fixed columns for filtering/ordering + JSONB payload for event-specific data.
-- Immutability enforced by application layer and database trigger.

CREATE TABLE clinical_events (
    event_id                UUID            PRIMARY KEY,
    record_id               UUID            NOT NULL REFERENCES medical_records(record_id),
    event_type              VARCHAR(30)     NOT NULL,
    occurred_at             TIMESTAMPTZ     NOT NULL,
    branch_id               UUID            NOT NULL REFERENCES branches(id),
    performed_by_staff_id   UUID            NOT NULL,
    idempotency_key         VARCHAR(64)     NOT NULL,
    payload                 JSONB           NOT NULL DEFAULT '{}',

    CONSTRAINT ck_clinical_events_type CHECK (
        event_type IN ('RECORD_CREATED', 'CONSULTATION', 'PRESCRIPTION', 'LAB_ORDER', 'LAB_RESULT', 'ATTACHMENT')
    )
);

-- IC-02 required indexes
CREATE INDEX ix_clinical_events_record_id_occurred_at
    ON clinical_events(record_id, occurred_at);

CREATE INDEX ix_clinical_events_branch_id_event_type
    ON clinical_events(branch_id, event_type);

CREATE UNIQUE INDEX ix_clinical_events_idempotency_key
    ON clinical_events(idempotency_key);

CREATE INDEX ix_clinical_events_payload_gin
    ON clinical_events USING gin(payload);

-- Trigger to enforce append-only: block UPDATE and DELETE (AUD-03, CRN-02)
CREATE OR REPLACE FUNCTION prevent_clinical_event_mutation()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'Clinical events are immutable — UPDATE and DELETE are forbidden (CRN-02, AUD-03)';
END;
$$;

CREATE TRIGGER trg_clinical_events_no_update
    BEFORE UPDATE ON clinical_events
    FOR EACH ROW EXECUTE FUNCTION prevent_clinical_event_mutation();

CREATE TRIGGER trg_clinical_events_no_delete
    BEFORE DELETE ON clinical_events
    FOR EACH ROW EXECUTE FUNCTION prevent_clinical_event_mutation();

COMMENT ON TABLE clinical_events IS 'Immutable append-only clinical event store — IC-02 hybrid JSONB schema';
COMMENT ON COLUMN clinical_events.event_type IS 'Discriminator: RECORD_CREATED, CONSULTATION, PRESCRIPTION, LAB_ORDER, LAB_RESULT, ATTACHMENT';
COMMENT ON COLUMN clinical_events.payload IS 'Event-specific data as JSONB — schema enforced at application layer';
COMMENT ON COLUMN clinical_events.idempotency_key IS 'Client-generated key for offline replay deduplication (CRN-43)';
