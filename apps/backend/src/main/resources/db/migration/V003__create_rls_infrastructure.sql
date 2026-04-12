-- V003: Row-Level Security infrastructure for multi-tenant isolation.
--
-- Strategy:
--   1. Application sets `app.branch_id` per transaction via SET LOCAL.
--   2. current_branch_id() reads that setting.
--   3. RLS policies on tenant-scoped tables enforce branch_id = current_branch_id().
--   4. Defense-in-depth: Spring also filters by branch_id at the repository layer.
--
-- RLS is NOT enabled on the `branches` table itself (it's the lookup table).

-- Function to read the current tenant context set by the application
CREATE OR REPLACE FUNCTION current_branch_id()
RETURNS UUID
LANGUAGE sql
STABLE
AS $$
    SELECT nullif(current_setting('app.branch_id', true), '')::UUID;
$$;

COMMENT ON FUNCTION current_branch_id() IS
    'Returns the branch_id set by the application via SET LOCAL app.branch_id. '
    'Returns NULL if not set — RLS policies will block all rows.';

-- Helper function: apply RLS policy to any tenant-scoped table.
-- Called from repeatable migration R__rls_policies.sql whenever tables are added.
CREATE OR REPLACE FUNCTION apply_rls_policy(target_table TEXT)
RETURNS VOID
LANGUAGE plpgsql
AS $$
DECLARE
    policy_name TEXT := 'tenant_isolation_' || target_table;
BEGIN
    EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', target_table);
    EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', target_table);

    -- Drop existing policy if present (idempotent)
    BEGIN
        EXECUTE format('DROP POLICY IF EXISTS %I ON %I', policy_name, target_table);
    EXCEPTION WHEN undefined_object THEN
        NULL;
    END;

    EXECUTE format(
        'CREATE POLICY %I ON %I '
        'FOR ALL '
        'USING (branch_id = current_branch_id()) '
        'WITH CHECK (branch_id = current_branch_id())',
        policy_name, target_table
    );
END;
$$;

COMMENT ON FUNCTION apply_rls_policy(TEXT) IS
    'Enables RLS on the given table and creates a policy filtering by branch_id = current_branch_id(). '
    'Idempotent — safe to call multiple times.';
