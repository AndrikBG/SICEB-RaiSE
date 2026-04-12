-- V011: Audit & Compliance schema — hash-chained immutable audit log
-- Drivers: CRN-17, CRN-18, US-066, CRN-13
-- Constraint: IC-03 (hash chain computed & serialized in PostgreSQL)

-- ============================================================
-- Audit log (append-only, tamper-evident via hash chain)
-- ============================================================
CREATE TABLE audit_log (
    entry_id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id       UUID REFERENCES branches(id),
    user_id         UUID REFERENCES users(user_id),
    action          VARCHAR(200) NOT NULL,
    target_entity   VARCHAR(200),
    target_id       UUID,
    ip_address      VARCHAR(45),
    user_agent      VARCHAR(500),
    payload         JSONB NOT NULL,
    previous_hash   TEXT,
    entry_hash      TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_log_created_at ON audit_log(created_at);
CREATE INDEX idx_audit_log_user ON audit_log(user_id, created_at);
CREATE INDEX idx_audit_log_branch ON audit_log(branch_id, created_at);
CREATE INDEX idx_audit_log_target ON audit_log(target_entity, target_id, created_at);

-- ============================================================
-- Hash chain trigger function (IC-03)
-- NOTE: The application must NOT compute previous_hash.
-- ============================================================
CREATE OR REPLACE FUNCTION audit_hash_chain()
RETURNS TRIGGER AS $$
DECLARE
    v_previous_hash TEXT;
BEGIN
    SELECT entry_hash INTO v_previous_hash
    FROM audit_log
    ORDER BY created_at DESC, entry_id DESC
    LIMIT 1
    FOR UPDATE;

    NEW.previous_hash := COALESCE(v_previous_hash, 'GENESIS');
    NEW.entry_hash := encode(
        digest(convert_to(NEW.previous_hash || '|' || NEW.payload::text, 'UTF8'), 'sha256'),
        'hex'
    );

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_audit_hash_chain ON audit_log;
CREATE TRIGGER trg_audit_hash_chain
    BEFORE INSERT ON audit_log
    FOR EACH ROW
    EXECUTE FUNCTION audit_hash_chain();

-- ============================================================
-- Immutability guardrails (defense-in-depth)
-- ============================================================
CREATE OR REPLACE FUNCTION audit_log_no_update_delete()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'audit_log is immutable';
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_audit_log_no_update ON audit_log;
CREATE TRIGGER trg_audit_log_no_update
    BEFORE UPDATE ON audit_log
    FOR EACH ROW
    EXECUTE FUNCTION audit_log_no_update_delete();

DROP TRIGGER IF EXISTS trg_audit_log_no_delete ON audit_log;
CREATE TRIGGER trg_audit_log_no_delete
    BEFORE DELETE ON audit_log
    FOR EACH ROW
    EXECUTE FUNCTION audit_log_no_update_delete();

