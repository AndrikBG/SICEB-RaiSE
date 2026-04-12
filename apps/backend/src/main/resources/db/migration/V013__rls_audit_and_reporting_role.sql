-- V013: RLS for audit_log + admin_reporting BYPASSRLS role + INSERT-only privileges
-- Drivers: SEC-02, CRN-17, CRN-18
-- Tasks: T3.4.1, T3.4.2, T3.5.1

-- ============================================================
-- Custom RLS for audit_log
-- SELECT: branch-scoped (Director General uses BYPASSRLS via admin_reporting)
-- INSERT: always allowed (events created in various branch contexts, some with NULL branch_id)
-- NOTE: Does NOT use apply_rls_policy() because audit_log needs
--       split SELECT/INSERT policies.
-- ============================================================
ALTER TABLE audit_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_log FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS audit_log_branch_read ON audit_log;
CREATE POLICY audit_log_branch_read ON audit_log
    FOR SELECT
    USING (branch_id = current_branch_id());

DROP POLICY IF EXISTS audit_log_insert_all ON audit_log;
CREATE POLICY audit_log_insert_all ON audit_log
    FOR INSERT
    WITH CHECK (TRUE);

-- ============================================================
-- admin_reporting role — BYPASSRLS for cross-branch reports (T3.4.2)
-- Director General uses SET ROLE admin_reporting within transaction scope.
-- ============================================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'admin_reporting') THEN
        CREATE ROLE admin_reporting NOLOGIN BYPASSRLS;
    END IF;
END $$;

GRANT SELECT ON ALL TABLES IN SCHEMA public TO admin_reporting;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO admin_reporting;

DO $$
BEGIN
    EXECUTE format('GRANT admin_reporting TO %I', current_user);
END $$;

-- ============================================================
-- audit_log INSERT-only privileges (T3.5.1)
-- Defense-in-depth: DB-level restriction on UPDATE/DELETE/TRUNCATE.
-- Triggers already prevent UPDATE/DELETE; this adds TRUNCATE protection
-- and ensures the privilege model matches the immutability contract.
-- NOTE: The table owner retains full privileges. In production, a
-- separate application DB role (non-owner) should be used.
-- ============================================================
REVOKE UPDATE, DELETE, TRUNCATE ON audit_log FROM PUBLIC;

COMMENT ON TABLE audit_log IS
    'Immutable, hash-chained audit log (IC-03). INSERT-only: UPDATE/DELETE blocked by '
    'triggers; TRUNCATE revoked from PUBLIC. Owner retains privileges for migrations.';
