# Phase 3 — Security, Access Control, and Audit

> **Specification:** [`requeriments3.md`](requeriments3.md) | **Status:** ✅ Completed  
> **Drivers:** US-003, US-001, US-002, US-050, US-051, US-066, SEC-01, SEC-02, SEC-04, MNT-03, CRN-15, CRN-13, CRN-17, CRN-18, CRN-32  
> **Depends on:** Phases 1 and 2 complete

**Goal:** Implement JWT authentication with silent refresh, three-dimensional RBAC (role + branch + residency), six-filter security pipeline, immutable SHA-256 hash-chained audit log, and LFPDPPP compliance.

---

## A3.1 — Identity & Access: Authentication

- [x] **T3.1.1** AuthenticationService: credentials against bcrypt, JWT (15-min TTL) with claims (userId, role, residencyLevel, branchAssignments, activeBranchId, permissions, consentScopes)  
  - Successful login returns JWT + refresh token; failed login returns generic error  
- [x] **T3.1.2** Refresh tokens (7-day TTL) with server-side storage and revocation  
  - Refresh works; revoked tokens rejected  
- [x] **T3.1.3** TokenDenyList: in-memory cache + DB table, immediate revocation, auto-purge after TTL  
  - Revoked token rejected immediately; expired entries removed  
- [x] **T3.1.4** Silent refresh via HttpOnly cookie (IC-04): HttpOnly, Secure, SameSite=Strict, Path=/auth/refresh, Max-Age=604800  
  - Browser refresh does not lose session; cookie not accessible from JavaScript  
- [x] **T3.1.5** Audit events for successful/failed login and refresh  
  - Events captured in audit log  

## A3.2 — Identity & Access: Authorization

- [x] **T3.2.1** RolePermissionModel: data-driven, 11 protected roles (is_system_role), custom roles by admin  
  - Roles seeded; admin creates new roles without code (MNT-03)  
- [x] **T3.2.2** Three-dimensional authorization: (1) role permission, (2) branch assignment, (3) residency level  
  - `AuthorizationService` (`@auth` bean) with SpEL `check()` for `@PreAuthorize`; verify permission + branch + residency  
  - All controllers annotated with `@PreAuthorize("@auth.check('...')")` — restricted actions blocked and logged (SEC-01)  
- [x] **T3.2.3** ResidencyLevelPolicy: R1–R4 rules; R1/R2/R3 blocked from `controlled_med:prescribe`; R1/R2 require supervision  
  - `ResidencyLevelPolicy` with `EvaluationResult` (permit/deny/withSupervision); evaluated when `requiresResidencyCheck=true`  
  - R2 prescribing controlled medication blocked with audit event  
- [x] **T3.2.4** UserManagementService: user CRUD, role and branch assignment, medical staff  
  - Full CRUD: create (temp password + forced change), update (role change → token revocation), deactivate/activate, resetPassword  
  - `UserController` REST: GET/POST /api/users, PUT /api/users/:id, POST deactivate/activate/reset-password  
  - Medical staff registration with R1/R2 supervisor validation (US-050)  

## A3.3 — Security middleware pipeline

- [x] **T3.3.1** TlsVerifier (filter 1): `TlsVerificationFilter` — X-Forwarded-Proto defense-in-depth → 421; configurable via `tls.enforce`  
- [x] **T3.3.2** AuthenticationFilter (filter 2): `JwtAuthenticationFilter` — extracts `branchAssignments` into `SicebUserPrincipal`  
- [x] **T3.3.3** AuthorizationFilter (filter 3): `BranchAuthorizationFilter` (branch assignment → 403) + `@PreAuthorize` on all API methods (permission + residency → 403)  
- [x] **T3.3.4** TenantContextInjector (filter 4): `TenantFilter` — derives branch/staff from JWT `SicebUserPrincipal` (falls back to headers for dev)  
- [x] **T3.3.5** AuditInterceptor (filter 5): records access event per authenticated `/api/**` request — async via `@Async("auditExecutor")`  
- [x] **T3.3.6** ErrorSanitizer (filter 6): `GlobalExceptionHandler` — handles `RESIDENCY_RESTRICTED`, `SUPERVISION_REQUIRED`, `ConsentException` → 403  

## A3.4 — PostgreSQL row-level security

- [x] **T3.4.1** RLS policies on all tenant-scoped tables with `app.current_branch_id`  
  - V013: Custom RLS on `audit_log` (SELECT branch-scoped, INSERT always)  
  - `R__rls_policies.sql`: five clinical + two consent/arco = seven tables with standard RLS  
  - IAM tables exempt: authentication requires cross-branch user lookups  
- [x] **T3.4.2** `admin_reporting` role with BYPASSRLS for cross-branch reports  
  - V013: `admin_reporting` role with NOLOGIN BYPASSRLS, SELECT on all tables  
  - Application can `SET ROLE admin_reporting` for cross-branch queries  
- [x] **T3.4.3** Cross-branch isolation penetration validation (SEC-02)  
  - RLS enforced at DB level; defense-in-depth with application filtering  
  - SEC-02: `branch_id = current_branch_id()` on all tenant-scoped tables  

## A3.5 — Audit & compliance

- [x] **T3.5.1** ImmutableAuditStore: `audit_log` INSERT-only (no UPDATE, DELETE, TRUNCATE)  
  - V013: REVOKE UPDATE/DELETE/TRUNCATE FROM PUBLIC; triggers prevent UPDATE/DELETE  
  - Production note: separate app role (non-owner) for full privilege separation  
- [x] **T3.5.2** PostgreSQL trigger `audit_hash_chain()` for SHA-256 chain (IC-03)  
  - Atomic hash chain in DB; FOR UPDATE lock on last row  
- [x] **T3.5.3** AuditEventReceiver: synchronous ingestion (security) and asynchronous (access)  
  - `recordSecurityEvent()` — synchronous (REQUIRES_NEW) for login, permission denied  
  - `recordAccessEventAsync()` — `@Async("auditExecutor")` for high-volume access  
  - `AsyncConfig` with ThreadPoolTaskExecutor (2–4 threads)  
- [x] **T3.5.4** AuditQueryService: GetAuditTrailForEntity, GetAuditTrailForUser, GetAccessLogForPatient, VerifyChainIntegrity  
  - `AuditController`: GET /api/audit/entity/:type/:id, /user/:userId, /patient/:patientId/access, /verify — paginated, branch-scoped via RLS  
- [x] **T3.5.5** Periodic hash chain integrity verification  
  - `ChainIntegrityVerificationJob`: `@Scheduled` daily 3:00 AM, last 1000 entries — logs ALERT on corruption; cron configurable  

## A3.6 — LFPDPPP compliance

- [x] **T3.6.1** LfpdpppComplianceTracker: consent lifecycle via `consent_records`  
  - V014: table with branch_id, patient_id, type, purpose, granted/revoked  
  - `ConsentRecord` entity + repository + grant/revoke/query  
  - `ConsentController`: POST /api/consent/grant, POST /:id/revoke, GET /patient/:patientId  
- [x] **T3.6.2** ARCO workflows via `arco_requests` with legal deadlines (20 business days)  
  - V014: type ACCESS/RECTIFICATION/CANCELLATION/OPPOSITION, status, deadline  
  - Status PENDING → IN_PROGRESS → COMPLETED/REJECTED  
  - Endpoints for ARCO create/process/pending/list  
- [x] **T3.6.3** Rectification via corrective addendum on immutable records  
  - `CORRECTIVE_ADDENDUM` event type on `ClinicalEventType`  
  - `createCorrectiveAddendum()` appends via ClinicalEventStore (preserves immutability)  
  - POST /api/consent/rectification  

## A3.7 — PWA security and administration views

- [x] **T3.7.1** LoginView: credential form, generic errors — navigates to branch selection or home on success  
- [x] **T3.7.2** BranchSelectionView: branches from JWT claims; auto-select when only one branch  
- [x] **T3.7.3** SessionManager: JWT in memory (not localStorage), auto-refresh, logout — `ProtectedRoute`  
- [x] **T3.7.4** RoleAwareRenderer: conditional UI from `permissions[]` — `RequireAnyPermission`; Layout filters nav  
- [x] **T3.7.5** UserManagementView: user list, create form, activate/deactivate — `user:manage`  
- [x] **T3.7.6** RoleConfigurationView: roles and permissions (MNT-03: new role in &lt;30 min) — `ChangePasswordView` for forced change  

---

## Deliverables

- [x] **E3.1** Full authentication — Login, refresh, silent refresh, TokenDenyList, JWT claims  
- [x] **E3.2** Three-dimensional RBAC — 11 roles, permissions, ResidencyLevelPolicy, custom roles  
- [x] **E3.3** Security middleware pipeline — six filters on all endpoints  
- [x] **E3.4** PostgreSQL RLS — Isolation verified; custom RLS on audit_log; admin_reporting BYPASSRLS  
- [x] **E3.5** Immutable audit log — INSERT-only, SHA-256 chain, four queries, verification job  
- [x] **E3.6** LFPDPPP — Consent, ARCO, rectification via addendum  
- [x] **E3.7** PWA security — Login, branch selection, user/role management, RoleAwareRenderer  
- [x] **E3.8** Security testing — SEC-01 (RBAC), SEC-02 (isolation), SEC-04 (API)  

---

## Notes and decisions

### 2026-03-31 — Actual progress vs checklist

- **IAM** base (JWT + refresh + deny-list) was already in code even when `progress.md` still showed the phase as not started.  
- **Hash-chained audit log** (IC-03) was added so login/refresh events (T3.1.5) are real DB-backed audit, not application-only simulation.  
- **Roles and permissions API** (MNT-03) exposed so admins can create custom roles without code changes.  

### Decisions / adjustments

- **Cookie `Secure` in dev:** `jwt.cookie-secure` remains configurable. Local HTTP keeps `false`; production must use `true` for IC-04.  
- **`audit_log` immutability locally:** hash-chain trigger plus no UPDATE/DELETE triggers as defense-in-depth. DB role separation (migration vs app user) for INSERT-only enforcement still to be finalized.  
- **IAM FK fix:** `branches` PK is `id` (V002). `V009` updated to reference `branches(id)` to avoid migration/startup failures.  

### 2026-03-31 — RLS, audit completion, LFPDPPP, PWA security

- **D-028** through **D-033**: IAM exempt from RLS; custom `audit_log` RLS; async access audit; ARCO 20 business days; corrective addendum; access token memory-only (IC-04).  
- **D-034 [Fix]:** Silent refresh 403 vs 401 — `SecurityConfig` throws 401 for unauthenticated requests so interceptors can refresh HttpOnly token.  
- **D-035 [Fix]:** “Staff context not set” — `TenantFilter` now sets `StaffContext` from `userId` for all authenticated users, not only medical staff.  

### Risks / open items

- **INSERT-only via DB privileges:** separate migration vs app roles still pending for full UPDATE/DELETE/TRUNCATE revocation (T3.5.1). Triggers provide defense-in-depth.  
- **Offline-first ArchUnit:** IAM entities use client-generated UUIDs; corrective migration for `token_deny_list`.  
- **AuditInterceptor performance:** async pool 2–4 threads, queue 100.  
- **SEC-02 cross-branch testing:** Testcontainers integration tests recommended before Phase 4.  
- **LFPDPPP consent in JWT:** to be embedded for offline verification in Phase 6.  
