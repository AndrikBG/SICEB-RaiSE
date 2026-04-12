# Iteration Plan — SICEB ADD Process

This document defines the iteration plan for the Attribute-Driven Design (ADD) of SICEB. Each iteration targets a coherent set of architectural drivers, progressing from foundational system structure through high-priority, business-critical capabilities toward operational resilience and future extensibility.

**Criteria for iteration ordering:**

1. The first iteration establishes the overall system structure (greenfield, top-down decomposition).
2. Subsequent iterations prioritize the primary user stories (top-ranked US) and their associated high/high quality attribute scenarios, addressing those that directly support the four business objectives (Gestión de Clientes, Gestión de Inventario, Gestión Financiera, Gestión de Personal) in early iterations.
3. Cross-cutting concerns (security, offline capability) are addressed as soon as the elements they affect are defined.
4. Integration and extensibility concerns are deferred to later iterations since they target future releases.

**Driver sources:**

- **US-xxx**: Top-ranked primary user stories by QA scenario support (from ArchitecturalDrivers.md)
- **US-xxx**: User stories with explicit priority level (from [Requirements/US/Table_US.md](../../Requirements/US/Table_US.md))
- **QA scenarios** (PER, SEC, REL, USA, ESC, AUD, MNT, IOP): Quality attribute scenarios (from Quality_Attribute_Scenarios.md)
- **CRN-xx**: High-priority architectural concerns (from ArchitecturalDrivers.md)
- **CON-xx**: Technical constraints (from ArchitecturalDrivers.md)

---

## Iteration Summary


| #     | Goal                                                                                                                                                                                                                                                                                                                                                                                                              | Drivers Addressed                                                                                                                                      |
| ----- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **1** | **Establish overall system structure** — Define the high-level system decomposition (PWA frontend, REST API backend, cloud database, local offline storage), allocate the 18 epics into cohesive modules, establish inter-module dependency rules, the multi-branch single-deployment model, foundational technical conventions, and offline-aware design conventions that all subsequent iterations must follow. | CRN-25, CRN-26, CRN-27, CRN-29, CRN-41, CRN-42, CRN-43, CON-01, CON-02, CON-03, CON-04, CON-05                                                         |
| **2** | **Core clinical workflow and medical records** *(Gestión de Clientes)* — Enable patient registration, clinical record creation, consultation recording, prescribing, and laboratory study tracking. Enforce medical record immutability and NOM-004 compliance. This is the highest business-value stream: without clinical care, the clinic does not operate.                                                    | US-024, US-025, US-026, US-031, US-019, US-020, US-023, US-027, US-038, US-040, US-041, US-042, PER-03, USA-02, AUD-03, CRN-02, CRN-01, CRN-31, CRN-37 |
| **3** | **Security, access control, and audit infrastructure** *(Gestión de Personal)* — Implement authentication, role-based access control for 11 roles with branch-scoped and residency-level permissions, REST API protection, personal data handling (LFPDPPP), and the centralized immutable audit trail that downstream iterations depend on.                                                                      | US-003, US-001, US-002, US-050, US-051, US-066, SEC-01, SEC-02, SEC-04, MNT-03, CRN-15, CRN-13, CRN-17, CRN-18, CRN-32                                 |
| **4** | **Multi-branch operations and inventory management** *(Gestión de Inventario)* — Enable branch registration, active branch selection, branch-scoped inventory views, and real-time inventory updates across branches. Validate the multi-tenant scalability model for network growth. Adopt command/delta-based inventory mutations to prepare for offline conflict resolution.                                   | US-071, US-074, US-004, US-004, US-005, US-064, PER-01, ESC-01, ESC-02, ESC-03, CRN-24, CRN-35, CRN-44                                                 |
| **5** | **Pharmacy, payments, and regulatory compliance** *(Gestión Financiera)* — Enable pharmacy dispensation with prescription validation, controlled substance traceability for COFEPRIS, payment registration, supply request approval workflows, and the asynchronous business compensation protocol for offline regulatory violations.                                                                             | US-044, US-032, US-033, US-034, US-035, US-044, PER-04, SEC-03, AUD-01, AUD-02, USA-04, CRN-33, CRN-14, CRN-45                                         |
| **6** | **Offline-first architecture and synchronization** — Enable the PWA to operate fully offline, transition transparently between modes, and synchronize data reliably upon reconnection — including conflict resolution, partial failure recovery, offline ID generation, cache corruption detection, and offline-specific business rule enforcement.                                                               | US-076, REL-01, REL-02, USA-01, REL-04, CRN-21, CRN-34, CRN-36, CRN-38, CRN-05, CRN-16, CRN-39                                                         |
| **7** | **Financial reporting, integrations, and operational resilience** — Implement consolidated financial reporting, external system integrations (academic, CFDI), API versioning, fault isolation between modules, database migration strategy, and backup/recovery targets.                                                                                                                                         | PER-02, REL-03, MNT-01, MNT-02, USA-03, IOP-01, IOP-02, CRN-04, CRN-06, CRN-08, CRN-09, CRN-11, CRN-19                                                 |


---

## Iteration Details

### Iteration 1 — Establish Overall System Structure


| Aspect        | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| ------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Goal**      | Define the high-level decomposition of SICEB into containers and modules, establishing technology choices, interaction patterns, the multi-branch tenant model, cross-cutting technical conventions, and offline-aware design conventions that all subsequent iterations will refine and inherit.                                                                                                                                                                                                                                                                                                                                                    |
| **Rationale** | This is a greenfield system. No design decisions can be made about individual quality attributes or user stories until the fundamental architecture (layers, containers, module boundaries) is in place. All five technical constraints are addressed here since they shape the technology stack. Additionally, because offline synchronization (US-076) is the highest-ranked driver and a deep cross-cutting concern, mandatory design conventions must be established now so that modules built in Iterations 2–5 are inherently compatible with offline operation, avoiding costly retrofit when Iteration 6 designs the detailed sync protocol. |



| Driver | Type       | Why this iteration                                                                                                                                                     |
| ------ | ---------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| CRN-25 | Concern    | Defines the top-level containers: PWA, REST API, cloud DB, local storage                                                                                               |
| CRN-26 | Concern    | Allocates the 18 epics into cohesive, loosely coupled modules                                                                                                          |
| CRN-27 | Concern    | Establishes dependency rules to prevent circular dependencies between modules                                                                                          |
| CRN-29 | Concern    | Determines the single-deployment, tenant-isolated multi-branch model                                                                                                   |
| CRN-41 | Concern    | UTC timestamp convention must be established before any data is persisted                                                                                              |
| CRN-42 | Concern    | Fixed-precision currency handling must be in place before any financial entity is designed                                                                             |
| CRN-43 | Concern    | Offline-aware design conventions must be established before any domain module is built, so that Iterations 2–5 produce sync-compatible code without requiring retrofit |
| CON-01 | Constraint | PWA with Hybrid Cloud (SaaS) — shapes the entire technology stack                                                                                                      |
| CON-02 | Constraint | HTTPS / Secure WebSocket — determines communication infrastructure                                                                                                     |
| CON-03 | Constraint | Browser compatibility (last 2 versions of Chrome, Edge, Safari, Firefox) — constrains frontend choices                                                                 |
| CON-04 | Constraint | REST API for external integrations — shapes the backend's interface layer                                                                                              |
| CON-05 | Constraint | No DICOM/PACS; text-only lab results — bounds the data model scope                                                                                                     |


---

### Iteration 2 — Core Clinical Workflow and Medical Records


| Aspect                 | Description                                                                                                                                                                                                                                                                                                                                                                                                                               |
| ---------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Goal**               | Design the clinical care modules that represent the primary business value stream: patient management, medical consultations, prescribing, laboratory study tracking, and the immutable clinical record.                                                                                                                                                                                                                                  |
| **Business objective** | **Gestión de Clientes** — Maintain centralized digital records with complete care history.                                                                                                                                                                                                                                                                                                                                                |
| **Rationale**          | The clinic's core revenue comes from patient consultations. The clinical record is the most regulated artifact (NOM-004-SSA3-2012) and the most architecturally constrained (append-only, permanent retention). Addressing this early ensures the data model is correct before downstream modules (pharmacy, laboratory, payments) depend on it. This iteration covers 4 of the 10 primary user stories (US-024, US-025, US-026, US-031). |



| Driver | Type                | Why this iteration                                                                |
| ------ | ------------------- | --------------------------------------------------------------------------------- |
| US-024 | Primary US (rank 8) | Create clinical record — entry point for all patient care                         |
| US-025 | Primary US (rank 6) | Add consultation to record — the daily core operation; supports REL-01, USA-01    |
| US-026 | Primary US (rank 5) | Record immutability — must be enforced from the data model layer; supports REL-02 |
| US-031 | Primary US (rank 9) | Prescribe medications — consultations generate prescriptions; supports USA-01     |
| US-019 | US (HIGH)           | Register new patients with demographic information                                |
| US-020 | US (HIGH)           | Classify patients by type with automatic discount calculation                     |
| US-023 | US (HIGH)           | Validate guardian presence for minor patients                                     |
| US-027 | US (HIGH)           | View comprehensive medical history across services                                |
| US-038 | US (HIGH)           | Request laboratory studies during consultation                                    |
| US-040 | US (HIGH)           | View pending laboratory study requests                                            |
| US-041 | US (HIGH)           | Enter laboratory study results in text format                                     |
| US-042 | US (HIGH)           | View laboratory results in the patient's medical record                           |
| PER-03 | QA Scenario         | Patient search under 1 second over 50,000+ records — requires indexing strategy   |
| USA-02 | QA Scenario         | New resident onboarding — guided consultation flow supports daily operations      |
| AUD-03 | QA Scenario         | Medical record immutability — 100% of modification attempts blocked and logged    |
| CRN-02 | Concern             | Insert-only medical record schema must be designed with the initial data model    |
| CRN-01 | Concern             | Retention policies (permanent for clinical records) influence storage design      |
| CRN-31 | Concern             | NOM-004-SSA3-2012 compliance mandates specific record structure and sections      |
| CRN-37 | Concern             | System-wide unique patient identifier is foundational to the clinical workflow    |


---

### Iteration 3 — Security, Access Control, and Audit Infrastructure


| Aspect                 | Description                                                                                                                                                                                                                                                                                                                                                                                                          |
| ---------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Goal**               | Implement the security architecture: authentication, role-based access control with branch-scoped and residency-level permissions, API protection, personal data handling, and the centralized immutable audit trail.                                                                                                                                                                                                |
| **Business objective** | **Gestión de Personal** — Control over physicians, residents, and staff with role-appropriate access.                                                                                                                                                                                                                                                                                                                |
| **Rationale**          | Security is cross-cutting and must be layered on top of the structural foundation (Iteration 1) and the clinical data model (Iteration 2) before more modules are built. The audit trail is required by virtually every subsequent feature (pharmacy traceability, supply approval logs, regulatory reports). Residency-level restrictions (R1–R4) are critical for controlled substance enforcement in Iteration 5. |



| Driver | Type                    | Why this iteration                                                                 |
| ------ | ----------------------- | ---------------------------------------------------------------------------------- |
| US-003 | Primary US (rank 4)     | Role-based permissions — foundational to every user-facing module; supports SEC-02 |
| US-001 | US (HIGH)               | Create user accounts with role-based permissions                                   |
| US-002 | US (HIGH)               | Secure login with credentials                                                      |
| US-050 | US (HIGH)               | Validate residents can only perform actions allowed for their level (R1–R4)        |
| US-051 | US (HIGH)               | Block R1, R2, R3 residents from prescribing controlled medications                 |
| US-066 | US (HIGH)               | Register audit log entries for record access (LFPDPPP traceability)                |
| SEC-01 | QA Scenario             | Role-based access control — 100% of restricted actions blocked and logged          |
| SEC-02 | QA Scenario (High/High) | Branch-level data segmentation — zero unauthorized cross-branch access             |
| SEC-04 | QA Scenario             | REST API protection — 100% of unauthenticated requests rejected                    |
| MNT-03 | QA Scenario             | Admin-configurable roles — new roles operational in <30 min, zero code changes     |
| CRN-15 | Concern                 | RBAC for 11 roles with branch-scoped permissions                                   |
| CRN-13 | Concern                 | API hardening: HTTPS enforcement, error sanitization                               |
| CRN-17 | Concern                 | Centralized audit log consumed by Iterations 4–7; must exist first                 |
| CRN-18 | Concern                 | Audit log immutability — tamper-proof even for DBAs                                |
| CRN-32 | Concern                 | LFPDPPP personal data protection (consent, access rights)                          |


---

### Iteration 4 — Multi-Branch Operations and Inventory Management


| Aspect                 | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| ---------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Goal**               | Enable multi-branch operations: branch registration, active branch selection with context switching, branch-scoped inventory views, and real-time inventory updates across branches. Validate the multi-tenant scalability model. Adopt command/delta-based inventory mutations as the foundation for offline conflict resolution.                                                                                                                                                                                                                                                                                                                                                     |
| **Business objective** | **Gestión de Inventario** — Rigorous control of medical supplies, materials, and medications across all branches.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| **Rationale**          | With clinical workflows and security in place, this iteration activates the multi-branch dimension. Branch selection (US-074, rank 2) and branch registration (US-071, rank 3) are the second and third highest-ranked primary user stories. Inventory management (US-004, rank 7) directly supports daily clinical operations and is a prerequisite for pharmacy dispensation in Iteration 5. Inventory mutations are designed from the start as delta commands (CRN-44) following the offline-aware conventions from Iteration 1 (CRN-43), ensuring that when Iteration 6 introduces offline sync, inventory conflict resolution is deterministic and requires no data-layer rework. |



| Driver | Type                    | Why this iteration                                                                                        |
| ------ | ----------------------- | --------------------------------------------------------------------------------------------------------- |
| US-074 | Primary US (rank 2)     | Select active branch — context switch for multi-branch users; supports SEC-02, ESC-02                     |
| US-071 | Primary US (rank 3)     | Register new branch — enables network expansion; supports SEC-02, ESC-02                                  |
| US-004 | Primary US (rank 7)     | Complete inventory view for admin — total visibility of clinical resources; supports PER-01               |
| US-004 | US (HIGH)               | General Administrator sees complete inventory of ALL services                                             |
| US-005 | US (HIGH)               | Service Manager sees ONLY the inventory of their service                                                  |
| US-064 | US (HIGH)               | Configure tariffs by medical service with base price                                                      |
| PER-01 | QA Scenario (High/High) | Inventory updates reflected across all views in less than 2 seconds                                       |
| ESC-01 | QA Scenario             | New branch fully operational in less than 1 hour                                                          |
| ESC-02 | QA Scenario (High/High) | Growth from 3 to 15 branches with <10% performance degradation                                            |
| ESC-03 | QA Scenario             | Staff branch-context switch in less than 3 seconds without logout                                         |
| CRN-24 | Concern                 | Multi-tenant model must sustain network growth without performance degradation                            |
| CRN-35 | Concern                 | Inventory consistency under concurrent edits from multiple branches                                       |
| CRN-44 | Concern                 | Inventory mutations must be modeled as delta commands to enable deterministic offline conflict resolution |


---

### Iteration 5 — Pharmacy, Payments, and Regulatory Compliance


| Aspect                 | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| ---------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Goal**               | Design the pharmacy dispensation workflow with prescription validation, controlled substance traceability for COFEPRIS, payment registration, supply request approval workflows, and the asynchronous business compensation protocol for offline regulatory violations.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| **Business objective** | **Gestión Financiera** — Efficient registration of income and expenses per service.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| **Rationale**          | Pharmacy and payments are the second-highest business value stream after clinical care. Controlled substance traceability (COFEPRIS) is a hard regulatory constraint that must be designed into the dispensation flow, not added retroactively. This iteration depends on the clinical model (Iteration 2), RBAC and audit infrastructure (Iteration 3), and inventory (Iteration 4). Additionally, because CRN-14 and CRN-16 create scenarios where offline operations may violate regulatory rules that can only be detected upon synchronization (e.g., a deactivated resident who prescribed controlled medications while offline, or a dispensation against stale inventory), this iteration must define an explicit asynchronous compensation protocol (CRN-45) that generates mandatory priority alerts, audit entries, and supervisor review tasks — rather than silently failing with an HTTP error code at sync time. |



| Driver | Type                 | Why this iteration                                                                                                                   |
| ------ | -------------------- | ------------------------------------------------------------------------------------------------------------------------------------ |
| US-044 | Primary US (rank 10) | Register payments — tracks all clinic income; supports REL-01                                                                        |
| US-032 | US (HIGH)            | View patient prescriptions for dispensing                                                                                            |
| US-033 | US (HIGH)            | Validate prescription exists before dispensing                                                                                       |
| US-034 | US (HIGH)            | Verify inventory before dispensing medications                                                                                       |
| US-035 | US (HIGH)            | Register controlled medication dispensation with traceability                                                                        |
| US-044 | US (HIGH)            | Register payments for consultations, medications, and lab studies                                                                    |
| PER-04 | QA Scenario          | Full prescription validation (stock + prescriber permissions) in under 2 seconds                                                     |
| SEC-03 | QA Scenario          | 8-field immutable audit record for every controlled medication dispensation                                                          |
| AUD-01 | QA Scenario          | Supply request approval traceability (5 fields, report in <15 seconds)                                                               |
| AUD-02 | QA Scenario          | COFEPRIS audit report: 100% of controlled substance transactions, <30 seconds                                                        |
| USA-04 | QA Scenario          | Supply request creation in 3 steps / under 2 minutes with auto-complete                                                              |
| CRN-33 | Concern              | COFEPRIS controlled substance tracking across prescriptions, dispensations, and inventory                                            |
| CRN-14 | Concern              | Offline controlled medication dispensing — regulatory risk mitigation strategy                                                       |
| CRN-45 | Concern              | Asynchronous business compensation — define what happens when offline operations are rejected post-sync due to regulatory violations |


---

### Iteration 6 — Offline-First Architecture and Synchronization


| Aspect        | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| ------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Goal**      | Enable the PWA to operate fully offline, transition transparently between online and offline modes, and synchronize data reliably upon reconnection — including conflict resolution, partial failure recovery, offline ID generation, cache corruption detection, and offline-specific business rule enforcement.                                                                                                                                               |
| **Rationale** | Offline capability is SICEB's most architecturally challenging cross-cutting concern and the highest-ranked primary user story (US-076, rank 1). It is placed after the clinical, pharmacy, and inventory modules (Iterations 2–5) so that the sync protocol and conflict resolution strategies are grounded in real data flows rather than abstract assumptions. It addresses three of the six high/high quality attribute scenarios (REL-01, REL-02, USA-01). |



| Driver | Type                    | Why this iteration                                                                |
| ------ | ----------------------- | --------------------------------------------------------------------------------- |
| US-076 | Primary US (rank 1)     | Offline operation and synchronization — supports REL-01, REL-02, USA-01           |
| REL-01 | QA Scenario (High/High) | 100% of offline records synchronized with zero losses and zero duplicates         |
| REL-02 | QA Scenario (High/High) | Partial sync failure recovery — resume from exact cutoff point                    |
| USA-01 | QA Scenario (High/High) | Transparent mode transition in under 3 seconds with non-intrusive indicator       |
| REL-04 | QA Scenario             | Checksum-based cache corruption detection and forced re-download                  |
| CRN-21 | Concern                 | Core offline-first strategy: service workers, local storage, sync queue           |
| CRN-34 | Concern                 | Queue-based sync protocol with ordering guarantees and duplicate prevention       |
| CRN-36 | Concern                 | Caching strategy: what data is stored locally, invalidation, corruption detection |
| CRN-38 | Concern                 | UUID-based offline ID generation to prevent collisions during multi-branch sync   |
| CRN-05 | Concern                 | Backward compatibility for schemas when offline branches hold pre-migration data  |
| CRN-16 | Concern                 | Offline supervisor availability validation for R1/R2 residents                    |
| CRN-39 | Concern                 | Visual indicators for sync status, data freshness, and mode transitions           |


---

### Iteration 7 — Financial Reporting, Integrations, and Operational Resilience


| Aspect        | Description                                                                                                                                                                                                                                                                                                                     |
| ------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Goal**      | Implement consolidated financial reporting, external system integrations (academic institution, CFDI electronic invoicing), REST API versioning, fault isolation between modules, the database migration strategy, and backup/recovery targets.                                                                                 |
| **Rationale** | These drivers target operational maturity, future extensibility, and version 2.0 functionality. They are important but do not block the system's initial go-live. Addressing them last allows the team to focus on core clinical and operational value first, while ensuring the architecture accommodates them without rework. |



| Driver | Type        | Why this iteration                                                            |
| ------ | ----------- | ----------------------------------------------------------------------------- |
| PER-02 | QA Scenario | Consolidated financial report across 10 branches in under 10 seconds          |
| REL-03 | QA Scenario | Failure in one module does not affect critical care modules                   |
| MNT-01 | QA Scenario | New medical services added through configuration, zero code changes           |
| MNT-02 | QA Scenario | CFDI module integrates via existing payment interfaces without altering logic |
| USA-03 | QA Scenario | Responsive PWA fully functional on 10-inch tablets                            |
| IOP-01 | QA Scenario | REST API for external academic system to query resident training data         |
| IOP-02 | QA Scenario | New integration endpoints added without modifying existing ones               |
| CRN-04 | Concern     | Schema evolution strategy across 10+ incremental releases                     |
| CRN-06 | Concern     | Backup frequency, RPO/RTO targets for cloud DB and offline branch data        |
| CRN-08 | Concern     | CFDI integration with SAT web services via the payment module                 |
| CRN-09 | Concern     | CFDI issuance strategy during network outages                                 |
| CRN-11 | Concern     | REST API versioning to support current and future external consumers          |
| CRN-19 | Concern     | Unified exception management strategy (recoverable vs. fatal errors)          |


---

## Driver Coverage Matrix

All **10 primary user stories**, all **6 high/high quality attribute scenarios**, all **5 technical constraints**, all **35 high-priority concerns**, and all **24 quality attribute scenarios** are addressed across the 7 iterations.

### Primary User Stories (top-ranked US)


| Rank | Driver | Description                           | Iteration |
| ---- | ------ | ------------------------------------- | --------- |
| 1    | US-076 | Offline operation and synchronization | 6         |
| 2    | US-074 | Active branch selection               | 4         |
| 3    | US-071 | Branch registration                   | 4         |
| 4    | US-003 | Role-based permissions                | 3         |
| 5    | US-026 | Record immutability                   | 2         |
| 6    | US-025 | Add consultation to record            | 2         |
| 7    | US-004 | Complete inventory view (Admin)       | 4         |
| 8    | US-024 | Create clinical record                | 2         |
| 9    | US-031 | Prescribe medications                 | 2         |
| 10   | US-044 | Register payments                     | 5         |


### High/High Quality Attribute Scenarios


| Driver | Attribute   | Iteration |
| ------ | ----------- | --------- |
| PER-01 | Performance | 4         |
| SEC-02 | Security    | 3         |
| REL-01 | Reliability | 6         |
| REL-02 | Reliability | 6         |
| USA-01 | Usability   | 6         |
| ESC-02 | Scalability | 4         |


### Concerns by Category


| Category        | Drivers                                                | Iterations          |
| --------------- | ------------------------------------------------------ | ------------------- |
| Data Management | CRN-01, CRN-02, CRN-04, CRN-05, CRN-06                 | 2, 2, 7, 6, 7       |
| Integration     | CRN-08, CRN-09, CRN-11                                 | 7, 7, 7             |
| Security        | CRN-13, CRN-14, CRN-15, CRN-16, CRN-17, CRN-18, CRN-45 | 3, 5, 3, 6, 3, 3, 5 |
| Operational     | CRN-19, CRN-21, CRN-24                                 | 7, 6, 4             |
| Development     | CRN-25, CRN-26, CRN-27, CRN-43                         | 1, 1, 1, 1          |
| Business        | CRN-29                                                 | 1                   |
| Compliance      | CRN-31, CRN-32, CRN-33                                 | 2, 3, 5             |
| Synchronization | CRN-34, CRN-35, CRN-36, CRN-37, CRN-38, CRN-44         | 6, 4, 6, 2, 6, 4    |
| UX              | CRN-39                                                 | 6                   |
| Technical       | CRN-41, CRN-42                                         | 1, 1                |


### All Quality Attribute Scenarios


| Category         | Drivers                        | Iterations |
| ---------------- | ------------------------------ | ---------- |
| Performance      | PER-01, PER-02, PER-03, PER-04 | 4, 7, 2, 5 |
| Security         | SEC-01, SEC-02, SEC-03, SEC-04 | 3, 3, 5, 3 |
| Maintainability  | MNT-01, MNT-02, MNT-03         | 7, 7, 3    |
| Reliability      | REL-01, REL-02, REL-03, REL-04 | 6, 6, 7, 6 |
| Usability        | USA-01, USA-02, USA-03, USA-04 | 6, 2, 7, 5 |
| Scalability      | ESC-01, ESC-02, ESC-03         | 4, 4, 4    |
| Auditability     | AUD-01, AUD-02, AUD-03         | 5, 5, 2    |
| Interoperability | IOP-01, IOP-02                 | 7, 7       |


### Constraints


| Driver | Description                                             | Iteration |
| ------ | ------------------------------------------------------- | --------- |
| CON-01 | PWA with Hybrid Cloud (SaaS)                            | 1         |
| CON-02 | HTTPS / Secure WebSocket                                | 1         |
| CON-03 | Last 2 browser versions (Chrome, Edge, Safari, Firefox) | 1         |
| CON-04 | REST API for external integrations                      | 1         |
| CON-05 | No DICOM/PACS; text-only lab results                    | 1         |


