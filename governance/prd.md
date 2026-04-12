# PRD: SICEB

> Product Requirements Document
> Drivers: 76 user stories · 27 quality attribute scenarios · 12 technical + business constraints · 45 architectural concerns

---

> **User stories completos (US-001 a US-076):** [`docs/Requirements/US/Table_US.md`](../docs/Requirements/US/Table_US.md)
> **Escenarios de calidad con estructura de 6 partes:** [`docs/Requirements/Quality_Attribute_Scenarios.md`](../docs/Requirements/Quality_Attribute_Scenarios.md)
> **Prioridad de atributos de calidad (High/High matrix):** [`docs/Requirements/Quality_Attributes_Priority.md`](../docs/Requirements/Quality_Attributes_Priority.md)
> **Preocupaciones arquitectónicas (CRN-01 a CRN-45):** [`docs/Requirements/Architecture_Concerns.md`](../docs/Requirements/Architecture_Concerns.md)
> **Restricciones (CON-01 a CON-12):** [`docs/Requirements/Constraints.md`](../docs/Requirements/Constraints.md)
> **Análisis de US críticos:** [`docs/Requirements/Critical_US_Analysis.md`](../docs/Requirements/Critical_US_Analysis.md)
> Este documento es un resumen operativo. Ante cualquier ambigüedad, las fuentes de verdad son los docs anteriores.
> Para reconciliar drift ejecuta `/rai-docs-update`.

---

---

## Problem

The Private Wellness Integrated Clinics Network operates with completely manual processes: supply requests via Word documents and email, paper medical records with no consolidation across services or branches, manual financial reporting, and no enforcement of resident training-level restrictions (R1–R4). This causes operational slowness, fragmented patient information, unmonitored inventory, slow financial reporting, and compliance gaps with NOM-004-SSA3-2012, LFPDPPP, and COFEPRIS.

Specific business needs triggering this system:
- NEC-03: Centralized digital patient record accessible from all medical services
- NEC-08: Role and permission-differentiated access control
- NEC-10: Resident restriction validation by level (R1–R4)
- NEC-11: Controlled medication traceability
- NEC-13: Multi-branch centralized management with consolidated reporting
- NEC-01/02: Material control with automated supply request traceability

## Goals

1. Every patient has a single consolidated, immutable electronic record accessible from any branch
2. Supply and workshop workflows are fully digitized with complete request/approval/delivery traceability
3. Role-based access and resident-level restrictions (R1–R4) are enforced automatically at the service layer
4. Financial reports and CFDI invoices generate automatically
5. The system operates offline and synchronizes with zero data loss and zero duplicates

---

## Requirements

### RF-01: Authentication and Multi-Branch Access Control
*(ADD Iteration 3 — EP-01, EP-14 · US-001, US-002, US-003, US-073, US-074)*

Users authenticate with email/password credentials and are assigned exactly one role (from 11 defined roles) and one or more authorized branches. Multi-branch users select their active branch context upon login — this selection becomes a security context that scopes all data views. Branch context switches without logout complete in under 3 seconds. Role and branch assignment is managed by the General Administrator without code changes. New roles become operational in under 30 minutes.

**11 Roles:** General Director, General Administrator, Service Manager, Attending Physician, Residents R1–R4, Lab Personnel, Pharmacy Personnel, Admin/Reception Staff.

**Measurables:** 100% of role-restricted actions blocked and logged (SEC-01); zero unauthorized cross-branch access (SEC-02); 100% of unauthenticated API requests rejected (SEC-04).

### RF-02: Electronic Medical Record — Immutable and NOM-004 Compliant
*(ADD Iteration 2 — EP-06 · US-024, US-025, US-026, US-027, US-029)*

Each patient has exactly one electronic medical record with a system-wide unique UUID — no duplicates across branches. The record uses an **event-sourced append-only pattern**: every clinical action (consultation, prescription, lab order, result, attachment) is persisted as an immutable `ClinicalEvent`. No modification or deletion of existing events is permitted at the database level. Mandatory sections comply with NOM-004-SSA3-2012. Permanent retention. File attachments (PDF, images) are supported. Read-model projections (`ClinicalTimelineReadModel`, `Nom004RecordView`) serve queries efficiently.

**LFPDPPP ARCO rights:** Rectification requests append a corrective `ClinicalEvent` — they never modify the original record.

**Measurables:** Patient search returns results in under 1 second over 50,000+ records (PER-03); 100% of modification attempts blocked and logged (AUD-03).

### RF-03: Patient Registration and Classification
*(ADD Iteration 2 — EP-05 · US-019, US-020, US-023)*

Reception staff and managers register patients with demographic information. Patients classified as:
- **Student:** 30% discount applied automatically
- **Worker:** 20% discount applied automatically
- **External:** no discount
- **Minor (<18):** guardian name and relationship mandatory

One patient = one system-wide UUID regardless of which branch first registered them (CRN-37).

### RF-04: Clinical Consultation, Prescriptions, and Residency-Level Enforcement
*(ADD Iteration 2 — EP-06, EP-11 · US-025, US-030, US-031, US-049, US-050, US-051, US-065)*

Physicians and residents add consultations (diagnosis, notes, vital signs) to the patient's medical record. The system enforces residency level restrictions at the service layer via `ResidencyLevelPolicy`:

| Residency Level | Can prescribe controlled medications |
|----------------|--------------------------------------|
| Attending Physician | Yes (no restrictions) |
| R4 | Yes |
| R3 | No |
| R2 | No |
| R1 | No — mandatory supervision required for all consultations |

Blocked actions generate an audit log entry and return a `403 FORBIDDEN` with a descriptive message. Supervisors are assigned to R1/R2 residents explicitly; offline consultations by R1/R2 must carry the cached supervisor reference.

Consultations must be registerable while offline (US-076).

### RF-05: Laboratory Management
*(ADD Iteration 2 — EP-08 · US-038, US-039, US-040, US-041, US-042)*

Physicians order lab studies during consultations. Lab personnel see all pending requests for their branch. Results entered in text format only (no DICOM/PACS per CON-05). Results automatically attach to the patient's medical record via a `ClinicalEvent`. Reagent inventory tracked with temperature and expiration alerts. Study lifecycle: order → prepayment verification → processing → result entry → record attachment.

### RF-06: Pharmacy Management and COFEPRIS Controlled Substance Traceability
*(ADD Iteration 5 — EP-07 · US-032, US-033, US-034, US-035, US-036, US-037)*

Pharmacy personnel validate prescriptions against the clinical record, verify stock, and dispense medications. Every controlled substance dispensation records **8 mandatory COFEPRIS traceability fields**: prescriber identity, dispenser identity, patient identity, medication name, lot number, quantity, date, and time — in the immutable audit log. Prescription validation (stock + prescriber permission check) executes in under 2 seconds (PER-04). COFEPRIS audit report covers 100% of controlled substance transactions and generates in under 30 seconds (AUD-02).

**Offline risk (CRN-14):** When a branch is offline, controlled medication dispensation carries regulatory risk. The **asynchronous compensation protocol (CRN-45)** handles post-sync violations: mandatory supervisor review task + audit entry + priority administrator alert. Silent failures are prohibited.

### RF-07: Supply and Workshop Request Workflows
*(ADD Iteration — EP-03, EP-04, EP-13 · US-009, US-011, US-013, US-015, US-016, US-017, US-018)*

Service Managers create digital supply requests with justification, quantity, and catalog auto-complete. The Administrator approves or rejects with reasons recorded in the system. The system sends automatic notifications at each workflow step. Delivery confirmation updates branch inventory automatically. Supply request creation completes in 3 steps in under 2 minutes (USA-04). Full request history is searchable and reportable. Supply approval traceability reports include 5 fields and generate in under 15 seconds (AUD-01). Workshop requests follow the same approval workflow; attendance tracked on completion.

### RF-08: Inventory Management with Branch Isolation and Delta Commands
*(ADD Iteration 4 — EP-02 · US-004, US-005, US-006, US-007)*

The General Administrator sees inventory across ALL services and branches. Service Managers see ONLY their service's inventory. All inventory mutations are recorded as **delta commands** (`DecrementStock`, `IncrementStock`) — never absolute state transfers. Current stock is a materialized view derived from the ordered delta sequence, enabling deterministic conflict resolution for concurrent offline edits (CRN-44). Inventory updates propagate across all branch views via WebSocket in under 2 seconds (PER-01). Automated alerts: low stock (below minimum threshold), expiration approaching, temperature exceedance.

### RF-09: Multi-Branch Administration and Network Scalability
*(ADD Iteration 4 — EP-18 · US-071, US-072, US-073, US-074, US-075)*

The General Administrator creates, updates, and deactivates branches. A new branch is fully operational in under 1 hour (ESC-01). Personnel assigned to multiple branches switch active branch context without logging out (under 3 seconds, ESC-03). The architecture supports growth from 3 to 15+ branches with less than 10% performance degradation (ESC-02 — High/High priority). The General Administrator can view a consolidated dashboard aggregating real-time data from all active branches.

### RF-10: Payment Registration, CFDI Invoicing, and Financial Reporting
*(ADD Iteration 5 billing · Iteration 7 reporting — EP-09, EP-10 · US-044, US-045, US-046, US-047, US-048)*

All consultations, medication dispensations, and lab studies generate payment records. Patient-type discounts (Student 30%, Worker 20%) applied automatically. Payments must be registerable while offline; CFDI generation is queued for reconnection (CRN-09). Consolidated income/expense/profitability reports covering all 10 branches generate in under 10 seconds (PER-02). All monetary values use fixed-precision arithmetic — no floating-point rounding errors (CRN-42).

### RF-11: Offline-First Operation and Synchronization
*(ADD Iteration 6 — EP-17 · US-076, REL-01, REL-02, USA-01)*

The highest-ranked architectural driver. Critical modules (consultations, prescriptions, payments) operate fully offline using IndexedDB local storage. The system transitions between online and offline modes transparently in under 3 seconds with a non-intrusive status indicator (USA-01 — High/High priority). Upon reconnection:
- All offline records synchronized with **zero data loss and zero duplicates** (REL-01 — High/High priority)
- Partial sync failures resume from the **exact cutoff point** without re-transmitting already-synced records (REL-02 — High/High priority)
- Checksum-based cache corruption detection triggers forced re-download (REL-04)
- Offline IDs (UUID v7) never collide with IDs from other branches (CRN-38)
- R1/R2 resident consultations offline carry cached supervisor reference for post-sync validation (CRN-16)

### RF-12: Audit Trail, Personal Data Compliance (LFPDPPP), and Informed Consent (NOM-024)
*(ADD Iteration 3 — EP-14 · US-062, US-063, US-066)*

Every access to patient records, every role-restricted action attempt, and every controlled substance transaction generates an **immutable, hash-chained audit log entry** (`SHA-256(previousHash + payload)`). The application DB role has INSERT-only privilege on audit tables — UPDATE, DELETE, and TRUNCATE are revoked even for DBAs (CRN-18). ARCO rights requests (Access, Rectification, Cancellation, Opposition) are tracked with a 20-business-day legal deadline. Informed consents are stored with purpose and revocation lifecycle per NOM-024-SSA3-2012. Patient consent status is embedded in the JWT for offline-compatible verification.

### RF-13: External API and Interoperability
*(ADD Iteration 7 — EP-16 · IOP-01, IOP-02)*

A versioned REST API (OpenAPI-documented at `/docs`) enables external systems to query resident training activity data. The academic integration API returns training records for a resident in under 5 seconds (IOP-01). New integration endpoints can be added without modifying existing ones (IOP-02). API versioning strategy prevents breaking changes to external consumers (CRN-11).
