-- Repeatable migration: (Re)apply RLS policies to all tenant-scoped tables.
-- This file is re-executed by Flyway whenever its checksum changes.
-- Add new tables here as they are created in versioned migrations.
--
-- Convention: every table with a `branch_id` column (except `branches` itself)
-- must appear here.
--
-- NOTE: audit_log has custom RLS policies defined in V013 (split SELECT/INSERT),
-- so it is NOT included here. IAM tables (users, roles, permissions) are not
-- tenant-scoped — authentication requires cross-branch user lookups.

-- Phase 2: Clinical Care tables
SELECT apply_rls_policy('patients');
SELECT apply_rls_policy('medical_records');
SELECT apply_rls_policy('clinical_events');
SELECT apply_rls_policy('patient_search_view');
SELECT apply_rls_policy('pending_lab_studies_view');

-- Phase 3: LFPDPPP Compliance tables
SELECT apply_rls_policy('consent_records');
SELECT apply_rls_policy('arco_requests');

-- Phase 4: Branch Management tables (S4.1)
SELECT apply_rls_policy('branch_onboarding_status');
SELECT apply_rls_policy('branch_service_catalog');
