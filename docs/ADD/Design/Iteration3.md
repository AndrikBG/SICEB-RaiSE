# Iteration 3 — Security, Access Control, and Audit Infrastructure

## Goal

Implement the security architecture: authentication, role-based access control with branch-scoped and residency-level permissions, API protection, personal data handling (LFPDPPP), and the centralized immutable audit trail.

Security is cross-cutting and must be layered on top of the structural foundation (Iteration 1) and the clinical data model (Iteration 2) before more modules are built. The audit trail is required by virtually every subsequent feature (pharmacy traceability, supply approval logs, regulatory reports). Residency-level restrictions (R1–R4) are critical for controlled substance enforcement in Iteration 5. Iteration 2 left prescriber-level restrictions structurally supported but not fully enforced — this iteration completes that enforcement.

**Business objective:** Gestión de Personal — Control over physicians, residents, and staff with role-appropriate access.

---

## Step 2: Iteration Drivers

### Primary User Story

| Driver | Type | Description | Why this iteration |
|---|---|---|---|
| **US-003** | Primary US (rank 4) | Role-based permissions — foundational to every user-facing module; supports SEC-02 | Rank 4 primary user story; every module built in Iterations 4–7 depends on RBAC being in place |

### Supporting User Stories

| Driver | Type | Description | Why this iteration |
|---|---|---|---|
| **US-001** | US (HIGH) | Create user accounts with role-based permissions | Foundational — users must exist before any access control is meaningful |
| **US-002** | US (HIGH) | Secure login with credentials | Authentication is the entry point for all system interactions |
| **US-050** | US (HIGH) | Validate residents can only perform actions allowed for their level (R1–R4) | Residency-level enforcement is required before pharmacy (Iteration 5) adds controlled substance dispensation |
| **US-051** | US (HIGH) | Block R1, R2, R3 residents from prescribing controlled medications | Critical regulatory constraint; Iteration 2 left this structurally supported but unenforced |
| **US-066** | US (HIGH) | Register audit log entries for record access (LFPDPPP traceability) | Legal compliance; audit infrastructure must exist before more modules generate auditable events |

### Quality Attribute Scenarios

| Driver | Type | Description | Why this iteration |
|---|---|---|---|
| **SEC-01** | QA Scenario | Role-based access control — 100% of restricted actions blocked and logged | Defines the measurable security guarantee for RBAC |
| **SEC-02** | QA Scenario (High/High) | Branch-level data segmentation — zero unauthorized cross-branch access | One of the 6 high/high scenarios; depends on RBAC + branch context + tenant isolation working together |
| **SEC-04** | QA Scenario | REST API protection — 100% of unauthenticated requests rejected | Hardens the API before it is exposed to more consumers in Iterations 4–7 |
| **MNT-03** | QA Scenario | Admin-configurable roles — new roles operational in <30 min, zero code changes | Requires the role/permission model to be data-driven, not hard-coded |

### Architectural Concerns

| Driver | Type | Description | Why this iteration |
|---|---|---|---|
| **CRN-15** | Concern | RBAC for 11 roles with branch-scoped permissions | Core security concern — defines the entire permission model |
| **CRN-13** | Concern | API hardening: HTTPS enforcement, error sanitization | Must be done before the API surface grows in subsequent iterations |
| **CRN-17** | Concern | Centralized audit log consumed by Iterations 4–7; must exist first | Pharmacy (AUD-02), supply chain (AUD-01), and offline compensation (CRN-45) all depend on the audit infrastructure |
| **CRN-18** | Concern | Audit log immutability — tamper-proof even for DBAs | Regulatory requirement; must be architecturally enforced, not just application-level |
| **CRN-32** | Concern | LFPDPPP personal data protection (consent, access rights) | Patient data is already being stored from Iteration 2; legal compliance must be layered on now |

---

## Step 3: Elements to Refine

| Element | Current State | Refinement Action |
|---|---|---|
| **Identity & Access platform module (IAM)** | Defined in Iteration 1 as a high-level platform module with generic responsibilities. Referenced in SD-01 and SD-02 but no internal decomposition, no permission model, no residency-level logic. | **Decompose internally** into: `AuthenticationService`, `AuthorizationMiddleware`, `RolePermissionModel`, `ResidencyLevelPolicy`, `UserManagementService`, `TokenDenyList`. Define the data-driven permission model supporting MNT-03. |
| **Audit & Compliance platform module (AUD)** | Defined in Iteration 1 as a write-only sink. Iteration 2 wired clinical write operations to emit audit events, but the module itself has no internal structure, no tamper-proof guarantees, and no query interfaces. | **Fully design**: `AuditEventReceiver`, `ImmutableAuditStore` (hash-chained, INSERT-only), `AuditQueryService`, `LfpdpppComplianceTracker`. |
| **API Server — Security middleware layer** | SD-01 shows a generic `Validate token and check permissions` flow. No concrete middleware pipeline, no error sanitization. | **Define** a six-filter pipeline: TlsVerifier → AuthenticationFilter → AuthorizationFilter → TenantContextInjector → AuditInterceptor → ErrorSanitizer. |
| **Prescriptions module (RX)** | `PrescriptionCommandHandler` accepts any authenticated staff member's prescription command. Iteration 2 noted: "fully enforced in Iteration 3." | **Wire** to `AuthorizationMiddleware` and `ResidencyLevelPolicy` so R1/R2/R3 are blocked from prescribing controlled medications. |
| **Clinical Care module — read-side audit hooks** | Iteration 2 wired audit events for clinical writes. No read-side audit hooks exist for LFPDPPP. | **Add** `AuditInterceptor` hooks to read-side queries so every patient data access is logged. |
| **Cloud Database — security and audit schema** | No schema defined for users, roles, permissions, sessions, or audit log entries. | **Define** tables for security (users, roles, permissions, role_permissions, user_branch_assignments, medical_staff, refresh_tokens, token_deny_list) and audit (audit_log with hash chaining, consent_records, arco_requests). |
| **PWA Client — authentication and admin UI** | No login screen, no session management, no role-aware UI, no admin interfaces. | **Extend** with: `LoginView`, `BranchSelectionView`, `SessionManager`, `RoleAwareRenderer`, `UserManagementView`, `RoleConfigurationView`. |

---

## Step 4: Design Concepts

### Architectural Patterns

| Design Concept | Pros | Cons | Discarded Alternatives |
|---|---|---|---|
| **RBAC with branch-scoped and residency-level dimensions** — Three-dimensional permission model: role determines base permissions, branch assignment scopes data visibility, residency level restricts clinical actions. *Addresses: CRN-15, US-003, SEC-01, SEC-02, US-050, US-051* | Captures all three access control dimensions in a single coherent model; branch scoping enforces SEC-02; residency-level dimension is explicit, not scattered | Three-dimensional checks add latency to every request; requires careful permission matrix design; testing surface grows with role × branch × level combinations | **ABAC:** Higher complexity, policy language overhead unjustified. **ACL per resource:** Impractical at 50,000+ records scale |
| **Security middleware pipeline (Chain of Responsibility)** — Ordered filters: TLS → Authentication → Authorization → Tenant Context → Audit → Error Sanitization. *Addresses: CRN-13, SEC-04, US-066, SEC-01* | Single enforcement point; each filter independently testable; ordering guarantees unauthenticated requests never reach business logic | All requests pay the cost of the full pipeline; filter ordering is critical | **Per-endpoint annotations only:** Scattered, error-prone. **Dedicated API gateway:** Excessive for modular monolith |
| **Cryptographic hash-chained append-only audit log** — SHA-256 hash of previous entry + payload. INSERT-only at DB level. *Addresses: CRN-17, CRN-18, AUD-03* | Tamper-evidence detectable by any verifier; defense-in-depth with DB-level restriction; no external infrastructure | Hash chain verification requires sequential read; chain grows indefinitely | **Simple append-only without chaining:** DBA tampering undetectable. **Blockchain:** Extreme overhead, unnecessary consensus |

### Externally Developed Components

| Design Concept | Pros | Cons | Discarded Alternatives |
|---|---|---|---|
| **JWT with embedded claims for stateless authentication** — Short-lived access tokens (15 min) + refresh tokens (7 days). Claims carry role, permissions, residency level, branch assignments. *Addresses: US-002, SEC-04, CRN-43* | Stateless; embedded claims enable offline authorization; short TTL limits exposure; standard format | Token revocation requires deny-list; claims become stale within TTL window; JWT must be stored securely | **Server-side sessions:** Incompatible with offline authorization. **OAuth2 with external IdP:** External dependency, no offline introspection |
| **PostgreSQL Row-Level Security (RLS)** — DB-level tenant filtering via `app.current_branch_id` session variable. *Addresses: SEC-02* | Defense-in-depth below application code; transparent to queries; PostgreSQL-native | Adds overhead to every query; debugging harder with silent filtering | **Application-level WHERE only:** Single enforcement layer, insufficient for High/High scenario |

### Tactics

| Design Concept | Pros | Cons | Discarded Alternatives |
|---|---|---|---|
| **Data-driven role/permission model** — Roles, permissions, and mappings stored in DB. Admin UI for configuration. *Addresses: MNT-03, US-003, CRN-15* | <30 min for new roles, zero code changes; decouples permissions from releases; auditable | Risk of misconfiguration; needs validation layer for regulatory constraints | **Hard-coded roles:** Violates MNT-03. **Config-file roles:** Requires deployment |
| **Residency-level policy as a first-class domain concept** — `ResidencyLevelPolicy` with hierarchical R1–R4 rules. *Addresses: US-050, US-051, SEC-01* | Explicit, centrally maintained; testable in isolation; travels in JWT for offline | Additional abstraction; must sync with permission model | **Generic RBAC per action per level:** Explosion of permissions. **Hard-coded checks per handler:** Scattered, duplicated |
| **LFPDPPP compliance as a cross-cutting data access policy** — Consent verification, ARCO workflows, access logging. *Addresses: CRN-32, US-066* | Architecturally explicit; enforceable at middleware level; ARCO with deadline tracking | Adds access-check overhead; ARCO Rectification requires careful design with immutable records | **Consent as UI checkbox only:** No enforcement. **Post-hoc retrofit:** Expensive and risky |
| **Error sanitization at API boundary** — Terminal filter strips internals, returns `{ code, message, correlationId }`. *Addresses: CRN-13, SEC-04* | Prevents leakage in all error paths; standardized format; correlation ID for debugging | Developers must use correlation IDs for debugging; overly aggressive sanitization could hide useful info | **Verbose errors in production:** Information leakage. **Per-handler formatting:** Inconsistent |

---

## Step 5: Instantiation Decisions

| Instantiation Decision | Rationale |
|---|---|
| **`AuthenticationService` in IAM** — Validates credentials (bcrypt), issues JWT access token (15-min TTL) with claims: userId, role, residencyLevel, branchAssignments, activeBranchId, permissions, consentVerifiedScopes. Issues refresh token (7-day TTL, server-side, revocable). `TokenDenyList` (in-memory cache + DB) for immediate revocation. | Satisfies **US-002**, **SEC-04**. Embedded claims support CRN-43 rule (3) for offline authorization. |
| **`AuthorizationMiddleware` in IAM** — Intercepts every request. Evaluates three dimensions: (1) role permission, (2) branch assignment, (3) residency level via `ResidencyLevelPolicy`. Rejects with 403 + audit event on failure. | Satisfies **SEC-01**, **SEC-02**, **CRN-15**, **US-003**. |
| **`ResidencyLevelPolicy` in IAM** — Encodes R1–R4 hierarchical rules from DB, cached in memory. R1/R2/R3 blocked from `controlled_med:prescribe`; R1/R2 require mandatory supervision. | Satisfies **US-050**, **US-051**, **SEC-01**. |
| **`PrescriptionCommandHandler` wired to authorization** — Route annotated with `prescription:create` + conditionally `controlled_med:prescribe`. Middleware enforces before handler executes. | Completes Iteration 2 deferred enforcement. Satisfies **US-051**, **SEC-01**. |
| **Data-driven `RolePermissionModel`** — Tables: `roles`, `permissions`, `role_permissions`. 11 system roles seeded. Validation prevents regulatory conflicts. Admin UI for configuration. | Satisfies **MNT-03**, **US-003**, **CRN-15**. |
| **`UserManagementService` in IAM** — User CRUD with role/branch assignment. Deactivation triggers `TokenDenyList` revocation. Medical staff: residencyLevel, supervisorStaffId (mandatory R1/R2). | Satisfies **US-001**, **CRN-15**. |
| **Security middleware pipeline** — Six ordered filters: TlsVerifier → AuthenticationFilter → AuthorizationFilter → TenantContextInjector → AuditInterceptor → ErrorSanitizer. | Satisfies **SEC-04**, **SEC-01**, **SEC-02**, **US-066**, **CRN-13**. |
| **PostgreSQL RLS on tenant-scoped tables** — `WHERE branch_id = current_setting('app.current_branch_id')`. `admin_reporting` role with BYPASSRLS for Reporting module. | Satisfies **SEC-02** (zero unauthorized cross-branch access). |
| **`ImmutableAuditStore` in Audit & Compliance** — SHA-256 hash-chained `audit_log` table. INSERT-only DB privileges. Periodic integrity verification job. | Satisfies **CRN-17**, **CRN-18**, **AUD-03**. |
| **`AuditEventReceiver`** — Dual ingestion: synchronous for security-critical events, asynchronous for high-volume access logging. | Satisfies **CRN-17**, **US-066**. |
| **`AuditQueryService`** — Four queries: GetAuditTrailForEntity, GetAuditTrailForUser, GetAccessLogForPatient, VerifyChainIntegrity. | Satisfies **CRN-17**, **US-066**, **CRN-32**. |
| **`LfpdpppComplianceTracker`** — Consent lifecycle via `consent_records`. ARCO workflows via `arco_requests` with legal deadlines. Rectification as corrective addendum events. | Satisfies **CRN-32**. |
| **PWA security components** — `LoginView`, `BranchSelectionView`, `SessionManager` (JWT in memory, auto-refresh), `RoleAwareRenderer`, `UserManagementView`, `RoleConfigurationView`. | Satisfies **US-001**, **US-002**, **US-003**, **MNT-03**. |
| **Cloud Database security schema** — Tables: users, roles, permissions, role_permissions, user_branch_assignments, medical_staff, refresh_tokens, token_deny_list, audit_log, consent_records, arco_requests. Indexes on audit queries. | Satisfies **CRN-15**, **CRN-17**, **CRN-18**, **CRN-32**. |

---

## Step 6: Views, Interfaces, and Design Decisions

### Diagrams Created / Updated

| Diagram | Section in Architecture.md | Description |
|---|---|---|
| Identity & Access Module Internals | Section 6.1.2 | Component diagram decomposing IAM into AuthenticationService, AuthorizationMiddleware, ResidencyLevelPolicy, RolePermissionModel, UserManagementService, TokenDenyList with responsibility table |
| Audit & Compliance Module Internals | Section 6.1.3 | Component diagram decomposing Audit into AuditEventReceiver, ImmutableAuditStore, AuditQueryService, LfpdpppComplianceTracker with responsibility table |
| Security Middleware Pipeline | Section 6.1.4 | Ordered filter chain diagram: TlsVerifier → AuthenticationFilter → AuthorizationFilter → TenantContextInjector → AuditInterceptor → ErrorSanitizer with responsibility table |
| PWA Security and Admin Components | Section 6.2.2 | Component diagram for LoginView, BranchSelectionView, SessionManager, RoleAwareRenderer, UserManagementView, RoleConfigurationView with responsibility table |
| SD-01: Authenticated API Request Flow (updated) | Section 7 | Replaced generic flow with concrete security middleware pipeline showing all six filters as participants |
| SD-02: Branch Context Selection (updated) | Section 7 | Enhanced with JWT claim embedding, refresh token issuance, deny-list check, and PostgreSQL RLS session variable activation |
| SD-07: Controlled Medication Prescription Blocked | Section 7 | R2 resident attempts controlled substance prescription; AuthorizationFilter delegates to ResidencyLevelPolicy; action blocked; audit event emitted |
| SD-08: Admin Creates New Role | Section 7 | Administrator creates Nutritionist role through RoleConfigurationView; regulatory validation; zero code changes (MNT-03) |
| SD-09: Patient Record Access with LFPDPPP Audit | Section 7 | Physician queries clinical timeline; AuditInterceptor logs access to hash-chained ImmutableAuditStore (US-066, CRN-32) |

### Interfaces Defined

#### Command Interfaces (Identity & Access)

| Command | Endpoint | Key Drivers |
|---|---|---|
| Login | `POST /auth/login` | US-002, SEC-04 |
| RefreshToken | `POST /auth/refresh` | US-002, SEC-04 |
| Logout | `POST /auth/logout` | US-002 |
| CreateUser | `POST /users` | US-001, CRN-15 |
| UpdateUser | `PUT /users/:userId` | US-001, CRN-15 |
| DeactivateUser | `POST /users/:userId/deactivate` | US-001, SEC-04 |
| CreateRole | `POST /roles` | MNT-03, US-003, CRN-15 |
| UpdateRolePermissions | `PUT /roles/:roleId/permissions` | MNT-03, CRN-15 |

#### Query Interfaces (Identity & Access)

| Query | Endpoint | Key Drivers |
|---|---|---|
| ListUsers | `GET /users` | US-001, CRN-15 |
| GetUser | `GET /users/:userId` | US-001 |
| ListRoles | `GET /roles` | MNT-03, US-003 |
| ListPermissions | `GET /permissions` | MNT-03, CRN-15 |

#### Query Interfaces (Audit & Compliance)

| Query | Endpoint | Key Drivers |
|---|---|---|
| GetAuditTrailForEntity | `GET /audit/entity/:entityType/:entityId` | CRN-17 |
| GetAuditTrailForUser | `GET /audit/user/:userId` | CRN-17, US-066 |
| GetAccessLogForPatient | `GET /audit/patient/:patientId/access` | CRN-32, US-066 |
| VerifyChainIntegrity | `GET /audit/verify` | CRN-18 |

### Design Decisions

| Driver | Decision | Rationale | Discarded Alternatives |
|---|---|---|---|
| **CRN-15, US-003** | Three-dimensional RBAC: role permissions + branch scoping + residency-level restrictions. 11 initial roles seeded; custom roles via admin UI | Captures all access control requirements in a single coherent model; branch scoping enforces SEC-02; residency dimension is explicit and centrally maintained | ABAC — higher complexity; ACL per resource — impractical at scale |
| **US-002, SEC-04** | Stateless JWT with embedded claims (15-min TTL) + refresh tokens (7-day TTL) + TokenDenyList for immediate revocation | Embedded claims enable offline authorization (CRN-43 rule 3); short TTL limits exposure; deny-list closes revocation gap | Server-side sessions — incompatible with offline; OAuth2 with external IdP — external dependency, no offline introspection |
| **SEC-01, SEC-04, CRN-13, US-066** | Six-filter security middleware pipeline: TlsVerifier → AuthenticationFilter → AuthorizationFilter → TenantContextInjector → AuditInterceptor → ErrorSanitizer | Single enforcement point; ordering guarantees unauthenticated requests rejected first; all access audited; no internal details leak | Per-endpoint annotations — scattered; Dedicated API gateway — excessive for monolith |
| **SEC-02** | PostgreSQL RLS as defense-in-depth below application-level filtering. `admin_reporting` role with BYPASSRLS for cross-branch reports | Two-layer enforcement for High/High scenario; RLS prevents data leakage even with application bugs | Application-level WHERE only — single layer, insufficient for SEC-02 |
| **CRN-17, CRN-18, AUD-03** | SHA-256 hash-chained append-only audit log. INSERT-only DB privileges. Periodic integrity verification job | Tamper-evidence detectable by any verifier; DB-level restriction satisfies CRN-18; active detection via verification job | Simple append-only without chaining — DBA tampering undetectable; Blockchain — extreme overhead |
| **MNT-03** | Data-driven role/permission model in DB tables. Admin UI for role creation and permission assignment. Regulatory conflict validation | <30 min for new roles, zero code changes; decoupled from releases; auditable | Hard-coded roles — violates MNT-03; Config-file roles — requires deployment |
| **US-050, US-051, SEC-01** | `ResidencyLevelPolicy` as first-class component with hierarchical R1–R4 rules. R1/R2/R3 blocked from controlled substance prescribing. Rules in DB, cached, evaluated by middleware | Explicit, centrally maintained; testable in isolation; travels in JWT for offline | Generic RBAC per action per level — permission explosion; Hard-coded checks — scattered |
| **CRN-32** | LFPDPPP compliance as cross-cutting concern: consent lifecycle, ARCO workflows with legal deadlines, corrective addendum for Rectification on immutable records | Architecturally explicit; enforceable at middleware; reconciles LFPDPPP with NOM-004 immutability | Consent as UI checkbox — no enforcement; Post-hoc retrofit — expensive |
| **CRN-13, SEC-04** | Error sanitization as terminal middleware filter returning `{ code, message, correlationId }` | Prevents leakage in all error paths; standardized format; correlation ID for debugging | Verbose errors — information leakage; Per-handler formatting — inconsistent |

---

## Step 7: Analysis of Design and Iteration Goal Achievement

| Driver | Analysis Result |
|---|---|
| **US-003** — Role-based permissions | **Satisfied.** Three-dimensional RBAC via `AuthorizationMiddleware`. 11 initial roles seeded; custom roles via admin UI. SD-01 shows full enforcement flow. |
| **US-001** — Create user accounts with role-based permissions | **Satisfied.** `UserManagementService` handles user CRUD. Interfaces defined in Section 8.3. `UserManagementView` in PWA provides admin UI. |
| **US-002** — Secure login with credentials | **Satisfied.** `AuthenticationService` issues JWT with embedded claims + refresh tokens. SD-02 shows full authentication flow. `LoginView` and `SessionManager` in PWA. |
| **US-050** — Validate residents perform only level-allowed actions | **Satisfied.** `ResidencyLevelPolicy` encodes R1–R4 rules. `AuthorizationMiddleware` delegates for `requiresResidencyCheck` permissions. |
| **US-051** — Block R1/R2/R3 from prescribing controlled medications | **Satisfied.** `ResidencyLevelPolicy` blocks R1/R2/R3 from `controlled_med:prescribe`. SD-07 illustrates the complete enforcement and audit flow. |
| **US-066** — Audit log entries for record access | **Satisfied.** `AuditInterceptor` captures every API request. `GetAccessLogForPatient` enables LFPDPPP retrieval. SD-09 illustrates the flow. |
| **SEC-01** — 100% of restricted actions blocked and logged | **Satisfied.** Middleware pipeline guarantees every request passes authorization. Denials logged synchronously. SD-07 demonstrates blocking. |
| **SEC-02** — Zero unauthorized cross-branch access | **Satisfied.** Two-layer defense: `AuthorizationFilter` + PostgreSQL RLS. Even application bugs cannot leak cross-branch data. |
| **SEC-04** — 100% of unauthenticated requests rejected | **Satisfied.** `AuthenticationFilter` applied before any business logic. `TokenDenyList` for immediate revocation. `ErrorSanitizer` prevents information leakage. |
| **MNT-03** — New roles operational in <30 min, zero code changes | **Satisfied.** Data-driven model + `RoleConfigurationView`. SD-08 demonstrates complete role creation flow. |
| **CRN-15** — RBAC for 11 roles with branch-scoped permissions | **Satisfied.** 11 system roles seeded with `is_system_role` protection. Branch and residency dimensions supported. |
| **CRN-13** — API hardening | **Satisfied.** `TlsVerifier` + `ErrorSanitizer` enforce HTTPS and strip internals from all responses. |
| **CRN-17** — Centralized audit log | **Satisfied.** Full audit module: dual ingestion, hash-chained storage, four query interfaces for Iterations 4–7. |
| **CRN-18** — Audit log tamper-proof even for DBAs | **Satisfied.** SHA-256 hash chaining + INSERT-only DB privileges + periodic integrity verification + on-demand VerifyChainIntegrity endpoint. |
| **CRN-32** — LFPDPPP personal data protection | **Satisfied.** `LfpdpppComplianceTracker` with consent lifecycle, ARCO workflows, legal deadlines, and corrective addendum pattern for Rectification. |

### Summary

| Status | Count | Drivers |
|---|---|---|
| **Satisfied** | 15 | US-003, US-001, US-002, US-050, US-051, US-066, SEC-01, SEC-02, SEC-04, MNT-03, CRN-15, CRN-13, CRN-17, CRN-18, CRN-32 |
| **Partially Satisfied** | 0 | — |
| **Not Satisfied** | 0 | — |

All 15 drivers for Iteration 3 have been satisfied. The security architecture — including the six-filter middleware pipeline, three-dimensional RBAC with residency-level policy, JWT with embedded claims, PostgreSQL RLS defense-in-depth, cryptographic hash-chained immutable audit trail, data-driven role configuration, and LFPDPPP compliance infrastructure — is now in place. Downstream iterations can safely depend on the security model for pharmacy dispensation with controlled substance enforcement (Iteration 5), inventory access control (Iteration 4), and offline-specific regulatory compensation (Iterations 5–6).
