# Backlog: SICEB

> **Status**: Active — ADD Iteration Plan defines sequencing

---

> **Plan de iteraciones ADD completo (7 iteraciones, driver coverage matrix):** [`docs/ADD/Design/IterationPlan.md`](../docs/ADD/Design/IterationPlan.md)
> **Detalle de cada iteración:** [`docs/ADD/Design/Iteration1.md`](../docs/ADD/Design/Iteration1.md) · [`Iteration2.md`](../docs/ADD/Design/Iteration2.md) · [`Iteration3.md`](../docs/ADD/Design/Iteration3.md) · [`Iteration4.md`](../docs/ADD/Design/Iteration4.md)
> **Épicas de negocio originales (EP-01 a EP-18):** [`docs/Requirements/Vision_Scope_SICEB.md#epics`](../docs/Requirements/Vision_Scope_SICEB.md)
> Para reconciliar drift ejecuta `/rai-docs-update`.

---

## Epics

| ID | Epic | Status | Scope | Priority |
|----|------|--------|-------|----------|
| E1 | Authentication and Access Control (EP-01, EP-14) | In Progress | IAM module: 11 roles, branch-scoped permissions, JWT auth, token deny-list, Spring Security | High |
| E2 | Core Clinical Workflow and Medical Records (EP-05, EP-06, EP-11) | In Progress | Patient registration, immutable medical records, consultation recording, resident level restrictions, prescriptions, lab study tracking | High |
| E3 | Security Infrastructure and Audit Trail (EP-14) | Planned | RBAC enforcement, immutable audit log, LFPDPPP personal data handling, ARCO rights | High |
| E4 | Multi-Branch Operations and Inventory (EP-02, EP-18) | **In Progress** | Branch registration, active-branch context switching, branch-scoped inventory, delta-command mutations, real-time updates | High |
| E5 | Pharmacy, Payments, and Regulatory Compliance (EP-07, EP-09) | Planned | Prescription validation, COFEPRIS controlled substance traceability, dispensation workflow, payment registration, CFDI invoicing, supply request workflows (EP-03), workshop workflows (EP-04) | High |
| E6 | Offline-First Architecture and Synchronization (EP-17) | Planned | Service worker, IndexedDB sync queue, conflict resolution, partial failure recovery, offline ID generation, cache corruption detection, transparent mode transition | High |
| E7 | Financial Reporting, Integrations, and Operational Resilience (EP-10, EP-15, EP-16) | Planned | Consolidated financial reports, external API (academic, insurers), CFDI SAT integration, REST API versioning, fault isolation, backup/recovery strategy | Medium |

## Story Naming Convention

`story/s{epic}.{N}/{short-name}` — e.g., `story/s2.1/patient-registration`

## Iteration Mapping (ADD)

| ADD Iteration | Epic(s) | Key Drivers |
|---------------|---------|-------------|
| 1 | Foundation (cross-cutting) | CRN-25, CRN-26, CRN-27, CRN-41–43, CON-01–05 |
| 2 | E2 | US-024, US-025, US-026, US-031, PER-03, AUD-03, CRN-02, CRN-31 |
| 3 | E1, E3 | US-003, SEC-01, SEC-02, SEC-04, CRN-15, CRN-17, CRN-18, CRN-32 |
| 4 | E4 | US-071, US-074, US-004, PER-01, ESC-01, ESC-02, CRN-24, CRN-35, CRN-44 |
| 5 | E5 | US-044, US-032–035, PER-04, SEC-03, AUD-01, AUD-02, CRN-33, CRN-45 |
| 6 | E6 | US-076, REL-01, REL-02, USA-01, REL-04, CRN-21, CRN-34, CRN-36, CRN-38 |
| 7 | E7 | PER-02, REL-03, MNT-01, MNT-02, IOP-01, IOP-02, CRN-04, CRN-08, CRN-11 |
