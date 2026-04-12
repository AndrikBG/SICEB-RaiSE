---
type: architecture_design
project: "SICEB"
status: active
layers:
  - name: shared_kernel
    modules: [com.siceb.shared]
  - name: platform
    modules: [com.siceb.platform.iam, com.siceb.platform.branch, com.siceb.platform.audit, com.siceb.platform.sync, com.siceb.platform.consent]
  - name: domain
    modules: [com.siceb.domain.clinicalcare, com.siceb.domain.prescriptions, com.siceb.domain.laboratory, com.siceb.domain.pharmacy, com.siceb.domain.inventory, com.siceb.domain.supplychain, com.siceb.domain.scheduling, com.siceb.domain.billing, com.siceb.domain.reporting, com.siceb.domain.training]
  - name: api
    modules: [com.siceb.api]
  - name: frontend
    modules: [pwa-client]
---

# System Design: SICEB

---

> **Documento arquitectónico completo (C4 Context + Container + Component + Sequence + Interfaces):** [`docs/ADD/Design/Architecture.md`](../../docs/ADD/Design/Architecture.md)
> **Diagramas de secuencia SD-01 a SD-13:** [`docs/ADD/Design/Architecture.md#arch-07-seq`](../../docs/ADD/Design/Architecture.md)
> **Interfaces de comando y consulta (8.1 a 8.11):** [`docs/ADD/Design/Architecture.md#arch-08-interfaces`](../../docs/ADD/Design/Architecture.md)
> **Decisiones de diseño con alternativas descartadas por iteración:** [`docs/ADD/Design/Architecture.md#arch-09-decisions`](../../docs/ADD/Design/Architecture.md)
> **Convenciones offline-aware (Iteration 1 completa):** [`docs/ADD/Design/Iteration1.md`](../../docs/ADD/Design/Iteration1.md)
> Este documento es un mapa navegable. El razonamiento completo y los diagramas viven en `docs/ADD/`.
> Para reconciliar drift ejecuta `/rai-docs-update`.

---

## Architecture Overview

SICEB is a **three-tier web application** structured as a **modular monolith** on the backend — a single deployable Spring Boot artifact internally organized into bounded-context domain modules + cross-cutting platform modules + a shared kernel. Module dependency rules are a DAG enforced at build time by ArchUnit (`mvn verify`).

**Why modular monolith over microservices:** Simple deployment and operations for a small team; easier cross-module transactions; module boundaries can evolve toward microservices later; single-process debugging. Microservices would add distributed transaction overhead and operational complexity disproportionate to team size.

**Dependency order (leaf → top):** `shared` → `platform` → `domain` → `api`

No layer may depend on anything higher in this hierarchy. Circular dependencies between any top-level slices fail `mvn verify`.

## Containers

| Container | Technology | Responsibility | Key Drivers |
|-----------|-----------|----------------|-------------|
| **SICEB PWA Client** | React 19 / TypeScript / Vite / Tailwind / Shadcn-ui | SPA with Web App Manifest + Service Worker + IndexedDB (Dexie.js). Offline-capable clinical registration, prescriptions, and payment recording. Targets last 2 versions of Chrome, Edge, Safari, Firefox on desktop/tablet. | CON-01, CON-03, CRN-21 |
| **SICEB API Server** | Spring Boot 3.5 / Java 21 | Modular monolith REST API over HTTPS. WebSocket channel for real-time inventory and notification push. OpenAPI documented at `/docs`. Single deployable on cloud PaaS. | CON-02, CON-04, CRN-25 |
| **SICEB Cloud Database** | PostgreSQL 17 + Flyway | Canonical data store. All tenant-scoped tables include `branch_id`. Row-Level Security for branch isolation. Append-only clinical tables. Flyway versioned migrations only — Hibernate DDL is `validate`. | CRN-29, CRN-02, CRN-04 |
| **SICEB Local Storage** | IndexedDB (Dexie.js) in each PWA client | Branch-scoped subset of cloud data mirrored locally. Managed by Service Worker + sync queue manager. Enables offline operation for consultations, prescriptions, and payments. | CRN-21, CRN-36 |

## Backend Component Map

### Shared Kernel (`com.siceb.shared`)

| Component | Responsibility |
|-----------|---------------|
| `EntityId` | UUID v7 — all entity PKs; no auto-increment sequences permitted anywhere |
| `Money` | `DECIMAL(19,4)`, MXN, banker's rounding; no float/double on financial fields |
| `UtcDateTime` | UTC-normalized instant; all storage in UTC, UI converts to `America/Mexico_City` |
| `IdempotencyKey` | Client-generated key for idempotent write operations and sync deduplication |
| `ErrorCode` / `ErrorResponse` | Canonical error shape; no stack traces or sensitive data exposed to clients |

### Platform Modules (`com.siceb.platform`)

May use `shared`; must not depend on `domain` or `api`.

| Module | Responsibility |
|--------|---------------|
| `platform.iam` | JWT HttpOnly cookie auth; RBAC for 11 roles with branch-scoped permissions; `ResidencyLevelPolicy` (R1–R4 controlled medication restrictions); token deny-list; configurable roles without code changes |
| `platform.branch` | Branch CRUD; active-branch context selection; `TenantConnectionInterceptor` injects `branch_id` into every query; context switch without logout (<3 sec per ESC-03) |
| `platform.audit` | Immutable hash-chained audit log: `SHA-256(previousHash + payload)` per entry; application DB role has INSERT-only on audit tables (UPDATE/DELETE/TRUNCATE revoked even for DBAs); logs record access, restricted action attempts, controlled substance dispensations |
| `platform.sync` | Offline sync queue manager; delta-command ordering; conflict detection; idempotency enforcement by `IdempotencyKey`; partial failure recovery (Iteration 6 detail) |
| `platform.consent` | NOM-024 informed consent lifecycle; LFPDPPP ARCO rights (20-business-day legal deadline); ARCO rectification handled by appending a corrective `ClinicalEvent` — never modifying the original record |

### Domain Modules (`com.siceb.domain`)

10 bounded-context modules activated incrementally (IC-01). Unactivated modules contain only `package-info.java`.

**Currently activated (Phase 2):** `clinicalcare`, `prescriptions`, `laboratory`

| Module | Status | Responsibility | Domain Dependencies |
|--------|--------|---------------|---------------------|
| `clinicalcare` | **Active** | Patients, append-only medical records (`ClinicalEvent` stream), consultations, attachments, read-model projections (`PatientSearchReadModel`, `ClinicalTimelineReadModel`, `Nom004RecordView`) | — |
| `prescriptions` | **Active** | Prescription creation, `PrescriptionItem` line items, `ResidencyLevelPolicy` delegation to `platform.iam`, prescription lifecycle | `clinicalcare` |
| `laboratory` | **Active** | Study orders, prepayment verification, text-only result entry, reagent inventory | `clinicalcare` |
| `pharmacy` | Stub | Medication catalog, dispensation with 8-field COFEPRIS audit entry, controlled substance traceability, offline compensation protocol | `prescriptions`, `inventory` |
| `inventory` | Stub | Branch-scoped stock tracking, delta-command mutations, low-stock/expiration/temperature alerts, WebSocket propagation | — |
| `supplychain` | Stub | Supply request/approval/delivery/confirmation workflow, workshop request/approval | `inventory` |
| `scheduling` | Stub | Appointments, physician agendas, cancellations/rescheduling | `clinicalcare` |
| `billing` | Stub | Payments (`DECIMAL(19,4)`), tariffs, receipts, CFDI (SAT) integration, patient-type discounts | `clinicalcare`, `pharmacy`, `laboratory` |
| `reporting` | Stub | Financial reports (income, expense, profitability), supply history, patient activity — read-only projections | `billing`, `inventory`, `clinicalcare` |
| `training` | Stub | Workshop participation registration (not academic evaluations per CON-09), attendance tracking, academic API exposure | — |

### API Layer (`com.siceb.api`)

REST controllers. Depends on all lower layers; nothing depends on `api`.

| Controller | Responsibility |
|-----------|---------------|
| `AuthController` | Login, logout, token refresh; sets HttpOnly JWT cookie |
| `UserController` | User CRUD, role and branch assignment |
| `ClinicalController` | Patient registration, medical record, consultation endpoints |
| `ConsentController` | ARCO rights request endpoints (LFPDPPP) |
| `LabStudyController` | Lab study order and result entry endpoints |
| `RoleController` | Role and permission configuration (admin-only) |
| `AuditController` | Audit log query for compliance reporting |
| `SystemController` | Health (`/actuator/health`), metrics, readiness/liveness probes |
| `GlobalExceptionHandler` | Maps domain exceptions to `ErrorResponse` — no internal details exposed (CRN-13) |

### Spring Configuration (`com.siceb.config`)

Nothing depends on `config`.

| Component | Responsibility |
|-----------|---------------|
| `SecurityConfig` | JWT filter chain, whitelisted public routes (`/login`, `/actuator/health`), HTTPS enforcement |
| `MultiTenantConfig` | Wires `TenantConnectionInterceptor` into Hibernate session management |
| `WebMvcConfig` | CORS origins (`CORS_ORIGINS` env var), path matchers |
| `WebSocketConfig` | Real-time channels for inventory updates and notifications |
| `OpenApiConfig` | Swagger UI at `/docs`, API-docs at `/api-docs` |
| `AsyncConfig` | Thread pool for background sync operations |

## Frontend Component Map

| Component | Responsibility |
|-----------|---------------|
| `features/auth` | Login, branch selection, password change; owns `auth-store.ts` (Zustand) |
| `features/clinical` | Patient search, medical record timeline view, consultation wizard, pending lab studies, lab result entry |
| `features/admin` | User management, role/permission configuration |
| `components/ProtectedRoute` | Redirects unauthenticated requests to `/login` |
| `components/RoleAwareRenderer` | Conditionally renders UI elements by permission key (e.g., `controlled_med:prescribe`) |
| `components/Layout` | App shell: navigation, active-branch indicator, sync-status indicator (CRN-39) |
| `stores/auth-store.ts` | Auth state, active branch context, JWT presence |
| `stores/clinical-store.ts` | Patient and consultation state |
| `stores/sync-store.ts` | Online/offline mode, pending sync queue depth |
| `stores/ui-store.ts` | Global UI state (modals, toasts) |
| `lib/db.ts` | Dexie (IndexedDB) schema — offline tables for patients, consultations, sync queue |
| `lib/ws-client.ts` | WebSocket connection for real-time inventory and notification push |
| `lib/auth-api.ts` / `lib/clinical-api.ts` | Typed API clients; route through Service Worker when offline |
| `hooks/use-online-status.ts` | Reactive online/offline detection |
| `hooks/use-pwa.ts` | Service Worker update prompt |

## Key Architectural Decisions

| Decision | Choice | Discarded Alternatives | Drivers |
|----------|--------|----------------------|---------|
| Multi-tenancy model | Shared DB with `branch_id` discriminator + PostgreSQL RLS | DB-per-tenant (high cost), schema-per-tenant (complex migrations) | CRN-29 |
| Medical record mutability | Insert-only event stream (`ClinicalEvent`) — no UPDATE/DELETE | Application-level soft-delete | NOM-004, CRN-02, AUD-03 |
| Inventory mutation model | Delta commands (`DecrementStock`, `IncrementStock`) | Absolute state transfer (`SetStock`) | CRN-44, CRN-35 |
| Audit log integrity | Hash-chained entries (`SHA-256`), INSERT-only DB role | Application-level log only | CRN-18, COFEPRIS |
| Auth transport | HttpOnly JWT cookies | `localStorage` JWT (XSS-exposed) | CRN-13, SEC-04 |
| Entity identifiers | UUID v7 (`EntityId`) — no auto-increment | DB sequences | CRN-38, CRN-43 rule 1 |
| Offline business validation | Validations execute against JWT + local cache (no live DB) | Online-only validation | CRN-43 rule 3, CRN-16 |
| Schema evolution | Flyway versioned migrations; Hibernate `ddl-auto: validate` | Hibernate auto-DDL | CRN-04 |
| Write operations | Idempotent by `IdempotencyKey` — safe to retry | Non-idempotent writes | CRN-43 rule 2, REL-02 |
| Architecture enforcement | ArchUnit tests in `mvn verify` CI gate | Manual code review only | CRN-27, CRN-43 |

## Sequence Flows (documented in `docs/ADD/Design/Architecture.md`)

| Flow | Key Behavior |
|------|-------------|
| SD-01: Authenticated API Request | JWT validation → `TenantConnectionInterceptor` injects `branch_id` → all queries scoped to active branch |
| SD-02: Branch Context Selection | Login → branch list → select → new JWT with `branch_id` claim → all subsequent queries filtered |
| SD-03: Create Patient and Medical Record | Patient registered → UUID assigned → `MedicalRecord` created → first `ClinicalEvent` emitted |
| SD-04: Add Consultation + Prescriptions + Lab Orders | Consultation → `ResidencyLevelPolicy` checked → `ClinicalEvent` persisted → prescriptions + lab orders created → audit entry written |
| SD-07: R1 Controlled Medication Blocked | Prescribe controlled medication → `ResidencyLevelPolicy` blocks → attempt logged in audit trail → `403 FORBIDDEN` |
| SD-11: Inventory Delta Command + WebSocket | `DecrementStock` command → delta applied → materialized view updated → WebSocket broadcast to all branch subscribers |
| SD-12: Branch Context Switch Without Logout | User selects new branch → `branch_id` in JWT updated → all stores refreshed → context switch in <3 seconds |
