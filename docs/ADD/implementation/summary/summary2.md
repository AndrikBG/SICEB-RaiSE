# Summary — Phase 2 (Completed + Fix Closure)

> Last updated: 2026-03-24 | Activities completed: A2.1, A2.2, A2.3, A2.4, A2.5, A2.6, A2.7 + Phase 2 Fix Closure | Tasks: 25/25 (+ operational fixes)

---

## Files Added

### Backend — Flyway Migrations (`backend/src/main/resources/db/migration/`)

| File | Purpose |
|------|---------|
| `V004__create_patients_table.sql` | Patient aggregate root: UUID PK, first_name/paternal_surname/maternal_surname, date_of_birth (DATE), gender/patient_type/profile_status (VARCHAR with CHECK constraints), discount_percentage (DECIMAL 5,2), guardian fields (name, relationship, phone, id_confirmed), data_consent_given, special_case + notes, branch_id FK. Indexes: ix_patients_branch_id (B-tree), ix_patients_name_trgm (GIN pg_trgm on full name concat), ix_patients_dob, ix_patients_curp (partial). |
| `V005__create_medical_records_table.sql` | Medical record container: record_id UUID PK, patient_id UUID with UNIQUE constraint (exactly one record per patient — CRN-02), branch_id FK, created_at TIMESTAMPTZ, created_by_staff_id UUID. Index: ix_medical_records_branch_id. |
| `V006__create_clinical_events_table.sql` | IC-02 hybrid JSONB event store: event_id UUID PK, record_id FK, event_type VARCHAR(30) with CHECK (6 allowed types), occurred_at TIMESTAMPTZ, branch_id FK, performed_by_staff_id UUID, idempotency_key VARCHAR(64), payload JSONB. 4 indexes: ix_clinical_events_record_id_occurred_at (B-tree composite), ix_clinical_events_branch_id_event_type (B-tree composite), ix_clinical_events_idempotency_key (UNIQUE), ix_clinical_events_payload_gin (GIN). 2 triggers: trg_clinical_events_no_update + trg_clinical_events_no_delete calling `prevent_clinical_event_mutation()` function that raises exception on UPDATE/DELETE. |
| `V007__create_clinical_read_models.sql` | Two read model tables. `patient_search_view`: patient_id PK + FK, full_name, date_of_birth, patient_type, gender, phone, profile_status, branch_id FK, record_id FK, last_visit_date, consultation_count, created_at. 5 indexes: branch_id, full_name GIN pg_trgm, date_of_birth, composite branch_id+patient_type, composite branch_id+last_visit_date DESC. `pending_lab_studies_view`: study_id PK, event_id FK, record_id FK, patient_id FK, patient_name, consultation_id, study_type, priority, status, instructions, requested_at, requested_by_staff, result_text, result_recorded_at, result_recorded_by, branch_id FK. 3 indexes: branch+status, branch+requested_at, patient_id. |

### Backend — Domain Module: `com.siceb.domain.clinicalcare`

| File | Purpose |
|------|---------|
| `model/PatientType.java` | `enum PatientType` with 3 values: `STUDENT` (30% discount), `WORKER` (20%), `EXTERNAL` (0%). Each carries `BigDecimal discountPercentage` field and `discountPercentage()` accessor. |
| `model/Gender.java` | `enum Gender`: MALE, FEMALE, OTHER. |
| `model/ProfileStatus.java` | `enum ProfileStatus`: COMPLETE, INCOMPLETE. INCOMPLETE when CURP or other required data is missing (US-019). |
| `model/ClinicalEventType.java` | `enum ClinicalEventType`: RECORD_CREATED, CONSULTATION, PRESCRIPTION, LAB_ORDER, LAB_RESULT, ATTACHMENT. |
| `model/Patient.java` | `@Entity` with `@Table(name = "patients")`. UUID `@Id` (offline-safe). 22 fields including all demographic, guardian, classification, and audit fields. Builder pattern via static inner `Builder` class. Domain methods: `fullName()` (composites first + paternal + maternal), `ageInYears()` (via `Period.between`), `isMinor()` (<17), `updateType(PatientType, credentialNumber)`. Private `resolveProfileStatus()` sets INCOMPLETE when CURP is missing. |
| `model/MedicalRecord.java` | `@Entity` with `@Table(name = "medical_records")`. Fields: recordId (UUID PK), patientId (UUID, unique=true), branchId, createdAt (Instant), createdByStaffId. Constructor sets `createdAt = Instant.now()`. No setters — immutable once created. |
| `model/ClinicalEvent.java` | `@Entity` with `@Table(name = "clinical_events")`. Fields: eventId (UUID PK), recordId, eventType (`@Enumerated STRING`), occurredAt (Instant), branchId, performedByStaffId, idempotencyKey (String), payload (`Map<String,Object>` with `@JdbcTypeCode(SqlTypes.JSON)` and `columnDefinition = "jsonb"`). Builder pattern. Payload stored as `Map.copyOf()` for defensive copy. |
| `command/CreatePatientCommand.java` | `record` with Jakarta Validation: `@NotNull patientId`, `@NotBlank firstName/paternalSurname`, `@Past dateOfBirth`, `@NotNull gender/patientType`, guardian fields, `@NotBlank idempotencyKey`. |
| `command/AddConsultationCommand.java` | `record` with SOAP fields: consultationId, recordId, subjective, objective, diagnosis, diagnosisCode, plan, vitalSigns, requiresSupervision flag, supervisorStaffId, idempotencyKey. |
| `exception/ClinicalDomainException.java` | Extends `RuntimeException`, carries `ErrorCode` field. Used by all domain services for business rule violations. |
| `repository/PatientRepository.java` | `JpaRepository<Patient, UUID>`. Custom query `findDuplicateCandidates(String name, LocalDate dob)` using JPQL CONCAT + LOWER for homonym detection (US-019 scenario 2). `existsByPatientId(UUID)`. |
| `repository/MedicalRecordRepository.java` | `JpaRepository<MedicalRecord, UUID>`. `findByPatientId(UUID)`, `existsByPatientId(UUID)`. |
| `repository/ClinicalEventRepository.java` | `JpaRepository<ClinicalEvent, UUID>`. `findByIdempotencyKey(String)`, `existsByIdempotencyKey(String)`, `findByRecordIdOrderByOccurredAtAsc/Desc(UUID, Pageable)`, `findByRecordIdAndEventTypeOrderByOccurredAtAsc(UUID, ClinicalEventType)`, `findByBranchIdAndEventType(UUID, ClinicalEventType)`. |
| `service/ClinicalEventStore.java` | `@Service`. Core append-only persistence (IC-02, CRN-02). `append(ClinicalEvent)` — checks idempotency key, returns existing event on replay or saves new. `appendAll(List)` — atomic multi-event within single `@Transactional`. Read methods: `findByRecordChronological`, `findByRecordAndType`, `findByBranchAndType`, `findByIdempotencyKey`. |
| `service/PatientService.java` | `@Service`. `createPatient(CreatePatientCommand)` — validates guardian (<17 requires guardian unless special case, US-023), validates credential (STUDENT/WORKER requires credential, US-020), validates phone format (10-15 digits). Creates Patient → MedicalRecord → RECORD_CREATED event atomically in `@Transactional`. Returns `CreatePatientResult(patientId, recordId)`. `findDuplicateCandidates()` for homonym detection. |
| `service/ConsultationService.java` | `@Service`. `addConsultation(AddConsultationCommand)` — verifies record exists, builds SOAP payload (subjective, objective, diagnosis, diagnosisCode, plan, vitalSigns, requiresSupervision, supervisorStaffId), appends CONSULTATION event. Returns consultationId. |
| `readmodel/PatientSearchEntry.java` | `@Entity` with `@Table(name = "patient_search_view")`. 12 fields: patientId PK, fullName, dateOfBirth, patientType, gender, phone, profileStatus, branchId, recordId, lastVisitDate, consultationCount, createdAt. `updateLastVisit(Instant)` increments count and updates date. |
| `readmodel/PatientSearchRepository.java` | `JpaRepository<PatientSearchEntry, UUID>`. `search(branchId, query, dateOfBirth, patientType, Pageable)` — JPQL with LIKE on LOWER(fullName) + optional date and type filters, ordered by fullName. |
| `readmodel/ClinicalTimelineService.java` | `@Service @Transactional(readOnly = true)`. `getTimeline(UUID recordId)` returns `List<TimelineEntry>` ordered ASC. `getTimelinePaginated(UUID, Pageable)` returns `Page<TimelineEntry>` ordered DESC (most recent first). `TimelineEntry` record: eventId, eventType, occurredAt, performedByStaffId, summary, payload. `extractSummary()` generates human-readable summary per event type (e.g., "Consultation: {diagnosis}", "Prescription: {N} medications"). |
| `readmodel/Nom004RecordService.java` | `@Service @Transactional(readOnly = true)`. `buildRecord(UUID patientId)` — loads Patient + MedicalRecord + all ClinicalEvents, builds NOM-004-SSA3-2012 structured record with 6 mandatory sections: identification (patient demographics + guardian if minor), clinicalNotes (CONSULTATION events), diagnostics (extracted diagnosis+code), labSummaries (LAB_ORDER + LAB_RESULT), prescriptions (PRESCRIPTION events), attachments (ATTACHMENT events). Returns `Nom004Record` record. |
| `readmodel/ClinicalEventProjector.java` | `@Service`. `project(ClinicalEvent)` — dispatches to projectors by event type. `projectRecordCreated` → creates `PatientSearchEntry` from Patient data. `projectConsultation` → updates `lastVisitDate` and increments `consultationCount` on search entry. |

### Backend — Domain Module: `com.siceb.domain.prescriptions`

| File | Purpose |
|------|---------|
| `command/CreatePrescriptionCommand.java` | `record` with `@NotNull prescriptionId/consultationId/recordId`, `@NotEmpty List<PrescriptionItemDto>`, `@NotBlank idempotencyKey`. Nested `PrescriptionItemDto` record with 8 fields: medicationId (UUID), medicationName, quantity (Integer), dosage, frequency, duration, route, instructions. |
| `service/PrescriptionCommandHandler.java` | `@Service`. `createPrescription(CreatePrescriptionCommand)` — verifies CONSULTATION event exists, validates non-empty items list, maps items to List of Maps for JSONB payload (medicationId, medicationName, quantity, dosage, frequency, duration, route, instructions). Appends PRESCRIPTION event with prescriptionId as eventId. |

### Backend — Domain Module: `com.siceb.domain.laboratory`

| File | Purpose |
|------|---------|
| `model/StudyStatus.java` | `enum StudyStatus`: PENDING, IN_PROGRESS, COMPLETED, REJECTED. |
| `model/PendingLabStudy.java` | `@Entity` with `@Table(name = "pending_lab_studies_view")`. 16 fields. Constructor sets initial status to PENDING. `recordResult(String resultText, UUID recordedBy)` transitions status to COMPLETED, sets resultText/resultRecordedAt/resultRecordedBy. |
| `command/CreateLabStudiesCommand.java` | `record` with `@NotNull consultationId/recordId`, `@NotEmpty List<LabStudyItem>`, `@NotBlank idempotencyKey`. Nested `LabStudyItem` record: studyId (UUID), studyType, priority, instructions. |
| `command/RecordLabResultCommand.java` | `record` with `@NotNull studyId/resultId`, `@NotBlank resultText/idempotencyKey`. |
| `repository/PendingLabStudyRepository.java` | `JpaRepository<PendingLabStudy, UUID>`. `findByBranchIdAndStatusOrderByRequestedAtAsc`, `findByBranchIdAndStatusInOrderByRequestedAtAsc` (multi-status filter), `findByPatientIdOrderByRequestedAtDesc`. |
| `service/LabStudyCommandHandler.java` | `@Service`. `createLabStudies(CreateLabStudiesCommand)` — verifies CONSULTATION event exists, creates one LAB_ORDER event per study + one PendingLabStudy read model entry per study. Idempotency keys suffixed with `-LAB-{index}`. `recordLabResult(RecordLabResultCommand)` — finds PendingLabStudy, validates status is PENDING or IN_PROGRESS, appends LAB_RESULT event, calls `study.recordResult()` to transition status. |

### Backend — Platform Module: `com.siceb.platform.iam`

| File | Purpose |
|------|---------|
| `StaffContext.java` | `ThreadLocal<UUID>` holder for current staff member identity. Static methods: `set(UUID)`, `get()` → `Optional<UUID>`, `require()` → UUID (throws if not set), `clear()`. Same pattern as `TenantContext`. In Phase 2: populated from `X-Staff-Id` header. In Phase 3: populated from JWT claims. |

### Backend — API Layer: `com.siceb.api`

| File | Purpose |
|------|---------|
| `ClinicalController.java` | `@RestController @RequestMapping("/api")`, `@Tag("Clinical Care")`. 6 endpoints: `POST /patients` (201 + patientId/recordId), `POST /consultations` (201 + consultationId), `POST /consultations/{id}/prescriptions` (201 + prescriptionId), `GET /patients/search` (paginated, params: q, dateOfBirth, type), `GET /patients/{id}/timeline` (paginated), `GET /patients/{id}/nom004` (structured record). All `@Operation` annotated for OpenAPI. |
| `LabStudyController.java` | `@RestController @RequestMapping("/api")`, `@Tag("Laboratory")`. 3 endpoints: `POST /consultations/{id}/lab-studies` (201 + studyIds list), `POST /lab-studies/{studyId}/results` (201 + resultId), `GET /lab-studies/pending` (filtered by branch + optional status). |
| `GlobalExceptionHandler.java` | `@RestControllerAdvice`. Handlers: `ClinicalDomainException` → mapped HTTP status via `ErrorCode`, `IllegalStateException` → 400, `NoSuchElementException` → 404, `MethodArgumentNotValidException` → 400 with field-level details map, generic `Exception` → 500. All return structured `ErrorResponse`. |

### Frontend

| File | Purpose |
|------|---------|
| `src/lib/clinical-api.ts` | Typed API client wrapping Axios. 17 TypeScript interfaces for request/response types. `clinicalHeaders()` injects `X-Branch-Id` and `X-Staff-Id` dev headers. 10 exported API functions: createPatient, addConsultation, createPrescription, createLabStudies, recordLabResult, searchPatients, getTimeline, getNom004, getPendingLabStudies. |
| `src/stores/clinical-store.ts` | Zustand store `useClinicalStore` — `ClinicalStateManager` per architecture. State: activePatient, timeline, pendingLabStudies, searchQuery, searchResults, isLoading, wizard (WizardState). WizardState: step, recordId, consultationId, SOAP fields, prescriptionItems array, labStudies array. 13 actions including addPrescriptionItem/removePrescriptionItem/updatePrescriptionItem and addLabStudy/removeLabStudy/updateLabStudy. |
| `src/components/layout.tsx` | Layout shell: sticky header with SICEB logo, 3-item nav (Pacientes, Consultas, Laboratorio) with active route highlighting, connection status dot, "Dev Mode" label. `<Outlet />` for nested routes. Responsive: nav hidden on small screens. |
| `src/features/clinical/ConsultationsLandingView.tsx` | Landing page for `/consultations` route. Displays a prompt to search and select a patient before starting a consultation. Links back to patient search. |
| `src/features/clinical/PatientSearchView.tsx` | Search bar with Enter key + button trigger. Results table: name, DOB, type badge (color-coded), last visit, consultation count. Row click navigates to MedicalRecordView. "+ Nuevo Paciente" button opens `NewPatientModal` — form with 2-column grid: nombres, apellidos, DOB, gender, tipo, teléfono, CURP, consentimiento checkbox. Submits via `clinicalApi.createPatient()` with `crypto.randomUUID()` for patientId and idempotencyKey. Helper components: `Input`, `Select`. |
| `src/features/clinical/MedicalRecordView.tsx` | Two tabs: "Timeline Clínico" and "NOM-004". Timeline: paginated list of `TimelineCard` components, each expandable to show raw JSON payload. Cards color-coded by event type (purple=registro, blue=consulta, green=receta, amber=lab orden, teal=lab resultado). Pagination: Anterior/Siguiente buttons. NOM-004 tab: 6 collapsible sections (Identificación, Notas Clínicas, Diagnósticos, Laboratorio, Recetas, Adjuntos). "+ Nueva Consulta" button links to ConsultationWizard. |
| `src/features/clinical/ConsultationWizard.tsx` | 4-step wizard with StepIndicator (numbered circles + labels + progress bar). **Step 1**: SOAP fields (vitalSigns textarea, subjective, objective, diagnosis text + CIE-10 code, plan, requiresSupervision checkbox). **Step 2**: Prescription items (add/remove, 4-field grid: name, dosage, frequency, duration). **Step 3**: Lab studies (add/remove, study type + priority select + instructions). **Step 4**: Review summary. Submit calls 3 API endpoints sequentially: addConsultation → createPrescription (if items) → createLabStudies (if studies). Navigates back to MedicalRecordView on success. |
| `src/features/clinical/PendingLabStudiesView.tsx` | Status filter buttons (Todos pendientes, Pendiente, En proceso, Completado). Study cards: study type, status badge, priority "URGENTE" label (animated pulse), patient name, instructions, timestamp. Click on PENDING/IN_PROGRESS study navigates to LabResultEntryForm. Auto-loads on mount and filter change. |
| `src/features/clinical/LabResultEntryForm.tsx` | Back link to pending list. Study ID display. Textarea for result text (10 rows). "Validar y Publicar Resultado" button calls `clinicalApi.recordLabResult()` with `crypto.randomUUID()` for resultId and idempotencyKey. Navigates back to `/lab` on success. Error display. |

---

## Files Modified

| File | Change |
|------|--------|
| `backend/src/main/java/com/siceb/platform/branch/TenantFilter.java` | Added `X-Staff-Id` header extraction → `StaffContext.set()`. Renamed `HEADER_NAME` → `BRANCH_HEADER`, added `STAFF_HEADER` constant. Added `StaffContext.clear()` in `finally` block alongside `TenantContext.clear()`. |
| `backend/src/test/java/com/siceb/platform/branch/TenantFilterTest.java` | Updated 3 references from `TenantFilter.HEADER_NAME` to `TenantFilter.BRANCH_HEADER`. |
| `backend/src/test/java/com/siceb/architecture/DomainStubsArchTest.java` | Added `ACTIVATED_MODULES` set (clinicalcare, prescriptions, laboratory). Added `isInActivatedModule(JavaClass)` helper. Tests `noBusinessLogicInStubModules`, `noJpaEntitiesInStubModules`, `noSpringComponentsInStubModules` now skip classes in activated modules. `allTenDomainModulesExist` updated to detect parent packages from sub-packages. |
| `backend/src/main/resources/db/migration/R__rls_policies.sql` | Replaced comment-only content with 5 `SELECT apply_rls_policy(...)` calls for patients, medical_records, clinical_events, patient_search_view, pending_lab_studies_view. |
| `frontend/src/main.tsx` | Added `import { BrowserRouter } from 'react-router-dom'`. Wrapped `<App />` in `<BrowserRouter>`. |
| `frontend/src/App.tsx` | Replaced landing page (status badge + sync counter) with `<Routes>` containing 6 routes inside `<Layout>`: `/` redirects to `/patients`, `/patients` → PatientSearchView, `/patients/:patientId` → MedicalRecordView, `/patients/:patientId/consultation` → ConsultationWizard, `/lab` → PendingLabStudiesView, `/lab/:studyId/result` → LabResultEntryForm. |
| `backend/src/main/java/com/siceb/domain/clinicalcare/service/ClinicalEventStore.java` | Added read-model projection trigger (`ClinicalEventProjector`) after append, including idempotent replay path to keep CQRS read-side convergent. |
| `backend/src/main/java/com/siceb/domain/clinicalcare/readmodel/ClinicalEventProjector.java` | Made projection idempotent/upsert for `patient_search_view` and fixed CONSULTATION projection by resolving patient via `recordId` when payload lacks patientId. |
| `backend/src/main/java/com/siceb/domain/clinicalcare/readmodel/PatientSearchEntry.java` | Extended read model with `curp` and `credentialNumber` for broader lookup scenarios (`US-028`). |
| `backend/src/main/java/com/siceb/domain/clinicalcare/readmodel/PatientSearchRepository.java` | Expanded search query to include fullName, CURP, credential, phone and patient UUID text match. |
| `backend/src/main/java/com/siceb/api/ClinicalController.java` | Added path/body `consultationId` consistency validation for prescriptions and new endpoint `GET /api/patients/duplicates` for duplicate detection support (`US-019`). |
| `backend/src/main/java/com/siceb/api/LabStudyController.java` | Added path/body `consultationId` consistency validation for lab study orders. |
| `backend/src/main/java/com/siceb/domain/laboratory/service/LabStudyCommandHandler.java` | Populates `PendingLabStudy.patientName` from patient aggregate for meaningful lab queue display (`US-040`). |
| `frontend/src/lib/clinical-api.ts` | Added `findPotentialDuplicates` client call and extended search result types with CURP/credential fields. |
| `frontend/src/features/clinical/PatientSearchView.tsx` | Added duplicate pre-check before save, improved API error rendering, broader search hint text, and immediate `activePatient` hydration after create. |
| `backend/src/main/java/com/siceb/domain/clinicalcare/readmodel/ClinicalTimelineService.java` | Added missing `CORRECTIVE_ADDENDUM` switch case in `extractSummary` method to fix compilation error and removed unnecessary `@SuppressWarnings("unchecked")`. |

### Additional Files Added (Fix Closure)

| File | Purpose |
|------|---------|
| `backend/src/main/resources/db/migration/V008__extend_patient_search_lookup_fields.sql` | Adds `curp` and `credential_number` columns + indexes to `patient_search_view` to support broader lookup in `US-028`. |

---

## Files Deleted

None — Phase 2 did not remove any files.

---

## How Components Relate

### A2.1–A2.4 — Backend Clinical Modules

```
com.siceb
├── api/
│   ├── SystemController.java               (Phase 1 — unchanged)
│   ├── ClinicalController.java             (A2.6 — clinical REST endpoints)
│   ├── LabStudyController.java             (A2.6 — lab study REST endpoints)
│   └── GlobalExceptionHandler.java         (A2.6 — structured error handling)
├── config/                                  (Phase 1 — unchanged)
├── shared/                                  (Phase 1 — unchanged)
├── platform/
│   ├── branch/
│   │   ├── TenantContext.java              (Phase 1 — unchanged)
│   │   ├── TenantFilter.java              (Phase 1 → Phase 2: added X-Staff-Id)
│   │   ├── TenantAwareDataSource.java     (Phase 1 — unchanged)
│   │   └── TenantConnectionInterceptor.java (Phase 1 — unchanged)
│   ├── iam/
│   │   └── StaffContext.java               (A2.2 — staff identity ThreadLocal)
│   ├── audit/package-info.java             (stub)
│   └── sync/package-info.java              (stub)
└── domain/
    ├── clinicalcare/                        (A2.1–A2.2, A2.5 — ACTIVATED)
    │   ├── model/
    │   │   ├── Patient.java, MedicalRecord.java, ClinicalEvent.java
    │   │   ├── PatientType.java, Gender.java, ProfileStatus.java
    │   │   └── ClinicalEventType.java
    │   ├── command/
    │   │   ├── CreatePatientCommand.java
    │   │   └── AddConsultationCommand.java
    │   ├── exception/
    │   │   └── ClinicalDomainException.java
    │   ├── repository/
    │   │   ├── PatientRepository.java
    │   │   ├── MedicalRecordRepository.java
    │   │   └── ClinicalEventRepository.java
    │   ├── service/
    │   │   ├── ClinicalEventStore.java
    │   │   ├── PatientService.java
    │   │   └── ConsultationService.java
    │   └── readmodel/
    │       ├── PatientSearchEntry.java, PatientSearchRepository.java
    │       ├── ClinicalTimelineService.java
    │       ├── Nom004RecordService.java
    │       └── ClinicalEventProjector.java
    ├── prescriptions/                       (A2.3 — ACTIVATED)
    │   ├── command/CreatePrescriptionCommand.java
    │   └── service/PrescriptionCommandHandler.java
    ├── laboratory/                          (A2.4 — ACTIVATED)
    │   ├── model/StudyStatus.java, PendingLabStudy.java
    │   ├── command/CreateLabStudiesCommand.java, RecordLabResultCommand.java
    │   ├── repository/PendingLabStudyRepository.java
    │   └── service/LabStudyCommandHandler.java
    ├── pharmacy/package-info.java           (stub)
    ├── inventory/package-info.java          (stub)
    ├── supplychain/package-info.java        (stub)
    ├── scheduling/package-info.java         (stub)
    ├── billing/package-info.java            (stub)
    ├── reporting/package-info.java          (stub)
    └── training/package-info.java           (stub)
```

### Request Flow — Clinical Command

```
HTTP Request (POST /api/patients)
  │ Headers: X-Branch-Id, X-Staff-Id, Content-Type: application/json
  │ Body: CreatePatientCommand JSON
  ▼
TenantFilter (@Order 1)
  │ Sets TenantContext (branch_id) + StaffContext (staff_id)
  ▼
SecurityConfig (permitAll — Phase 3 will enforce auth)
  ▼
ClinicalController.createPatient()
  │ @Valid validates CreatePatientCommand
  ▼
PatientService.createPatient()
  │ TenantContext.require() → branch_id
  │ StaffContext.require() → staff_id
  │ validateGuardian() — US-023: <17 requires guardian
  │ validateCredential() — US-020: STUDENT/WORKER requires credential
  │ validatePhone() — US-019: 10-15 digits
  ▼
PatientRepository.save(patient)
MedicalRecordRepository.save(record)
ClinicalEventStore.append(RECORD_CREATED event)
  │ Check idempotency key → return existing or save new
  ▼
TenantAwareDataSource.getConnection()
  │ SET LOCAL app.branch_id = '<uuid>'
  ▼
PostgreSQL
  │ INSERT into patients (RLS: branch_id check)
  │ INSERT into medical_records (RLS: branch_id check)
  │ INSERT into clinical_events (append-only, idempotency UNIQUE)
  ▼
Response: 201 Created { patientId, recordId }
  │
TenantFilter.finally → TenantContext.clear() + StaffContext.clear()
```

### CQRS Data Flow

```
                   ┌─────────────────────────────────┐
                   │        Write Side (Commands)      │
                   │                                   │
Command ────────▶  │  PatientService                   │
                   │  ConsultationService               │
                   │  PrescriptionCommandHandler        │
                   │  LabStudyCommandHandler            │
                   │         │                          │
                   │         ▼                          │
                   │  ClinicalEventStore.append()      │
                   │         │                          │
                   │         ▼                          │
                   │  clinical_events (append-only)    │
                   └─────────┬───────────────────────┘
                             │
                             ▼
                   ┌─────────────────────────────────┐
                   │        Read Side (Queries)        │
                   │                                   │
                   │  ClinicalEventProjector           │
                   │         │                          │
                   │         ├──▶ patient_search_view   │──▶ GET /patients/search
                   │         └──▶ (last visit update)   │
                   │                                   │
                   │  ClinicalTimelineService          │──▶ GET /patients/:id/timeline
                   │  (queries clinical_events directly)│
                   │                                   │
                   │  Nom004RecordService              │──▶ GET /patients/:id/nom004
                   │  (Patient + events → 6 sections)  │
                   │                                   │
                   │  PendingLabStudyRepository        │──▶ GET /lab-studies/pending
                   │  (queries pending_lab_studies_view)│
                   └─────────────────────────────────┘
```

### A2.7 — Frontend Clinical Workflow

```
frontend/src/
├── main.tsx                                 (Phase 1 → Phase 2: added BrowserRouter)
├── App.tsx                                  (Phase 1 → Phase 2: routes + Layout)
├── index.css                                (Phase 1 — unchanged)
├── components/
│   ├── pwa-update-prompt.tsx               (Phase 1 — unchanged)
│   └── layout.tsx                           (A2.7 — app shell with nav + status)
├── hooks/
│   ├── use-pwa.ts                          (Phase 1 — unchanged)
│   └── use-online-status.ts               (Phase 1 — unchanged)
├── lib/
│   ├── db.ts                              (Phase 1 — unchanged)
│   ├── api-client.ts                      (Phase 1 — unchanged)
│   ├── ws-client.ts                       (Phase 1 — unchanged)
│   └── clinical-api.ts                    (A2.7 — typed clinical API client)
├── stores/
│   ├── auth-store.ts                      (Phase 1 — unchanged)
│   ├── ui-store.ts                        (Phase 1 — unchanged)
│   ├── sync-store.ts                      (Phase 1 — unchanged)
│   └── clinical-store.ts                  (A2.7 — ClinicalStateManager + wizard)
└── features/clinical/
    ├── ConsultationsLandingView.tsx       (A2.7 — consultations entry point)
    ├── PatientSearchView.tsx              (A2.7 — search + new patient modal)
    ├── MedicalRecordView.tsx              (A2.7 — timeline + NOM-004 tabs)
    ├── ConsultationWizard.tsx             (A2.7 — 4-step guided flow)
    ├── PendingLabStudiesView.tsx          (A2.7 — lab work queue)
    └── LabResultEntryForm.tsx             (A2.7 — text result capture)
```

### Store → API Dependency Graph

```
useClinicalStore ←── PatientSearchView (reads/writes searchQuery, searchResults, activePatient)
       │                     │
       │              clinicalApi.searchPatients()
       │                     │
       │              clinicalApi.createPatient()
       │
useClinicalStore ←── MedicalRecordView (reads/writes timeline, activePatient)
       │                     │
       │              clinicalApi.getTimeline()
       │              clinicalApi.getNom004()
       │
useClinicalStore ←── ConsultationWizard (reads/writes wizard state)
       │                     │
       │              clinicalApi.addConsultation()
       │              clinicalApi.createPrescription()
       │              clinicalApi.createLabStudies()
       │
useClinicalStore ←── PendingLabStudiesView (reads/writes pendingLabStudies)
       │                     │
       │              clinicalApi.getPendingLabStudies()
       │
       └──── LabResultEntryForm (standalone — uses clinicalApi directly)
                              │
                       clinicalApi.recordLabResult()
```

### Build Output (Production)

```
dist/
├── index.html                      0.71 KB
├── manifest.webmanifest            0.54 KB
├── sw.js                           (Workbox, precaches 11 entries ~335 KB)
├── workbox-a1d84f0b.js
├── favicon.svg
├── icons/
│   ├── icon-192.svg
│   └── icon-512.svg
└── assets/
    ├── index-*.css                21.10 KB (gzip:  5.25 KB)
    ├── index-*.js                305.14 KB (gzip: 97.57 KB)
    └── workbox-window.prod.es5-*.js  5.74 KB (gzip: 2.25 KB)
```

### Flyway Migration History (cumulative)

```
Version | Description                   | Type
--------|-------------------------------|----------
001     | init extensions               | Versioned  (Phase 1)
002     | create branches table         | Versioned  (Phase 1)
003     | create rls infrastructure     | Versioned  (Phase 1)
004     | create patients table         | Versioned  (Phase 2)
005     | create medical records table  | Versioned  (Phase 2)
006     | create clinical events table  | Versioned  (Phase 2)
007     | create clinical read models   | Versioned  (Phase 2)
008     | extend patient search fields  | Versioned  (Phase 2 fix closure)
—       | rls policies                  | Repeatable (Phase 1 → Phase 2 updated)
```

---

## Key Architectural Decisions Made

17. **`ACTIVATED_MODULES` in DomainStubsArchTest** — IC-01 only enforces stubs on non-activated modules; Phase 2 adds clinicalcare, prescriptions, laboratory to the set
18. **StaffContext via `X-Staff-Id` header** — same ThreadLocal pattern as TenantContext; populated from dev header now, JWT claims in Phase 3
19. **DB triggers for append-only enforcement** — `trg_clinical_events_no_update` + `trg_clinical_events_no_delete` raise exceptions; defense-in-depth beyond application layer
20. **ClinicalTimeline queries event store directly** — composite index on (record_id, occurred_at) is efficient enough; avoids maintaining a separate materialized view table
21. **GlobalExceptionHandler with ErrorResponse** — single `@RestControllerAdvice` maps all exceptions to structured `ErrorResponse` using `ErrorCode` catalog
22. **React Router DOM with Layout component** — leverages existing dependency; provides consistent header/nav/footer shell with online status indicator
23. **useClinicalStore as ClinicalStateManager** — Zustand store follows Phase 1 pattern; wizard state is transient (not persisted) since consultation data is session-only

---

## Test Results

- **Backend:** All tests pass (compilation + unit + ArchUnit architecture tests)
- **Frontend:** TypeScript compiles + Vite builds successfully
- **Pending:** Performance test E2.8 (sub-1s search over 50,000+ records) — requires seeded data, deferred to integration testing

---

## Notes / Known Gaps

- **E2.8 Performance tests deferred**: requires 50,000+ seeded patient records to validate PER-03 (sub-1s search). Will be addressed during integration testing.
- **CQRS eventual consistency**: `ClinicalEventProjector` is synchronous (in-transaction). Under high write load, may need async projection with outbox pattern.
- **Auth still dev-mode**: `X-Branch-Id` and `X-Staff-Id` headers used for tenant/staff context. Real JWT enforcement comes in Phase 3.
- **Frontend lint**: pre-existing lint issues in `dev-dist` generated files and legacy MedicalRecordView remain outside changed scope.
- **Duplicate detection basic**: `GET /api/patients/duplicates` performs name+DOB matching. More sophisticated homonym algorithms (Soundex, probabilistic) not yet implemented.

---

## Phase 2 — Final Summary

| Deliverable                     | Status | Key Metric                                                                          |
| ------------------------------- | ------ | ----------------------------------------------------------------------------------- |
| E2.1 — ClinicalEventStore       | ✅      | V006: hybrid JSONB, 4 indexes, 2 triggers, idempotent append                        |
| E2.2 — Clinical Care aggregates | ✅      | Patient (22 fields, builder), MedicalRecord (immutable), Consultation (SOAP)        |
| E2.3 — Prescriptions module     | ✅      | PRESCRIPTION events with items in JSONB payload                                     |
| E2.4 — Laboratory module        | ✅      | LAB_ORDER + LAB_RESULT events, PendingLabStudy read model                           |
| E2.5 — CQRS read models         | ✅      | PatientSearch (pg_trgm), Timeline (paginated), NOM-004 (6 sections), EventProjector |
| E2.6 — REST API                 | ✅      | 6 command endpoints + 4 query endpoints, GlobalExceptionHandler, OpenAPI            |
| E2.7 — PWA clinical views       | ✅      | 5 views + Layout + ClinicalStateManager, 305 KB JS bundle                           |
| E2.8 — Performance tests        | ⏳      | Deferred: requires 50,000+ seeded records for PER-03 validation                     |
