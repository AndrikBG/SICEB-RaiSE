---
type: guardrails
version: "1.0.0"
---

# Guardrails: SICEB

> Code and architecture guardrails derived from `rai init --detect` conventions and ADD architectural decisions (Iterations 1–4).

---

> **Preocupaciones arquitectónicas completas (CRN-01 a CRN-45):** [`docs/Requirements/Architecture_Concerns.md`](../docs/Requirements/Architecture_Concerns.md)
> **Decisiones de diseño con alternativas descartadas:** [`docs/ADD/Design/Architecture.md#arch-09-decisions`](../docs/ADD/Design/Architecture.md)
> **Convenciones offline-aware (CRN-43 detalle completo):** [`docs/ADD/Design/Iteration1.md`](../docs/ADD/Design/Iteration1.md)
> Este documento son las reglas verificables y ejecutables. El razonamiento completo está en `docs/`.
> Para reconciliar drift ejecuta `/rai-docs-update`.

---

---

## Guardrails Activos

### Code Quality (detected conventions)

| ID | Level | Guardrail | Verification | Derived from |
|----|-------|-----------|--------------|--------------|
| GRD-01 | must- | All Java code passes `mvn checkstyle:check` before commit | `mvn checkstyle:check` exits 0 | Detected convention |
| GRD-02 | must- | All unit tests pass before any commit | `mvn test` exits 0 | Detected convention |
| GRD-03 | must- | Full build including ArchUnit architecture tests passes before any commit | `mvn verify` exits 0 | CRN-27, DependencyArchTest |
| GRD-04 | must- | Frontend code passes ESLint before commit | `pnpm lint` exits 0 | Detected convention |
| GRD-05 | should- | TypeScript strict mode — no `any` casts without explicit justification comment | Compiler clean | CON-01, codebase convention |

### Architecture — Dependency Rules (CRN-27, enforced by ArchUnit)

| ID | Level | Guardrail | Verification | Derived from |
|----|-------|-----------|--------------|--------------|
| GRD-10 | must- | `com.siceb.shared` must not depend on `domain`, `platform`, or `config` — it is the leaf | `DependencyArchTest` | CRN-27 |
| GRD-11 | must- | `com.siceb.platform` must not depend on `domain` or `api` | `DependencyArchTest` | CRN-27 |
| GRD-12 | must- | `com.siceb.domain` must not depend on `api` or `config` | `DependencyArchTest` | CRN-27 |
| GRD-13 | must- | All top-level slices must be free of circular dependencies | `DependencyArchTest` | CRN-27 |
| GRD-14 | must- | Cross-domain dependency direction: `clinicalcare` ← `prescriptions` ← `pharmacy` ← (`billing`, `inventory`). Reverse dependencies are violations. | Code review + ArchUnit | CRN-27 |

### Domain Module Activation (IC-01, enforced by ArchUnit)

| ID | Level | Guardrail | Verification | Derived from |
|----|-------|-----------|--------------|--------------|
| GRD-20 | must- | Unactivated domain modules contain only `package-info.java` — no classes, entities, or Spring components | `DomainStubsArchTest` | IC-01 |
| GRD-21 | must- | Activating a domain module requires: (1) adding it to `ACTIVATED_MODULES` in `DomainStubsArchTest`, (2) a story scope commit, (3) a plan before implementation | Code review | IC-01, Process Rules |
| GRD-22 | must- | Currently activated (Phase 2): `clinicalcare`, `prescriptions`, `laboratory` | `DomainStubsArchTest` | IC-01, Iteration 2 |

### Data Integrity and Compliance

| ID | Level | Guardrail | Verification | Derived from |
|----|-------|-----------|--------------|--------------|
| GRD-30 | must- | Medical record tables are insert-only — no UPDATE or DELETE on `medical_records`, `clinical_events`, or any append-only clinical table. Enforced at DB level, not only application level | DB schema + `AUD-03` test | NOM-004, CRN-02 |
| GRD-31 | must- | ARCO rectification appends a corrective `ClinicalEvent` — never modifies the original event | Code review | CRN-02, CRN-32 |
| GRD-32 | must- | All timestamps stored and transmitted in UTC via `UtcDateTime`. Conversion to `America/Mexico_City` happens only at the UI presentation layer | Code review | CRN-41 |
| GRD-33 | must- | All monetary values use `Money` shared kernel type (`DECIMAL(19,4)`, banker's rounding). No `float`, `double`, or `BigDecimal` literals on financial fields | Code review | CRN-42 |
| GRD-34 | must- | Controlled substance dispensations generate an 8-field COFEPRIS audit record: prescriber, dispenser, patient, medication, lot, quantity, date, time. Written to immutable audit log | `SEC-03` test | COFEPRIS, CRN-33 |
| GRD-35 | must- | All database schema changes go through Flyway versioned migrations. Hibernate `ddl-auto: validate` only — the ORM never creates or alters tables | `application.yml`, migration naming | CRN-04 |
| GRD-36 | must- | Row-Level Security enforces branch isolation — every query on tenant-scoped tables must carry `branch_id` context injected by `TenantConnectionInterceptor` | DB audit + `SEC-02` test | SEC-02, CRN-29 |
| GRD-37 | must- | Patient ARCO requests track a 20-business-day legal deadline. No ARCO request may be resolved without updating `resolvedAt` and `resolutionNotes` | Code review + `ArcoRequest` entity | CRN-32, LFPDPPP |

### Offline-Aware Design Conventions (CRN-43 — mandatory for all domain modules)

These four conventions were established in ADD Iteration 1 so that Iterations 2–5 produce sync-compatible code without requiring a data-layer retrofit in Iteration 6.

| ID | Level | Guardrail | Verification | Derived from |
|----|-------|-----------|--------------|--------------|
| GRD-40 | must- | **Rule 1 — UUID-only identifiers.** All entity primary keys use `EntityId` (UUID v7). No auto-increment sequences or DB-generated serial IDs anywhere in the codebase | ArchUnit (enforced in CI) | CRN-43, CRN-38 |
| GRD-41 | must- | **Rule 2 — Idempotent write operations.** Every command handler and write endpoint must be idempotent by `IdempotencyKey`. Re-submitting the same command produces the same result without side effects | Code review + integration test | CRN-43, REL-02 |
| GRD-42 | must- | **Rule 3 — Local-first business validations.** Business rule validations (role permissions, residency level, prescriber authorization) must execute against data available in the JWT and locally cached reference data — not live DB queries — so they work identically online and offline | Code review | CRN-43, CRN-16 |
| GRD-43 | must- | **Rule 4 — Delta-based inventory mutations.** All inventory state changes are recorded as intent-based delta commands (`DecrementStock`, `IncrementStock`). Absolute state transfers (`SetStock`) are prohibited. Current stock is a materialized view derived from the delta sequence | Code review | CRN-43, CRN-44, CRN-35 |

### Security

| ID | Level | Guardrail | Verification | Derived from |
|----|-------|-----------|--------------|--------------|
| GRD-50 | must- | JWT stored exclusively in HttpOnly cookies — never in `localStorage`, `sessionStorage`, or JavaScript-accessible variables | Code review + `SecurityConfig` | CRN-13, SEC-04 |
| GRD-51 | must- | All REST endpoints protected by Spring Security filter chain. Public routes (`/login`, `/actuator/health`) must be explicitly whitelisted in `SecurityConfig` | `SecurityConfig` review | SEC-04 |
| GRD-52 | must- | Resident level restrictions (R1–R4) enforced at the service/domain layer via `ResidencyLevelPolicy` — not only at the UI. A blocked action must also generate an audit log entry | `SEC-01` test | CRN-15 |
| GRD-53 | must- | Error responses must not expose internal details, stack traces, or database messages. `GlobalExceptionHandler` maps all exceptions to `ErrorResponse` with a stable `ErrorCode` | Code review + `ErrorResponseTest` | CRN-13 |
| GRD-54 | must- | The application DB role has INSERT-only privilege on audit log tables. UPDATE, DELETE, and TRUNCATE are revoked even for DBA accounts | DB role audit | CRN-18 |
| GRD-55 | must- | Audit log entries use SHA-256 hash chaining: `hash = SHA-256(previousHash + payload)`. Any entry without a valid chain link is a tamper indicator | Code review + hash chain test | CRN-18 |

### Offline Compensation (CRN-45)

| ID | Level | Guardrail | Verification | Derived from |
|----|-------|-----------|--------------|--------------|
| GRD-60 | must- | When an offline operation is rejected during sync due to a regulatory violation (e.g., controlled medication dispensed by a deactivated resident, or dispensed against stale inventory), the system must generate: (1) a mandatory supervisor review task, (2) an audit log entry, (3) a priority alert to the administrator. Silent HTTP errors on sync are prohibited | Code review + integration test | CRN-45, CRN-14 |
| GRD-61 | should- | CFDI invoices cannot be issued while offline (SAT requires real-time communication). When a payment is recorded offline, the CFDI generation must be queued and issued upon reconnection with a clear user-facing status indicator | Code review | CRN-09, CRN-39 |
