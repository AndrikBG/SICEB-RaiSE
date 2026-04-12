# Iteration 2 — Core Clinical Workflow and Medical Records

## Goal

Design the clinical care modules that represent the primary business value stream: patient management, medical consultations, prescribing, laboratory study tracking, and the immutable clinical record. Enforce medical record immutability, NOM-004-SSA3-2012 compliance, and a system-wide unique patient identifier.

The clinic's core revenue comes from patient consultations. The clinical record is the most regulated artifact (NOM-004-SSA3-2012) and the most architecturally constrained (append-only, permanent retention). Addressing this early ensures the data model is correct before downstream modules (pharmacy, laboratory, payments) depend on it. This iteration covers 4 of the 10 primary user stories (US-024, US-025, US-026, US-031).

**Business objective:** Gestión de Clientes — Maintain centralized digital records with complete care history.

---

## Step 2: Iteration Drivers

### Primary User Stories

| Driver | Description | Why this iteration |
|---|---|---|
| **US-024** (rank 8) | Create clinical record | Entry point for all patient care |
| **US-025** (rank 6) | Add consultation to record | The daily core clinical operation; supports REL-01, USA-01 |
| **US-026** (rank 5) | Record immutability | Must be enforced from the data model layer; supports REL-02 |
| **US-031** (rank 9) | Prescribe medications | Consultations generate prescriptions; supports USA-01 |

### Supporting User Stories

| Driver | Description | Why this iteration |
|---|---|---|
| **US-019** | Register new patients with demographic information | Foundational for creating medical records |
| **US-020** | Classify patients by type with automatic discount calculation | Patient type determines financial treatment |
| **US-023** | Validate guardian presence for minor patients | Regulatory requirement for patients under 17 |
| **US-027** | View comprehensive medical history across services | Core clinical read use case |
| **US-038** | Request laboratory studies during consultation | Lab orders originate from consultations |
| **US-040** | View pending laboratory study requests | Lab technician work queue |
| **US-041** | Enter laboratory study results in text format | Text-only per CON-05 |
| **US-042** | View laboratory results in the patient's medical record | Results become part of the immutable record |

### Quality Attribute Scenarios

| Driver | Description | Why this iteration |
|---|---|---|
| **PER-03** | Patient search under 1 second over 50,000+ records | Requires indexing strategy on clinical read models |
| **USA-02** | New resident onboarding — guided consultation flow | Supports daily clinical operations for R1–R4 residents |
| **AUD-03** | Medical record immutability — 100% of modification attempts blocked and logged | Core architectural constraint for clinical data |

### Architectural Concerns

| Driver | Description | Why this iteration |
|---|---|---|
| **CRN-02** | Immutable data model for clinical records | Insert-only clinical event schema must be designed with the initial data model |
| **CRN-01** | Data retention policies | Permanent retention for clinical records influences storage design |
| **CRN-31** | NOM-004-SSA3-2012 compliance | Mandates specific record structure and mandatory sections |
| **CRN-37** | System-wide unique patient identifier | Foundational to the clinical workflow across branches |

---

## Step 3: Elements to Refine

| Element | Current State | Refinement Action |
|---|---|---|
| **Clinical Care module (CC)** | Defined in Iteration 1 as a high-level domain module with patient registration, medical record creation, consultation recording, and attachment management responsibilities. | **Decompose internally** into DDD aggregates (`PatientAggregate`, `MedicalRecordAggregate`, `ConsultationAggregate`), an append-only `ClinicalEventStore`, and CQRS read models (`PatientSearchReadModel`, `ClinicalTimelineReadModel`, `Nom004RecordView`). Detail the immutable event stream, NOM-004 compliance, and patient identity service. |
| **Prescriptions module (RX)** | Defined in Iteration 1 with prescription creation, item management, and prescriber validation responsibilities. | **Refine** to show how prescriptions are created within a consultation context via `PrescriptionCommandHandler` appending to the shared clinical event stream. Scope to clinical ordering semantics — pharmacy dispensation and RBAC enforcement deferred to later iterations. |
| **Laboratory module (LAB)** | Defined in Iteration 1 with study request lifecycle, prepayment verification, and text-only result entry. | **Refine** to show `LabStudyCommandHandler` appending `LAB_ORDER` and `LAB_RESULT` events to the clinical event stream. Add `PendingLabStudiesReadModel` for lab technician work queues. Clarify how results become part of the immutable record. |
| **Domain model (clinical entities)** | High-level entity definitions exist for Patient, MedicalRecord, Consultation, Prescription, PrescriptionItem, LaboratoryStudy, Attachment. | **Detail and constrain**: introduce `ClinicalEvent` as the base event type; specialize into consultation, prescription, lab order, lab result, and attachment events. Add read-model projections. Enforce UUID identifiers and patient uniqueness (CRN-37). |
| **API Server interfaces (CC, RX, LAB)** | No concrete interfaces defined — section 8 was a placeholder. | **Define** command interfaces (CreatePatient, CreateMedicalRecord, AddConsultation, CreatePrescriptionFromConsultation, CreateLabStudiesFromConsultation, RecordLabResult) and query interfaces (SearchPatients, GetPatientClinicalTimeline, GetNom004Record, ListPendingLabStudies). |
| **PWA Client (clinical flows)** | Defined in Iteration 1 with five generic components (UI, State Management, API Client, Service Worker, Local Storage Manager). | **Extend** with clinical-specific components: `PatientSearchView`, `MedicalRecordView`, `ConsultationWizard`, `PendingLabStudiesView`, `LabResultEntryForm`, and `ClinicalStateManager`. |
| **Cloud Database (clinical schema)** | PostgreSQL selected with `branch_id` tenant isolation. No domain-specific schema defined. | **Define** table structures for clinical events, read-model projections, and indexing strategy for PER-03 (patient search and history retrieval). |

---

## Step 4: Design Concepts

### Architectural Patterns

| Design Concept | Pros | Cons | Discarded Alternatives |
|---|---|---|---|
| **Event-sourced / append-only clinical record** — Per-patient event stream of consultations, prescriptions, labs, attachments. *Addresses: US-026, CRN-02, AUD-03, CRN-01* | Strong immutability guarantee for clinical record and auditability. Fits permanent retention and NOM-004. Aligns with offline-aware, idempotent commands (CRN-43) | More complex query model requiring projections/read models. Team must discipline event schema evolution | Mutable record with soft-delete — risks silent data loss, weak immutability. Pure CRUD with update-in-place — violates US-026, CRN-02, AUD-03 |
| **CQRS for clinical workflows** — Write model for commands, read model for patient search and history. *Addresses: PER-03, US-027* | Separates write side (append-only events) from read side (optimized projections). Enables indexed projections for sub-1s search. Read models evolve independently | Additional complexity: projections, eventual consistency. Requires infrastructure to rebuild projections | Single unified CRUD model — slower queries, harder immutability enforcement. Ad-hoc denormalized tables — risks duplication and inconsistency |
| **DDD aggregates for clinical entities** — `PatientAggregate`, `MedicalRecordAggregate`, `ConsultationAggregate`. *Addresses: US-024, US-025, US-031, CRN-37* | Aggregates enforce invariants: one record per patient, append-only, prescriptions and labs tied to consultations. Clear transactional boundaries | Requires stronger modeling discipline. Must design aggregate boundaries carefully to avoid contention | Anemic domain model — business rules scattered, harder invariant reasoning. Entity-per-table with no aggregate boundaries — FK spaghetti, weak invariants |

### Externally Developed Components

| Design Concept | Pros | Cons | Discarded Alternatives |
|---|---|---|---|
| **PostgreSQL with normalized core + denormalized read views** — Normalized tables for aggregates, indexed views/materialized views for read side. *Addresses: PER-03, CRN-02* | Strong transactional guarantees and indexing. Normalized core aligns with DDD aggregates. Denormalized views serve read side efficiently. Row-level security compatible | Careful index and materialized view design required. Some complexity keeping views in sync | Document-only store — harder to enforce NOM-004 structure and complex queries. Pure NoSQL — weak relational guarantees, harder cross-cutting reports |
| **PostgreSQL `pg_trgm` extension for patient name search** — Trigram-based similarity search on patient names. *Addresses: PER-03* | Supports partial name matching natively. No external search infrastructure needed. Low operational overhead | Limited to similarity search, not full-text semantics. Index size grows with record count | External search engine (Elasticsearch) — overkill for current data volume. PostgreSQL `tsvector` — designed for full-text, not name similarity |

### Tactics

| Design Concept | Pros | Cons | Discarded Alternatives |
|---|---|---|---|
| **NOM-004-aligned structured projection** — Clinical record organized as a domain concept with mandatory sections. *Addresses: CRN-31, AUD-03* | Makes NOM-004 compliance explicit and auditable. Enables automated completeness validation. Projection can be regenerated if regulations change | Additional mapping logic between events and NOM sections. Needs maintenance if regulations change | Implicit compliance via free-form text — hard to prove compliance, no automated validation. Hard-coded screen layouts — UI-centric, brittle |
| **Global UUID-based patient identity** — `PatientId` as `EntityId` specialization enforced by `PatientAggregate`. *Addresses: CRN-37, CRN-43* | System-wide uniqueness across branches. Supports offline ID generation. No central coordination needed | Harder to read/debug vs integers. Slightly larger index size | Auto-increment per branch — collisions on merge. Composite natural keys — brittle, error-prone |
| **Guided consultation wizard in PWA** — Multi-step flow mirroring NOM-004 sections and back-end aggregates. *Addresses: USA-02* | Reduces errors for new residents. Structures data capture to match regulatory sections. UI mirrors domain model | More complex front-end state management. Must design flow states and backtracking rules | Unstructured forms — higher error rate, weaker NOM-004 alignment. Single long form — poor UX, error-prone |
| **Early audit event emission from clinical writes** — Clinical commands emit audit events to Audit & Compliance before full audit infra is built in Iteration 3. *Addresses: AUD-03, CRN-17* | No gap in auditability from day one. No retrofit when Iteration 3 designs the full audit trail | Audit & Compliance module receives events before its own detailed design is complete | Defer audit emission entirely to Iteration 3 — creates an unaudited gap for early clinical transactions |

---

## Step 5: Instantiation Decisions

| Instantiation Decision | Rationale |
|---|---|
| **`ClinicalEvent` stream per patient (append-only) as the core write model** — Every consultation, prescription, lab request/result, and attachment is stored as an immutable clinical event linked to `MedicalRecord` and `Patient`. No updates or deletes permitted. | Enforces US-026, CRN-02, CRN-01, and AUD-03 at the data model and domain levels. Each event carries an `IdempotencyKey` for safe offline replay (CRN-43). |
| **CQRS in Clinical Care bounded context** — Command-side event stream + read-side projections for patient timeline, NOM-004 sections, patient search, and pending lab studies. | Separates immutable writes from optimized reads. Enables fast patient search (PER-03) while keeping the core model simple and append-only. |
| **DDD aggregates: `PatientAggregate`, `MedicalRecordAggregate`, `ConsultationAggregate`** — Clear transactional boundaries within Clinical Care, Prescriptions, and Laboratory modules. | Aggregates enforce invariants: exactly one `MedicalRecord` per `Patient`, consultations are append-only, prescriptions and lab orders always tied to a consultation context (US-024, US-025, US-031, US-027). |
| **Global `PatientId` (UUID) with patient identity service** — `EntityId` specialization enforced by `PatientAggregate` validating uniqueness across all branches. | Guarantees a single clinical record per patient across the entire network (CRN-37). Compatible with offline ID generation per Iteration 1 conventions (CRN-43). |
| **Relational schema fragments for clinical entities + dedicated read models** — PostgreSQL core tables for `Patient`, `MedicalRecord`, `ClinicalEvent` specializations, plus `PatientSearchView`, `ClinicalTimelineView`. | Implements normalized core with indexed read models. Composite B-tree indexes and `pg_trgm` trigram indexes target PER-03. Respects `branch_id` tenant isolation. |
| **`Nom004RecordView` structured projection** — NOM-004-SSA3-2012 mandatory sections as a first-class read model generated from clinical events. | Makes regulatory compliance explicit and automatable (CRN-31, AUD-03). Can be regenerated without altering stored events. |
| **API endpoints for clinical workflows** — Command interfaces: `CreatePatient`, `CreateMedicalRecord`, `AddConsultation`, `CreatePrescriptionFromConsultation`, `CreateLabStudiesFromConsultation`, `RecordLabResult`. Query interfaces: `SearchPatients`, `GetPatientClinicalTimeline`, `GetNom004Record`, `ListPendingLabStudies`. | Instantiates CQRS and aggregate model into concrete interfaces. Each command appends events; each query uses read models. Directly supports US-024, US-025, US-031, US-019, US-020, US-023, US-027, US-038, US-040, US-041, US-042. |
| **Guided `ConsultationWizard` in PWA** — Multi-step flow: (1) vital signs and diagnosis, (2) prescriptions, (3) lab orders, (4) review and confirm. | Materializes the guided flow concept. Supports USA-02 (resident onboarding) and reduces errors by mirroring domain aggregates and NOM-004 structure in the UI. |
| **PWA clinical components** — `PatientSearchView`, `MedicalRecordView`, `ConsultationWizard`, `PendingLabStudiesView`, `LabResultEntryForm`, `ClinicalStateManager`. | Extends PWA Client architecture from Iteration 1 with clinical-specific UI and state management aligned with back-end modules and read models. |
| **Audit event emission hooks from `ClinicalEventStore`** — Clinical write operations emit audit events to `Audit & Compliance` before the full audit infra is designed in Iteration 3. | Ensures all clinical changes produce the minimal audit envelope required for AUD-03. No retrofit needed when Iteration 3 designs the complete audit trail. |

---

## Step 6: Views, Interfaces, and Design Decisions

### Diagrams Created / Updated

| Diagram | Section in Architecture.md | Description |
|---|---|---|
| Clinical Care Domain Model | Section 4 (sub-diagram) | Detailed clinical entities with `ClinicalEvent` as append-only event stream, specializations (Consultation, Prescription, LaboratoryStudy, LaboratoryResult, Attachment), and three read-model projections |
| Clinical Care Module Internals | Section 6.1.1 | CQRS component diagram: command-side aggregates (`PatientAggregate`, `MedicalRecordAggregate`, `ConsultationAggregate`, `ClinicalEventStore`), `PrescriptionCommandHandler`, `LabStudyCommandHandler`, and read-side projections with Audit & Compliance sink |
| PWA Clinical Workflow Components | Section 6.2.1 | Clinical UI components (`PatientSearchView`, `MedicalRecordView`, `ConsultationWizard`, `PendingLabStudiesView`, `LabResultEntryForm`) coordinated by `ClinicalStateManager` through `API Client` |
| SD-03: Create Patient and Medical Record | Section 7 | Full sequence from patient search (not found) through patient creation, global uniqueness check, medical record creation, and initial clinical event — enforcing CRN-37 and CRN-02 |
| SD-04: Add Consultation with Prescriptions and Lab Orders | Section 7 | ConsultationWizard-driven multi-step flow: vitals/diagnosis, prescriptions, lab orders, review. Atomic event bundle committed to ClinicalEventStore with IdempotencyKey |
| SD-05: Enter Lab Results and Project into Medical Record | Section 7 | Lab technician enters text results for a pending study. Result appended as a new `LAB_RESULT` event — original `LAB_ORDER` event remains unchanged (CRN-02, CON-05) |
| SD-06: Search Patient and Load Clinical Timeline | Section 7 | Read-side flow hitting `PatientSearchReadModel` and `ClinicalTimelineReadModel` with `Nom004RecordView` loaded in parallel — optimized for PER-03 |

### Interfaces Defined

#### Command Interfaces (Write Side)

| Command | Module | Endpoint | Events Produced | Key Drivers |
|---|---|---|---|---|
| CreatePatient | Clinical Care | `POST /patients` | `RECORD_CREATED` (via linked CreateMedicalRecord) | CRN-37, US-019, US-020, US-023 |
| CreateMedicalRecord | Clinical Care | Internal (after CreatePatient) | `RECORD_CREATED` | US-026, CRN-02, CRN-01 |
| AddConsultation | Clinical Care | `POST /consultations` | `CONSULTATION` | US-025, USA-02 |
| CreatePrescriptionFromConsultation | Prescriptions | `POST /consultations/:id/prescriptions` | `PRESCRIPTION` | US-031, US-031 |
| CreateLabStudiesFromConsultation | Laboratory | `POST /consultations/:id/lab-studies` | `LAB_ORDER` per study | US-038 |
| RecordLabResult | Laboratory | `POST /lab-studies/:id/results` | `LAB_RESULT` | US-041, US-042, CON-05 |

#### Query Interfaces (Read Side)

| Query | Read Model | Endpoint | Performance Target | Key Drivers |
|---|---|---|---|---|
| SearchPatients | `PatientSearchReadModel` | `GET /patients/search` | Sub-1s over 50,000+ records | PER-03, US-027 |
| GetPatientClinicalTimeline | `ClinicalTimelineReadModel` | `GET /patients/:id/timeline` | Pre-computed, paginated | US-027, US-025 |
| GetNom004Record | `Nom004RecordView` | `GET /patients/:id/nom004` | Structured projection | CRN-31, AUD-03 |
| ListPendingLabStudies | `PendingLabStudiesReadModel` | `GET /lab-studies/pending` | Branch-scoped, sorted | US-040 |

### Design Decisions

| Driver | Decision | Rationale | Discarded Alternatives |
|---|---|---|---|
| **US-026, CRN-02, AUD-03** | Adopt append-only clinical event stream (`ClinicalEvent`) as the authoritative write model for medical records | Enforces immutability at the deepest layer; satisfies NOM-004 permanent retention (CRN-01) and 100% modification blocking (AUD-03); aligns with offline-aware conventions (CRN-43) | Update-in-place CRUD — weaker guarantee; Soft-delete — still allows logical mutation |
| **PER-03, US-027** | Introduce CQRS with dedicated read models (`PatientSearchReadModel`, `ClinicalTimelineReadModel`, `Nom004RecordView`, `PendingLabStudiesReadModel`) backed by indexed views | Sub-1s patient search over 50,000+ records without compromising append-only integrity; read models evolve independently | Single unified model — trade-off between write simplicity and read performance; Full event sourcing with runtime projection — higher complexity |
| **CRN-31** | Represent NOM-004 sections as structured projection (`Nom004RecordView`) generated from clinical events | First-class regulatory compliance; automated completeness validation; regenerable without altering events | Free-form text — impossible to automate verification; Hard-coded screen layouts — brittle, not auditable |
| **CRN-37** | Global `PatientId` (UUID) enforced by `PatientAggregate` with cross-branch uniqueness validation | Exactly one record per patient across all branches; compatible with offline ID generation | Auto-increment per branch — collisions on sync; Composite natural keys — brittle; Centralized sequence server — incompatible with offline |
| **CRN-01** | Permanent retention for all clinical events; no deletion or archival; retention policy encoded in `MedicalRecord` aggregate | Satisfies NOM-004 permanent retention; combined with append-only store prevents data loss | Time-based archival — violates NOM-004; Soft-delete with recovery window — implies eventual deletion |
| **USA-02** | Guided `ConsultationWizard` in PWA with four steps: vitals/diagnosis, prescriptions, lab orders, review | Reduces cognitive load for residents; enforces structured NOM-004-aligned data capture; mirrors domain aggregates | Unstructured forms — higher error rate; Single long form — overwhelming, no progressive validation |
| **US-024, US-025, US-031, US-038** | DDD aggregates with clear transactional boundaries; prescriptions and lab orders created within consultation context as atomic event bundles | Enforces clinical invariants at domain level; prevents partial consultation data; enables independent module testing | Anemic domain model — scattered rules; Entity-per-table — weak invariant enforcement |
| **AUD-03, CRN-17** | Wire clinical writes to emit audit events to Audit & Compliance via `ClinicalEventStore` before full audit infra (Iteration 3) | No auditability gap from day one; no retrofit needed when Iteration 3 completes audit design | Defer to Iteration 3 — unaudited gap for early clinical transactions |
| **PER-03** | Composite B-tree indexes + `pg_trgm` trigram indexes on `PatientSearchReadModel`; partial indexes per branch | Sub-1s search over 50,000+ records; trigram indexes support partial name matching; partial indexes improve multi-tenant cache hits | No indexes — unacceptable performance; Elasticsearch — disproportionate overhead; `tsvector` — overkill for name search |

---

## Step 7: Analysis of Design and Iteration Goal Achievement

| Driver | Analysis Result |
|---|---|
| **US-024** — Create clinical record | **Satisfied.** `PatientAggregate` and `MedicalRecordAggregate` handle patient registration and record creation. SD-03 illustrates the full flow. One-to-one patient-record invariant enforced at the aggregate level. |
| **US-025** — Add consultation to record | **Satisfied.** `ConsultationAggregate` appends `CONSULTATION` events to the clinical event stream. SD-04 illustrates the guided wizard flow including prescriptions and lab orders. |
| **US-026** — Record immutability | **Satisfied.** Append-only `ClinicalEventStore` rejects all update and delete operations. Events are immutable once persisted. AUD-03 audit emission ensures modification attempts are logged. |
| **US-031** — Prescribe medications | **Satisfied.** `PrescriptionCommandHandler` creates prescriptions within consultation context. Prescription events are appended to the clinical event stream. Prescriber-level restrictions (R1–R4) are structurally supported and will be fully enforced in Iteration 3. |
| **US-019** — Register new patients | **Satisfied.** `CreatePatient` command with demographic validation, patient type classification, and guardian checks. |
| **US-020** — Classify patients by type | **Satisfied.** `PatientAggregate` applies type classification (Student, Worker, External) with automatic discount calculation. |
| **US-023** — Validate guardian for minors | **Satisfied.** `PatientAggregate` enforces mandatory guardian fields when patient age is under 17. |
| **US-027** — View comprehensive medical history | **Satisfied.** `ClinicalTimelineReadModel` provides chronologically ordered projection of all clinical events per patient. SD-06 illustrates the flow. |
| **US-038** — Request laboratory studies | **Satisfied.** `CreateLabStudiesFromConsultation` command appends `LAB_ORDER` events within consultation context. SD-04 includes lab order creation. |
| **US-040** — View pending laboratory studies | **Satisfied.** `PendingLabStudiesReadModel` provides branch-scoped list of pending studies. SD-05 shows the lab technician flow. |
| **US-041** — Enter laboratory results | **Satisfied.** `RecordLabResult` command appends `LAB_RESULT` event. Text-only per CON-05. Original order event remains unchanged. |
| **US-042** — View lab results in medical record | **Satisfied.** Lab results appear in `ClinicalTimelineReadModel` and `Nom004RecordView` as part of the patient's immutable clinical history. |
| **PER-03** — Patient search under 1 second | **Satisfied.** `PatientSearchReadModel` with composite B-tree and `pg_trgm` trigram indexes. Partial indexes per branch. SD-06 illustrates the optimized read flow. |
| **USA-02** — Guided consultation flow for residents | **Satisfied.** `ConsultationWizard` implements a four-step guided flow mirroring NOM-004 sections. Each step validates completeness before progression. |
| **AUD-03** — 100% modification attempts blocked and logged | **Satisfied.** Append-only event store rejects mutations. `ClinicalEventStore` emits audit events to Audit & Compliance for every clinical write. |
| **CRN-02** — Immutable data model for clinical records | **Satisfied.** `ClinicalEvent` stream is insert-only. No update or delete operations are exposed at any layer. |
| **CRN-01** — Data retention policies | **Satisfied.** Permanent retention policy encoded in `MedicalRecord` aggregate. No deletion or archival mechanism exists for clinical events. |
| **CRN-31** — NOM-004-SSA3-2012 compliance | **Satisfied.** `Nom004RecordView` organizes clinical events into mandatory regulatory sections. Automated completeness validation supported. |
| **CRN-37** — System-wide unique patient identifier | **Satisfied.** Global `PatientId` (UUID) with cross-branch uniqueness validation in `PatientAggregate`. One medical record per patient guaranteed. |

### Summary

| Status | Count | Drivers |
|---|---|---|
| **Satisfied** | 19 | US-024, US-025, US-026, US-031, US-019, US-020, US-023, US-027, US-038, US-040, US-041, US-042, PER-03, USA-02, AUD-03, CRN-02, CRN-01, CRN-31, CRN-37 |
| **Partially Satisfied** | 0 | — |
| **Not Satisfied** | 0 | — |

All 19 drivers for Iteration 2 have been satisfied. The core clinical workflow architecture — including the immutable event stream, CQRS read models, NOM-004 structured projections, global patient identity, and guided consultation wizard — is now in place. Downstream iterations can safely depend on the clinical data model for pharmacy dispensation (Iteration 5), security enforcement (Iteration 3), and offline synchronization (Iteration 6).
