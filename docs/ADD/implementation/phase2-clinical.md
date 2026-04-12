# Phase 2 — Core Clinical Workflow and Medical Records

> **Specification:** [`requeriments2.md`](requeriments2.md) | **Status:** ✅ Completed (including operational fix closure)  
> **Drivers:** US-024, US-025, US-026, US-031, US-019, US-020, US-023, US-027, US-038, US-040, US-041, US-042, PER-03, USA-02, AUD-03, CRN-02, CRN-01, CRN-31, CRN-37  
> **Depends on:** Phase 1 complete

**Goal:** Implement Clinical Care, Prescriptions, and Laboratory with immutable (append-only) clinical events, CQRS with dedicated read models, and PWA views for the daily consultation workflow, complying with NOM-004-SSA3-2012.

---

## A2.1 — ClinicalEventStore implementation (IC-02)

- [x] **T2.1.1** Create `clinical_events` table with hybrid JSONB schema: fixed columns (event_id, record_id, event_type, occurred_at, branch_id, performed_by_staff_id, idempotency_key) + JSONB payload  
  - V006 migration: table with 8 fixed columns + JSONB payload; CHECK constraint on event_type  
- [x] **T2.1.2** Create indexes: ix_clinical_events_record_id_occurred_at, ix_clinical_events_branch_id_event_type, ix_clinical_events_idempotency_key (UNIQUE), ix_clinical_events_payload_gin  
  - Four indexes in V006: composite B-trees + GIN for JSONB  
- [x] **T2.1.3** Implement append-only logic: INSERT only, no UPDATE or DELETE  
  - DB triggers `trg_clinical_events_no_update` and `trg_clinical_events_no_delete`; ClinicalEventStore service only exposes `append()`  
- [x] **T2.1.4** Implement IdempotencyKey validation to prevent duplicate offline writes  
  - `ClinicalEventStore.append()` checks existence by key; idempotent replay returns existing event  

## A2.2 — Clinical Care aggregates

- [x] **T2.2.1** PatientAggregate: global patientId uniqueness (CRN-37), guardian validation for minors (US-023), patient type with discount (US-020)  
  - `PatientService` with validations: guardian for &lt;17, credential for Student/Worker, phone format  
- [x] **T2.2.2** MedicalRecordAggregate: exactly one record per patient, append-only (US-026, CRN-02)  
  - `MedicalRecord` entity with UNIQUE on patient_id; created with patient  
- [x] **T2.2.3** ConsultationAggregate: diagnosis, notes, vitals, requiresSupervision flag (US-025)  
  - `ConsultationService` persists CONSULTATION event in SOAP format + supervision flag  
- [x] **T2.2.4** Emit audit-related events from clinical aggregates  
  - Each write appends to ClinicalEventStore; implicit audit trail in event stream  

## A2.3 — Prescriptions module

- [x] **T2.3.1** PrescriptionCommandHandler: prescriptions within consultation context (US-031)  
  - Verifies CONSULTATION event exists; at least one line item  
- [x] **T2.3.2** PrescriptionItem: medication, quantity, dosage, instructions  
  - Items in JSONB payload of PRESCRIPTION event with 8 fields per item  
- [x] **T2.3.3** Structural prescription validation (full authorization in Phase 3)  
  - Required fields validated; prescriber-level auth deferred to Phase 3  

## A2.4 — Laboratory module

- [x] **T2.4.1** LabStudyCommandHandler: lab orders within consultation (US-038)  
  - Each study generates LAB_ORDER event + PendingLabStudy read-model row  
- [x] **T2.4.2** RecordLabResult: text results (CON-05), lifecycle (US-041)  
  - Separate LAB_RESULT event; status PENDING/IN_PROGRESS → COMPLETED  
- [x] **T2.4.3** PendingLabStudiesReadModel (US-040)  
  - `pending_lab_studies_view` with branch+status indexes; repository with filters  

## A2.5 — CQRS read models

- [x] **T2.5.1** PatientSearchReadModel with B-tree + pg_trgm (PER-03)  
  - V007: `patient_search_view` with five indexes including pg_trgm trigram  
- [x] **T2.5.2** ClinicalTimelineReadModel with chronological projection (US-027)  
  - `ClinicalTimelineService` queries event store with descending pagination  
- [x] **T2.5.3** Nom004RecordView with NOM-004-SSA3-2012 sections (CRN-31)  
  - `Nom004RecordService` builds structured record: identification, clinicalNotes, diagnostics, lab, prescriptions, attachments  
- [x] **T2.5.4** Project events to read models from ClinicalEventStore  
  - `ClinicalEventProjector` updates patient_search_view on RECORD_CREATED and CONSULTATION  

## A2.6 — REST interfaces for clinical flow

- [x] **T2.6.1** POST /patients (CreatePatient) — 201 with patientId and recordId  
- [x] **T2.6.2** POST /consultations (AddConsultation) — CONSULTATION persisted  
- [x] **T2.6.3** POST /consultations/:id/prescriptions (CreatePrescriptionFromConsultation)  
- [x] **T2.6.4** POST /consultations/:id/lab-studies (CreateLabStudiesFromConsultation)  
- [x] **T2.6.5** POST /lab-studies/:studyId/results (RecordLabResult)  
- [x] **T2.6.6** GET /patients/search, GET /patients/:id/timeline, GET /patients/:id/nom004, GET /lab-studies/pending  

## A2.7 — PWA views for clinical flow

- [x] **T2.7.1** PatientSearchView: search with filters, sub-1s results (PER-03)  
- [x] **T2.7.2** MedicalRecordView: clinical timeline and NOM-004 tabs (US-027, CRN-31)  
- [x] **T2.7.3** Four-step ConsultationWizard: vitals/diagnosis, prescriptions, lab, review (USA-02)  
- [x] **T2.7.4** PendingLabStudiesView and LabResultEntryForm (US-040, US-041)  
- [x] **T2.7.5** ClinicalStateManager for patient context and wizard state  

---

## Deliverables

- [x] **E2.1** ClinicalEventStore operational — JSONB table, indexes, append-only, idempotency  
- [x] **E2.2** Clinical Care module — Patient, MedicalRecord, Consultation aggregates with tests  
- [x] **E2.3** Prescriptions module — Prescriptions within consultation with line items  
- [x] **E2.4** Laboratory module — Orders, text results, pending queue  
- [x] **E2.5** CQRS read models — PatientSearch (sub-1s), Timeline, Nom004, PendingLab  
- [x] **E2.6** Clinical REST API — six commands + four queries with OpenAPI  
- [x] **E2.7** PWA clinical flow — PatientSearch, MedicalRecord, ConsultationWizard, Lab views  
- [ ] **E2.8** Performance tests — Sub-1s search over 50,000+ records (deferred: requires seeded data)  

---

## Notes and decisions

| # | Date | Decision | Context | Discarded |
|---|------|----------|---------|-----------|
| D-017 | 2026-03-23 | Updated DomainStubsArchTest to allow activated modules (clinicalcare, prescriptions, laboratory) | IC-01 only applies to stub modules; Phase 2 activates 3 of 10 domain modules | Modify package structure to separate stubs |
| D-018 | 2026-03-23 | StaffContext via X-Staff-Id header (dev); JWT claims in Phase 3 | Same pattern as TenantContext X-Branch-Id; enables testing without auth | Block development until Phase 3 auth |
| D-019 | 2026-03-23 | DB triggers for append-only enforcement + application layer | Defense-in-depth: even direct DB access cannot UPDATE/DELETE clinical events | Application-only enforcement |
| D-020 | 2026-03-23 | ClinicalTimelineService queries event store directly (no separate materialized view) | The composite index on (record_id, occurred_at) provides efficient chronological access | Separate materialized view table |
| D-021 | 2026-03-23 | GlobalExceptionHandler maps ClinicalDomainException to structured ErrorResponse | Consistent API error format using ErrorCode catalog from shared kernel | Per-controller exception handling |
| D-022 | 2026-03-23 | React Router DOM with Layout component for clinical workflow navigation | react-router-dom was already in dependencies; Layout provides consistent header + nav | Custom routing, single-page tabs |
| D-023 | 2026-03-23 | useClinicalStore (Zustand) as ClinicalStateManager with wizard state | Follows established Zustand pattern from Phase 1; wizard state is transient (no persist) | React Context, Redux |

### Fix closure update — 2026-03-24

- ✅ **CQRS projection closure:** `ClinicalEventStore.append()` now projects both new and idempotent-replay events to keep read models convergent.  
- ✅ **Consultation projection repair:** `ClinicalEventProjector` resolves `patientId` from `recordId` when missing in payload, restoring `consultationCount`/`lastVisitDate`.  
- ✅ **API contract consistency:** endpoints `/consultations/{consultationId}/prescriptions` and `/consultations/{consultationId}/lab-studies` validate path/body `consultationId` match.  
- ✅ **US-019 duplicate support (basic):** new endpoint `GET /api/patients/duplicates` for pre-save duplicate checks from frontend.  
- ✅ **US-028 search expansion (basic):** patient search now includes CURP, credential number, phone, and patient UUID text matching.  
- ✅ **Lab worklist data quality:** `PendingLabStudy.patientName` is now populated from patient aggregate instead of empty string.  
- ✅ **Frontend flow hardening:** new patient modal performs duplicate pre-check, sets `activePatient` before navigation, and surfaces structured backend validation details.  
- ✅ **Schema support:** new migration `V008__extend_patient_search_lookup_fields.sql` adds CURP/credential columns and indexes to `patient_search_view`.  
- ✅ **Compilation fix:** Added `CORRECTIVE_ADDENDUM` to the switch statement in `ClinicalTimelineService.java` to resolve an uncovered label compilation error and removed an unnecessary `@SuppressWarnings("unchecked")`.  
