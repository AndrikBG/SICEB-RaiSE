# Project Memory — SICEB

> This file is the **central project memory**. It records phase status, decisions, blockers, and lessons learned. Update after each work session.

---

## General Status


| Phase          | Status         | Progress     | Last Updated |
| -------------- | -------------- | ------------ | ------------ |
| 1 — Foundation | ✅ Completed   | 34/34 tasks  | 2026-03-23   |
| 2 — Clinical   | ✅ Completed   | 25/25 tasks + fixes  | 2026-03-24   |
| 3 — Security   | ✅ Completed   | 27/27 tasks  | 2026-03-31   |
| 4 — Inventory  | 🔲 Not started | 0/17 tasks   | —            |
| 5 — Pharmacy   | 🔲 Not started | 0/20 tasks   | —            |
| 6 — Offline    | 🔲 Not started | 0/17 tasks   | —            |
| 7 — Reporting  | 🔲 Not started | 0/16 tasks   | —            |


**Legend:** 🔲 Not started | 🔵 In progress | ✅ Completed | ⛔ Blocked

---

## Current Phase

**Phase:** 3 — Security, access control, and audit ✅ COMPLETED  
**Next phase:** 4 — Multi-branch operations and inventory  
**Next step:** Begin [`phase4-inventory.md`](phase4-inventory.md) — A4.1

---

## Decision Log


| #     | Date       | Decision                                                                            | Context                                                            | Discarded Alternatives                                            |
| ----- | ---------- | ----------------------------------------------------------------------------------- | ------------------------------------------------------------------- | ------------------------------------------------------------------- |
| D-001 | 2026-03-23 | Stack: Spring Boot 3.x (Java 21) + React + TypeScript + Vite + Workbox | Evaluated 4 frontend and 4 backend stacks against SICEB drivers | NestJS, ASP.NET Core, Spring/Kotlin, FastAPI; Angular, Vue, SolidJS |
| D-002 | 2026-03-23 | Local Docker first; cloud post Phase 6 | Validate full functionality before incurring cloud costs; migration by config, not code | Direct cloud development from Phase 1 |
| D-003 | 2026-03-23 | UI Library: Shadcn/ui + Tailwind CSS v4 | Lighter PWA bundle, no runtime CSS-in-JS, full control | Ant Design (heavier, runtime CSS) |
| D-004 | 2026-03-23 | Package manager: pnpm | npm hung on Windows; pnpm faster, stricter deps | npm, yarn |
| D-005 | 2026-03-23 | CI/CD: GitHub Actions (2 parallel jobs) | Git repo already on GitHub; backend + frontend jobs | GitLab CI, Azure DevOps |
| D-006 | 2026-03-23 | Merged A1.0 and A1.1 overlapping tasks | Avoided duplicate work on Docker Compose, PostgreSQL, Flyway | Sequential execution with duplication |
| D-007 | 2026-03-23 | Spring Boot 3.5.12 (latest 3.x stable) | Plan specifies 3.x; 4.0.4 available but would require plan change | Spring Boot 4.0.x |
| D-008 | 2026-03-23 | PWA: vite-plugin-pwa 1.2.0 with `registerType: 'prompt'` | User controls update timing; Workbox generateSW | Manual SW management |
| D-009 | 2026-03-23 | IndexedDB: 3 tables (syncQueue, cachedEntities, appSettings) | Offline-first architecture with branch_id isolation | Single monolithic store |
| D-010 | 2026-03-23 | Auth token in sessionStorage (not localStorage) | Reduces XSS token-theft window; tab-isolated | localStorage persistence |
| D-011 | 2026-03-23 | workbox-window as explicit dependency | Vite 8 Rolldown can't resolve transitive virtual module deps | Waiting for vite-plugin-pwa update |
| D-012 | 2026-03-23 | Shared DB with branch_id discriminator (CRN-29) | Hibernate @TenantId + PostgreSQL RLS defense-in-depth | Schema-per-tenant, DB-per-tenant |
| D-013 | 2026-03-23 | `SET LOCAL app.branch_id` via TenantAwareDataSource | Transaction-scoped, no pool stale state | Session-scoped SET, AOP interceptor |
| D-014 | 2026-03-23 | TLS via DB_SSL_PARAMS env var on JDBC URL | Empty for local Docker, `?sslmode=require` for cloud | Hardcoded SSL mode |
| D-015 | 2026-03-23 | Tenant from `X-Branch-Id` header (dev); JWT claims in Phase 3 | Testable multi-tenancy before auth | Block until Phase 3 |
| D-016 | 2026-03-23 | ArchUnit tests in `com.siceb.architecture` package (3 classes, 16 tests) | Separate test package for structural rules; static ClassFileImporter per class | @ArchTest annotation style, archunit.properties config |
| D-017 | 2026-03-23 | DomainStubsArchTest updated: `ACTIVATED_MODULES` set allows real code in Phase 2 modules | IC-01 now only enforces stubs on non-activated modules (7 of 10) | Modify package structure to separate stubs |
| D-018 | 2026-03-23 | StaffContext via `X-Staff-Id` header alongside TenantContext | Same pattern as tenant; enables testing without auth before Phase 3 | Block development until Phase 3 auth |
| D-019 | 2026-03-23 | DB triggers `trg_clinical_events_no_update/no_delete` for append-only | Defense-in-depth: even direct DB access cannot mutate clinical events | Application-only enforcement |
| D-020 | 2026-03-23 | ClinicalTimeline queries event store directly (no materialized view) | Composite index on (record_id, occurred_at) provides efficient chronological access | Separate materialized view table |
| D-021 | 2026-03-23 | GlobalExceptionHandler maps domain exceptions → ErrorResponse | Consistent structured error format across all API endpoints | Per-controller exception handling |
| D-022 | 2026-03-23 | React Router DOM with Layout component for clinical navigation | react-router-dom was already in dependencies; consistent nav + offline indicator | Custom routing, single-page tabs |
| D-023 | 2026-03-23 | useClinicalStore (Zustand) as ClinicalStateManager with transient wizard state | Follows established Zustand pattern; wizard state not persisted (session-only) | React Context, Redux |
| D-024 | 2026-03-31 | Audit hash-chain enforced in PostgreSQL trigger (IC-03) | Serializes chain atomically in DB; prevents concurrency bifurcation | Application-level hashing; post-hoc repair |
| D-025 | 2026-03-31 | Refresh token cookie `Secure` controlled by `jwt.cookie-secure` | Enables local HTTP dev; production must set `true` to meet IC-04 | Hardcode Secure=true (breaks local dev) |
| D-026 | 2026-03-31 | Authorization via `@PreAuthorize("@auth.check('...')")` SpEL | Leverages Spring Security method security with custom 3-dimensional AuthorizationService bean; clean, testable, no custom annotations needed | Custom filter with route→permission map; @RequiresPermission annotation + AOP |
| D-027 | 2026-03-31 | TenantFilter dual-source: JWT claims primary, X-headers fallback | Enables authenticated (production) and unauthenticated (dev/test) tenant context in same codebase | Separate filters for JWT vs header-based tenant; environment profiles |
| D-028 | 2026-03-31 | IAM tables exempt from RLS | Auth requires cross-branch user lookups; protection via @PreAuthorize | Apply RLS + separate auth DB role |
| D-029 | 2026-03-31 | audit_log custom RLS (SELECT scoped, INSERT always) | Events have various branch contexts including null; standard apply_rls_policy() insufficient | Standard RLS (would block login failure audit inserts) |
| D-030 | 2026-03-31 | AuditInterceptor switched to @Async("auditExecutor") | Prevents blocking request pipeline for high-volume access events | Synchronous (blocks response) |
| D-031 | 2026-03-31 | ARCO deadline = 20 business days (Mon-Fri) | LFPDPPP legal requirement | Calendar days (not compliant) |
| D-032 | 2026-03-31 | Corrective addendum as CORRECTIVE_ADDENDUM event type | Preserves immutability while enabling LFPDPPP rectification | Direct record modification (violates immutability) |
| D-033 | 2026-03-31 | Access token removed from sessionStorage persist | IC-04: JWT in memory only; silent refresh via HttpOnly cookie | Persist token (vulnerable to XSS) |


---

## Blockers and Issues


| #   | Date | Description | Status | Resolution |
| --- | ---- | ----------- | ------ | ---------- |
| B-001 | 2026-03-23 | npm install hangs repeatedly on Windows (>3 min with no output) | ✅ Resolved | Switched to pnpm (D-004); installs complete in ~3 min |
| B-002 | 2026-03-23 | Docker Maven build fails with "Premature end of Content-Length" on slow network | ✅ Resolved | Removed separate `dependency:go-offline` step; added Maven retry config |
| B-003 | 2026-03-23 | vite-plugin-pwa peer dep warning: expects Vite ≤7, we have Vite 8 | ⚠️ Tracked | Build works; awaiting vite-plugin-pwa update with Vite 8 support |
| B-004 | 2026-03-23 | Vite 8 (Rolldown) fails to resolve `workbox-window` from virtual module | ✅ Resolved | Installed `workbox-window` as explicit dependency (D-011) |


---

## Lessons Learned


| #   | Phase | Lesson |
| --- | ----- | ------- |
| L-001 | 1 | Use pnpm instead of npm on Windows — significantly more reliable for large dependency trees |
| L-002 | 1 | Docker multi-stage Maven builds: prefer single `mvn package` over `dependency:go-offline` + `package` when network is unreliable; add `-Dmaven.wagon.http.retryHandler.count=5` |
| L-003 | 1 | Vite 8 uses Rolldown instead of Rollup — some plugins' transitive virtual module deps need explicit installation |
| L-004 | 2 | CQRS projectors must run on both new writes and idempotent replays to keep read models convergent after offline sync |
| L-005 | 2 | Read model fields populated from related aggregates (e.g. patientName in PendingLabStudy) must be resolved at write time — empty placeholders lead to UI data quality issues |
| L-006 | 2 | Path-vs-body ID consistency checks on nested REST endpoints (e.g. `/consultations/{id}/prescriptions`) prevent subtle client bugs |
| L-007 | 3 | IAM tables cannot use standard RLS — authentication requires cross-branch user lookups (e.g. login before branch selection). Protect via @PreAuthorize instead |
| L-008 | 3 | Audit log INSERT policies must allow NULL branch_id — system events (login failures, scheduled jobs) occur outside tenant context |
| L-009 | 3 | Access tokens must NOT be persisted to sessionStorage (XSS risk per IC-04). Use memory-only storage + HttpOnly cookie silent refresh for session continuity |
| L-010 | 3 | Async audit ingestion via @Async prevents blocking the API request pipeline, but security-critical events (permission denials) should remain synchronous |
| L-011 | 3 | Immutable clinical records + LFPDPPP rectification rights are reconciled via corrective addendum events — the original record is never modified |


---

## Session Notes

### Session — 2026-03-23 (Planning)

- Created implementation plan with 7 phases
- Evaluated and selected technology stack
- Restructured plan into separate files per phase
- Decision D-002: local Docker development; cloud migration post Phase 6
- Added activity A1.0 (7 Docker tasks) in phase1-foundation.md

### Session — 2026-03-23 (Implementation Start)

- **Completed A1.0** (7/7 tasks): Full Docker local environment
  - `docker-compose.yml` with PostgreSQL 17, Spring Boot API, Vite PWA
  - Multi-stage Dockerfiles for both backend and frontend
  - Flyway V001 migration (pg_trgm + pgcrypto extensions) runs on startup
  - All config via env vars (`.env` + `application.yml`)
  - README.md with quickstart and development commands
- **Completed A1.1** (5/5 tasks): Dev environment and CI/CD
  - Monorepo structure: `apps/backend/` (Maven) + `apps/frontend/` (pnpm)
  - GitHub Actions CI with 2 parallel jobs (backend + frontend)
  - ESLint + TypeScript strict mode for frontend; Maven compiler for backend
- **Verified full stack** runs: DB healthy, API `{"status":"UP"}`, PWA serving at :5173
- Decisions D-003 through D-007 recorded
- Next: A1.2 — Shared Kernel Implementation

### Session — 2026-03-23 (PWA Client Scaffolding)

- **Completed A1.4** (5/5 tasks): Full PWA Client Scaffolding
  - **T1.4.1** — vite-plugin-pwa 1.2.0 with Workbox `generateSW`, prompt-based updates, `PwaUpdatePrompt` component, API `NetworkFirst` caching; manifest with SICEB branding and SVG icons
  - **T1.4.2** — Dexie.js database with 3 tables (`syncQueue`, `cachedEntities`, `appSettings`), all tenant-scoped by `branchId`, compound indexes, scoped query helpers
  - **T1.4.3** — Zustand stores: `useAuthStore` (sessionStorage persist), `useUiStore` (localStorage persist, sidebar/theme/toasts), `useSyncStore` (connection status, pending changes); `useOnlineStatus` hook
  - **T1.4.4** — Axios client with JWT interceptor + auto-logout on 401; STOMP client with `beforeConnect` JWT injection, auto-reconnect, heartbeat, subscribe/publish helpers
  - **T1.4.5** — Browserslist (last 2 versions Chrome/Edge/Safari/Firefox), `build.target: 'es2023'`; production build verified (196 KB JS + 12 KB CSS)
- Resolved Vite 8 + vite-plugin-pwa incompatibility (B-004 → D-011)
- Decisions D-008 through D-011 recorded
- Next: A1.5 — Multi-Tenant Database Configuration

### Session — 2026-03-23 (Multi-Tenant Database)

- **Completed A1.5** (4/4 tasks): Multi-Tenant Database Configuration
  - **T1.5.1** — V002 migration: `branches` table (UUID PK, name, address, is_active, timestamps); dev seed branch
  - **T1.5.2** — V003 migration: `current_branch_id()` + `apply_rls_policy()` functions; repeatable `R__rls_policies.sql`; Spring side: `TenantContext`, `TenantFilter`, `TenantAwareDataSource` (DelegatingDataSource wrapping HikariCP), `TenantConnectionInterceptor` (Hibernate CurrentTenantIdentifierResolver); 9 new unit tests
  - **T1.5.3** — HikariCP tuning (20s connect, 5min idle, 15min max-life, 30s leak detect); TLS via `DB_SSL_PARAMS` env var
  - **T1.5.4** — Flyway V001–V003 + repeatable; naming convention established; verified against Docker PostgreSQL 17
- Verified end-to-end: `docker compose up db`, Spring Boot boots, all 4 migrations run, `current_branch_id()` resolves correctly, `branches` table seeded
- 53 total tests passing (43 shared + 9 branch + 1 app)
- Decisions D-012 through D-015 recorded
- Next: A1.6 — Automated Architecture Tests

### Session — 2026-03-23 (Architecture Tests — Phase 1 Complete)

- **Completed A1.6** (3/3 tasks): Automated Architecture Tests
  - **T1.6.1** — `DomainStubsArchTest` (4 tests): IC-01 enforcement — no business logic, no @Entity, no Spring components in domain stubs; verifies all 10 domain module packages exist
  - **T1.6.2** — `DependencyArchTest` (9 tests): CRN-27 enforcement — domain modules cycle-free, top-level slices cycle-free, shared kernel has no internal dependencies, domain→no api/config, platform→no domain/api
  - **T1.6.3** — `OfflineConventionsArchTest` (3 tests): Offline-first enforcement — @Id fields must be UUID/EntityId, no @GeneratedValue allowed, IdempotencyKey type exists in shared kernel
- **Phase 1 COMPLETE:** 34/34 tasks, 69 total tests (43 shared + 9 tenant + 16 architecture + 1 app)
- Decision D-016 recorded
- Next: Phase 2 — Clinical Care

### Session — 2026-03-23 (Phase 2 — Clinical Care Complete)

- **Completed A2.1** (4/4 tasks): ClinicalEventStore Implementation
  - V004–V007 Flyway migrations: patients, medical_records, clinical_events (IC-02 hybrid JSONB), patient_search_view, pending_lab_studies_view
  - DB triggers enforce append-only (no UPDATE/DELETE on clinical_events)
  - `ClinicalEventStore` service with idempotent append via IdempotencyKey
  - RLS policies applied to all 5 new tables via `R__rls_policies.sql`

- **Completed A2.2** (4/4 tasks): Clinical Care Aggregates
  - `Patient` entity with builder, PatientType enum (auto-discount US-020), guardian validation (US-023), phone validation
  - `MedicalRecord` entity with unique patient constraint (CRN-02)
  - `ConsultationService` persists SOAP format consultations with supervision flag (USA-02)
  - `StaffContext` (X-Staff-Id header) for dev-mode staff identity

- **Completed A2.3** (3/3 tasks): Prescriptions Module
  - `PrescriptionCommandHandler` creates PRESCRIPTION events within consultation context
  - PrescriptionItem with 8 fields (medication, quantity, dosage, frequency, duration, route, instructions)
  - Validates consultation existence and non-empty items

- **Completed A2.4** (3/3 tasks): Laboratory Module
  - `LabStudyCommandHandler` creates LAB_ORDER events + PendingLabStudy read model entries
  - `RecordLabResult` creates LAB_RESULT events; transitions study status to COMPLETED
  - `PendingLabStudyRepository` with branch+status filters

- **Completed A2.5** (4/4 tasks): CQRS Read Models
  - `PatientSearchEntry` entity with pg_trgm indexes for sub-1s search
  - `ClinicalTimelineService` with paginated chronological projection
  - `Nom004RecordService` builds NOM-004-SSA3-2012 structured record (6 mandatory sections)
  - `ClinicalEventProjector` updates search view on RECORD_CREATED and CONSULTATION

- **Completed A2.6** (6/6 tasks): REST Interfaces
  - `ClinicalController`: POST /patients, POST /consultations, POST /consultations/:id/prescriptions, GET /patients/search, GET /patients/:id/timeline, GET /patients/:id/nom004
  - `LabStudyController`: POST /consultations/:id/lab-studies, POST /lab-studies/:studyId/results, GET /lab-studies/pending
  - `GlobalExceptionHandler` with structured ErrorResponse

- **Completed A2.7** (5/5 tasks): PWA Clinical Views
  - `PatientSearchView` with search, new patient modal, result table
  - `MedicalRecordView` with timeline tab + NOM-004 tab, expandable events
  - `ConsultationWizard` (4 steps): vitals/diagnosis, prescriptions, lab orders, review
  - `PendingLabStudiesView` with status filters and priority indicators
  - `LabResultEntryForm` for text-only result capture
  - `useClinicalStore` as ClinicalStateManager with wizard state

- **Phase 2 COMPLETE:** 25/25 tasks, backend compiles + all tests pass, frontend builds (305 KB JS + 21 KB CSS)
- Decisions D-017 through D-023 recorded
- Next: Phase 3 — Security, access control, and audit

### Session — 2026-03-24 (Phase 2 Fix Closure)

- **Closed operational gaps in Phase 2** (without moving scope into Phase 3+):
  - CQRS read-model convergence fixed: projection now executed from `ClinicalEventStore` on both new and idempotent replay events.
  - `CONSULTATION` projection repaired to resolve patient via `recordId` when payload lacks `patientId`.
  - API consistency guardrails added: path/body `consultationId` mismatch now returns validation error in prescription/lab-order endpoints.
  - Added duplicate-check endpoint: `GET /api/patients/duplicates` for pre-save homonym alerts.
  - Expanded patient search criteria to include CURP, credential number, phone and UUID exact text (`US-028` basic closure).
  - Added Flyway `V008__extend_patient_search_lookup_fields.sql` for search projection columns/indexes.
  - Frontend `NewPatientModal` now: duplicate pre-check, stronger validation error rendering, and `activePatient` hydration before routing.
  - Lab pending queue now stores patient name from aggregate instead of blank values.
- **Validation run:**
  - Backend: `mvn test` → 69/69 passing.
  - Frontend: `pnpm build` passing.
  - Frontend lint script still reports pre-existing issues outside changed scope (`dev-dist` generated file rules + legacy `MedicalRecordView` lint rule).

### Session — 2026-03-31 (Phase 3 — Auth + Audit Foundation)

- **Marked Phase 3 as started** based on existing code already present (JWT + refresh + deny-list + roles/permissions schema).
- **Implemented hash-chained audit log (IC-03)**:
  - Flyway `V011__create_audit_schema.sql`: `audit_log` table, SHA-256 chain trigger, and no UPDATE/DELETE guard triggers.
  - New backend module: `com.siceb.platform.audit` with `AuditEventReceiver` (sync ingestion) + JPA entity/repository.
- **Wired audit events for auth**:
  - `AuthenticationService` now records audit events for login success/failure, refresh, and logout.
- **Known constraint**: local workstation did not have Maven/Docker available to execute runtime verification; changes are implemented but require `docker compose up` validation on an environment with Docker Desktop running.

### Session — 2026-03-31 (Phase 3 — Roles & Permissions API)

- **Completed T3.2.1**: RolePermissionModel admin endpoints (MNT-03).
  - Fixed `V009__create_security_schema.sql` to reference `branches(id)` (aligns with V002).
  - Added `/api/roles` and `/api/permissions` endpoints + service layer for custom roles.
  - Added `IamException` mapped to the standard error envelope.
  - Ran `mvn test` and fixed offline-first violations in IAM entities (removed `@GeneratedValue`; added `V012__fix_token_deny_list_primary_key.sql`). All backend tests pass.

### Session — 2026-03-31 (Phase 3 — Auth Middleware + User Management)

- **Completed A3.2** (3/3 tasks remaining): Authorization + User Management
  - **T3.2.2** — `AuthorizationService` with three-dimensional RBAC: (1) role permission via `@PreAuthorize("@auth.check('...')")`, (2) branch assignment validation, (3) residency level policy evaluation. Audit-logged denials.
  - **T3.2.3** — `ResidencyLevelPolicy`: R1/R2/R3 blocked from `controlled_med:prescribe` (US-051); R1/R2 require supervision (US-050); `EvaluationResult` record with permit/deny/withSupervision states.
  - **T3.2.4** — `UserManagementService` + `UserController`: full user CRUD, role assignment (with token revocation on change), branch assignments, medical staff with supervisor validation, deactivate/activate/reset-password endpoints. All gated by `user:manage` / `user:read` permissions.

- **Completed A3.3** (6/6 tasks): Security Middleware Pipeline
  - **T3.3.1** — `TlsVerificationFilter`: defense-in-depth HTTPS check via `X-Forwarded-Proto`; configurable `tls.enforce` (false for local dev, true for prod).
  - **T3.3.2** — `JwtAuthenticationFilter` enhanced: now extracts `branchAssignments` claim into `SicebUserPrincipal` (new field + `isAssignedToBranch()` helper).
  - **T3.3.3** — `BranchAuthorizationFilter`: validates authenticated user is assigned to their `activeBranchId` from JWT → 403 if not. `@PreAuthorize` on controller methods handles permission + residency dimensions.
  - **T3.3.4** — `TenantFilter` upgraded: derives `TenantContext` and `StaffContext` from JWT `SicebUserPrincipal`; falls back to `X-Branch-Id`/`X-Staff-Id` headers when no principal (dev/test).
  - **T3.3.5** — `AuditInterceptor` (`HandlerInterceptor`): records access event for every authenticated `/api/**` request; registered via `WebMvcConfig`.
  - **T3.3.6** — `GlobalExceptionHandler` updated: maps `RESIDENCY_RESTRICTED` and `SUPERVISION_REQUIRED` to 403; `SecurityConfig` pipeline wires TLS → JWT → BranchAuth in correct order.

- **Infrastructure changes:**
  - `SicebUserPrincipal`: added `branchAssignments` field (9th parameter)
  - `ErrorCode`: added `RESIDENCY_RESTRICTED` (SICEB-2005), `SUPERVISION_REQUIRED` (SICEB-2006)
  - `AuditEventReceiver.SecurityAuditEvent`: added `permissionDenied()` and `accessEvent()` factories
  - `BranchRepository` created for branch lookups
  - `WebMvcConfig` created for AuditInterceptor registration
  - `SecurityConfig` restructured: injects 3 filters in pipeline order
  - All controllers annotated with `@PreAuthorize` matching permission catalog from V010

- Decisions D-026, D-027 recorded
- Next: A3.4 — PostgreSQL Row-Level Security

### Session — 2026-03-31 (Phase 3 — Completion: RLS, Audit, LFPDPPP, PWA Security)

- **Completed A3.4** (3/3 tasks): PostgreSQL Row-Level Security
  - **T3.4.1** — V013: Custom RLS on `audit_log` (SELECT branch-scoped, INSERT always). 7 tables total with standard RLS (5 clinical + 2 consent/arco). IAM tables exempt from RLS (auth requires cross-branch lookups).
  - **T3.4.2** — V013: `admin_reporting` BYPASSRLS role for Director General cross-branch reports. Application can `SET ROLE admin_reporting` within transaction.
  - **T3.4.3** — RLS policies verified at DB level. SEC-02 defense-in-depth established.

- **Completed A3.5** (4/4 remaining tasks): Audit & Compliance
  - **T3.5.1** — V013: REVOKE UPDATE/DELETE/TRUNCATE on audit_log FROM PUBLIC. Triggers prevent UPDATE/DELETE. Full privilege separation documented for production.
  - **T3.5.3** — `AuditEventReceiver` now has `recordAccessEventAsync()` with `@Async("auditExecutor")`. `AsyncConfig` provides ThreadPoolTaskExecutor (2-4 threads, queue 100). Security events remain synchronous.
  - **T3.5.4** — `AuditQueryService` with 4 operations: entity trail, user trail, patient access log (LFPDPPP), chain integrity verification. `AuditController` with REST endpoints gated by `audit:read`/`audit:verify`.
  - **T3.5.5** — `ChainIntegrityVerificationJob` @Scheduled daily at 3:00 AM. Verifies last 1000 entries. Cron configurable.

- **Completed A3.6** (3/3 tasks): LFPDPPP Compliance
  - **T3.6.1** — V014: `consent_records` table. `ConsentRecord` entity with grant/revoke lifecycle. `ConsentController` REST API.
  - **T3.6.2** — V014: `arco_requests` table. ARCO workflows with 20-business-day deadline calculation. Status transitions: PENDING → IN_PROGRESS → COMPLETED/REJECTED.
  - **T3.6.3** — `CORRECTIVE_ADDENDUM` event type added to `ClinicalEventType`. `LfpdpppComplianceTracker.createCorrectiveAddendum()` appends correction event via ClinicalEventStore (preserves immutability).

- **Completed A3.7** (6/6 tasks): PWA Security & Administration
  - **T3.7.1** — `LoginView`: clean login form, generic errors, auto-navigation to branch selection or home.
  - **T3.7.2** — `BranchSelectionView`: displays assigned branches from JWT; auto-selects single branch.
  - **T3.7.3** — SessionManager: access token NOT persisted (IC-04); silent refresh interceptor with request queue for concurrent 401s; `ProtectedRoute` guard.
  - **T3.7.4** — `RoleAwareRenderer` + `RequireAnyPermission`: conditional UI rendering by permissions. Navigation filtered in Layout.
  - **T3.7.5** — `UserManagementView`: user table, create form, activate/deactivate toggle.
  - **T3.7.6** — `RoleConfigurationView`: role list, permission grid, create custom roles (MNT-03). `ChangePasswordView` for forced change.

- **Phase 3 COMPLETE:** 27/27 tasks. All backend activities + LFPDPPP + PWA security implemented.
- Decisions D-028 through D-033 recorded.
- R__rls_policies.sql updated: 7 tenant-scoped tables + audit_log custom RLS.
- Flyway migrations: V011–V014 + updated R__rls.
- Frontend: 8 new files, 4 modified. Auth flow, admin views, RoleAwareRenderer.
- Next: Phase 4 — Multi-branch operations and inventory

---

## Quick Reference

- **General plan:** [`implementationPlan.md`](implementationPlan.md)
- **ADD specs:** [`requeriments1.md`](requeriments1.md) (iter. 1), [`requeriments2.md`](requeriments2.md) (iter. 2), [`requeriments3.md`](requeriments3.md) (iter. 3)
- **Phase 1 guide:** [`phase1-foundation.md`](phase1-foundation.md)
- **Phases 2–7 guides:** [`phase2-clinical.md`](phase2-clinical.md) … [`phase7-reporting.md`](phase7-reporting.md)

