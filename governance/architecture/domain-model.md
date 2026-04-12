---
type: architecture_domain_model
project: "SICEB"
status: active
bounded_contexts:
  - name: clinical_care
    modules: [domain.clinicalcare, domain.prescriptions, domain.laboratory]
  - name: pharmacy_and_dispensation
    modules: [domain.pharmacy]
  - name: inventory_and_supply
    modules: [domain.inventory, domain.supplychain]
  - name: finance
    modules: [domain.billing, domain.reporting]
  - name: personnel_and_training
    modules: [domain.training]
  - name: scheduling
    modules: [domain.scheduling]
shared_kernel:
  package: com.siceb.shared
  types: [EntityId, Money, UtcDateTime, IdempotencyKey, ErrorCode]
---

# Domain Model: SICEB

---

> **Modelo de dominio completo con diagramas Mermaid (23 entidades + relaciones):** [`docs/ADD/Design/Architecture.md#arch-04-domain`](../../docs/ADD/Design/Architecture.md)
> **Refinamiento Clinical Care — event-sourced append-only (Iteration 2):** [`docs/ADD/Design/Architecture.md#arch-04-clinical-care`](../../docs/ADD/Design/Architecture.md)
> **Descripciones detalladas de cada entidad con todos sus drivers:** [`docs/ADD/Design/Architecture.md#arch-04-elements`](../../docs/ADD/Design/Architecture.md)
> **Módulo de allocación completa (18 épicas → módulos):** [`docs/ADD/Design/Iteration1.md`](../../docs/ADD/Design/Iteration1.md)
> Este documento es un resumen navegable. Los diagramas y el razonamiento completo viven en `docs/ADD/`.
> Para reconciliar drift ejecuta `/rai-docs-update`.

---

## Shared Kernel (`com.siceb.shared`)

Leaf dependency — imported by all layers, depends on nothing else in `com.siceb`.

| Type | Description | Key Drivers |
|------|-------------|-------------|
| `EntityId` | UUID v7 — all entity PKs use this type; **no auto-increment sequences permitted anywhere** | CRN-38, CRN-43 rule 1 |
| `Money` | Fixed-precision `DECIMAL(19,4)` with MXN currency, banker's rounding — no `float`/`double` | CRN-42 |
| `UtcDateTime` | UTC-normalized instant — all timestamps stored in UTC, converted to `America/Mexico_City` at UI only | CRN-41 |
| `IdempotencyKey` | Client-generated key enabling safe command replay and duplicate-free sync | CRN-43 rule 2 |
| `ErrorCode` / `ErrorResponse` | Canonical API error shape — no internal stack traces or sensitive data exposed | CRN-13 |

## Bounded Contexts

### Clinical Care
**Modules:** `domain.clinicalcare`, `domain.prescriptions`, `domain.laboratory`
**Status:** Active (Phase 2 — ADD Iteration 2)
**Business objective:** Gestión de Clientes — centralized digital records with complete care history

Core business value stream. Uses an **event-sourced append-only pattern**:
- Write model: `ClinicalEvent` stream (eventType, payload) attached to `MedicalRecord`
- Read models (projections): `PatientSearchReadModel`, `ClinicalTimelineReadModel`, `Nom004RecordView`
- No UPDATE or DELETE on any clinical table — enforced at DB level, not only application level

**Entities:**

| Entity | Description | Key Drivers |
|--------|-------------|-------------|
| `Patient` | Person receiving care at any branch. Classified as Student (30% discount), Worker (20% discount), External, or Minor (<18 requires guardian). System-wide UUID — one patient = one record across all branches. Consent status embedded for LFPDPPP compliance. | US-019, US-020, US-022, US-023, CRN-37, CRN-32, PER-03 |
| `MedicalRecord` | Append-only clinical record. Insert-only at DB level per NOM-004-SSA3-2012. Permanent retention. Contains the `ClinicalEvent` stream. | US-024, US-026, US-027, CRN-02, CRN-01, CRN-31, AUD-03 |
| `ClinicalEvent` | Immutable event in the medical record stream (consultation, prescription, lab order, result, attachment). EventType + payload + `UtcDateTime`. The write model — read models are projections. | US-025, US-026, CRN-02 |
| `Consultation` | Single clinical encounter at a specific branch. Records diagnosis, notes, vital signs. R1/R2 consultations carry a `requiresSupervision` flag. Must be registerable offline. Origin point for prescriptions, lab orders, supply usage. | US-025, US-030, US-052, US-076, USA-01, CRN-16 |
| `Prescription` | Medication order generated during a consultation. Validated against prescriber's residency level before creation — R1/R2/R3 cannot prescribe controlled substances. | US-031, US-033, US-050, US-051, SEC-01, PER-04 |
| `PrescriptionItem` | (Value Object) Line item in a prescription: medication reference, quantity, dosage, instructions. | US-031, PER-04 |
| `LaboratoryStudy` | Lab test ordered during consultation. Full lifecycle: order → prepayment → processing → text result entry → visible in medical record. No DICOM/PACS per CON-05. | US-038–042, CRN-27 |
| `Attachment` | PDF, image, or scanned document attached to a medical record. Used for external documents and NOM-024 signed informed consents. | US-029, US-056, US-057 |

**Prescriptions bounded context note:** Separated from `clinicalcare` to isolate the `ResidencyLevelPolicy` — the validation rule that determines which resident level can prescribe which class of medication.

### Pharmacy and Dispensation
**Module:** `domain.pharmacy`
**Status:** Stub (ADD Iteration 5)
**Business objective:** Gestión Financiera + COFEPRIS regulatory compliance

| Entity | Description | Key Drivers |
|--------|-------------|-------------|
| `Medication` | Drug in the medication catalog. `isControlled` flag triggers COFEPRIS traceability requirements and restricts residency-level prescribing. Stock verified before dispensation. | US-034, US-035, US-051, SEC-03, CRN-33, CRN-14 |
| `Dispensation` | Pharmacy fulfillment event for a prescription. Records **8 mandatory COFEPRIS fields**: prescriber, dispenser, patient, medication, lot number, quantity, date, time. Written to the immutable audit log. Medications charged separately from consultation. | US-035–037, SEC-03, AUD-02, CRN-33 |

**Offline risk (CRN-14):** Dispensing controlled medications offline cannot verify real-time stock or confirm prescriber permissions. An **asynchronous business compensation protocol (CRN-45)** handles post-sync regulatory violations — generating mandatory supervisor review tasks and audit entries rather than silent HTTP errors.

### Inventory and Supply Chain
**Modules:** `domain.inventory`, `domain.supplychain`
**Status:** Stub (ADD Iteration 4)
**Business objective:** Gestión de Inventario — rigorous control of medical supplies, materials, and medications

| Entity | Description | Key Drivers |
|--------|-------------|-------------|
| `InventoryItem` | Stock record for a medication or medical supply at a specific branch. Includes minimum threshold (low-stock alert), expiration date (waste prevention). Updates propagate across all views in <2 seconds via WebSocket. Mutations are **delta commands** — never absolute state transfers. | US-004, US-005, US-006, US-034, PER-01, CRN-35, CRN-44 |
| `MedicalSupply` | Non-medication supply item (gauze, syringes, surgical gloves). Referenced by supply requests and tracked in branch inventory. | US-006, US-009, US-052, USA-04 |
| `SupplyRequest` | Formal supply request by a Service Manager. Full lifecycle: request (with justification) → approval/rejection by Administrator → delivery recording → receipt confirmation → automatic inventory update. | US-009, US-011, US-013, USA-04, AUD-01 |
| `SupplyRequestItem` | (Value Object) Line item in a supply request: supply reference + requested quantity. Supports catalog auto-complete and consumption-history-based suggested quantities. | US-009, USA-04 |
| `Workshop` | Training activity for residents. Request-approval workflow, attendance tracking. Exposed to academic systems via interoperability API. | US-015–018, IOP-01 |

**Delta command pattern (CRN-44):** All inventory mutations are `DecrementStock(item, qty, branch, timestamp)` / `IncrementStock(...)` commands — not `SetStock(item, 13)`. Current stock is a materialized view derived from the ordered delta sequence. This enables deterministic conflict resolution when two branches use the same supply offline and sync later.

### Finance
**Modules:** `domain.billing`, `domain.reporting`
**Status:** Stub (ADD Iteration 5 billing, Iteration 7 reporting)

| Entity | Description | Key Drivers |
|--------|-------------|-------------|
| `Payment` | Financial transaction for consultations, pharmacy, or lab studies. Fixed-precision arithmetic (`DECIMAL(19,4)`). Patient-type discounts applied automatically. Supports simple receipt + CFDI electronic invoice (SAT integration). Must be registerable offline. | US-044–048, PER-02, CRN-42, CRN-08 |
| `ServiceTariff` | Price configuration for a medical service: base price + effective date. Drives automatic charge calculation with patient-type discount. | US-064, CRN-42 |

### Personnel and Training
**Module:** `domain.training` (residency restrictions in `platform.iam`)
**Status:** training stub (Iteration 7); IAM active (Iteration 3)

| Entity | Description | Key Drivers |
|--------|-------------|-------------|
| `MedicalStaff` | Specialization of `User`. Carries residency level (Attending / R1–R4) and `canPrescribeControlled` flag. R1/R2 require a mandatory assigned supervisor reference. | US-049–051, US-065, SEC-01, CRN-16, ESC-03 |

**Residency restrictions enforced at service layer** — not only at UI. R1 cannot prescribe controlled medications under any circumstances (system blocks, logs attempt in audit trail).

### Scheduling
**Module:** `domain.scheduling`
**Status:** Stub (future iteration)

| Entity | Description | Key Drivers |
|--------|-------------|-------------|
| `Appointment` | Scheduled visit: date, time, physician, consultation type, status, cancellation reason. Physicians can view their daily agenda. | US-053–055 |

## Platform Modules (cross-cutting)

| Module | Core Entities | Responsibility |
|--------|--------------|----------------|
| `platform.iam` | `User`, `Role`, `Permission`, `MedicalStaff` | Authentication (JWT/HttpOnly cookies), RBAC for 11 roles with branch-scoped permissions, `ResidencyLevelPolicy` enforcement, token deny-list. New roles operational without code changes (MNT-03). `Permission.requiresResidencyCheck` flag delegates to the policy. Business validations execute against JWT-embedded user context — works identically online and offline (CRN-43 rule 3). |
| `platform.branch` | `Branch` | Branch CRUD, active-branch context selection, `branch_id` injection into all queries via `TenantConnectionInterceptor`. Context switch without logout in <3 seconds (ESC-03). |
| `platform.audit` | `AuditLogEntry` | Immutable, **hash-chained** audit log. Each entry includes `SHA-256(previousHash + payload)` creating a tamper-evident chain. Application DB role has INSERT-only privilege on audit tables — UPDATE, DELETE, TRUNCATE revoked even for DBAs. Logs: record access (LFPDPPP), restricted action attempts (SEC-01), controlled substance dispensations (COFEPRIS), every security-relevant event. |
| `platform.sync` | *(sync queue, conflict protocol)* | Offline sync queue manager, delta-command ordering, conflict resolution, idempotency enforcement (deduplication by `IdempotencyKey`). Detailed design: ADD Iteration 6. |
| `platform.consent` | `ConsentRecord`, `ArcoRequest` | NOM-024 informed consent lifecycle (granted, revoked, purpose). LFPDPPP ARCO rights: Access, Rectification, Cancellation, Opposition. 20-business-day legal deadline tracked per request. **Rectification on immutable records** is handled by appending a corrective addendum `ClinicalEvent` — never modifying the original record (preserves CRN-02). Consent status embedded in JWT for offline-compatible verification. |

## Key Invariants

| Invariant | Enforcement |
|-----------|-------------|
| One patient → one medical record (no duplicates across branches) | `patientId` unique constraint; UUID-based global identity (CRN-37) |
| Medical record entries are immutable | Insert-only schema; `DependencyArchTest` prevents domain code from using UPDATE/DELETE on clinical tables |
| Controlled medications blocked for R1/R2/R3 | `ResidencyLevelPolicy` in `platform.iam` + `Permission.requiresResidencyCheck` flag; `SEC-01` test |
| Inventory mutations are delta commands | Code review + CRN-44 guardrail |
| All timestamps UTC | `UtcDateTime` value object enforces UTC at construction; `CRN-41` |
| All monetary values fixed-precision | `Money` value object; no `float`/`double` on financial fields; `CRN-42` |
| Audit log tamper-proof | SHA-256 hash chain; DB role INSERT-only; `CRN-18` |
