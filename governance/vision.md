# Solution Vision: SICEB

> Sistema Integral de Control y Expedientes de Bienestar
> "Comprehensive Wellness Control and Records System"

---

> **Fuente de verdad narrativa:** [`docs/Requirements/Vision_Scope_SICEB.md`](../docs/Requirements/Vision_Scope_SICEB.md)
> **Stakeholders y contexto de negocio completo:** [`docs/Requirements/Vision_Scope_SICEB.md#5`](../docs/Requirements/Vision_Scope_SICEB.md)
> **Necesidades (NEC-01 a NEC-13):** [`docs/Requirements/Vision_Scope_SICEB.md#needs`](../docs/Requirements/Vision_Scope_SICEB.md)
> Este documento es un resumen operativo. Ante cualquier ambigüedad, la fuente de verdad es `docs/`.
> Para reconciliar drift ejecuta `/rai-docs-update`.

---

## Identity

### Description

SICEB is a healthcare management system for the **Private Wellness Integrated Clinics Network** — a private medical teaching institution operating multiple physical branches (sucursales) that offer general consultation, emergencies, internal medicine, pediatrics, gynecology and obstetrics, internal clinical laboratory, and internal pharmacy services.

The clinic operates as a **teaching institution**: attending physicians supervise medical residents at levels R1 through R4. Residents have role-specific restrictions enforced by the system — R1 residents require mandatory supervision and cannot prescribe controlled medications; R4 residents are practically autonomous.

**Current state (AS-IS):** The network operates with completely manual processes — supply requests via Word documents and email, paper medical records maintained separately per service with no cross-branch consolidation, manual financial report generation, and no digital enforcement of residency-level restrictions. This produces fragmented patient records, unmonitored inventory, slow reporting, and compliance gaps with NOM-004, LFPDPPP, and COFEPRIS.

**Future state (TO-BE):** SICEB replaces all manual processes with a centralized **Progressive Web App** backed by a REST API and PostgreSQL. It enforces Mexican healthcare regulations automatically, operates offline at branches during internet outages, and synchronizes data reliably when connectivity returns. A single deployment serves all branches with tenant-scoped data isolation via PostgreSQL Row-Level Security.

### Who Uses It

| Role | Primary Activities |
|------|--------------------|
| General Director | Executive financial and operational dashboards across all branches |
| General Administrator | Approve supply/workshop requests, manage inventory, manage users and branches, generate reports |
| Service Managers | Request supplies, register material usage, manage their service's medical records |
| Attending Physicians | Attend patients, create consultations, prescribe medications (including controlled), order lab studies, supervise residents |
| Residents R1–R4 | Attend patients under level-appropriate restrictions, participate in workshops |
| Lab Personnel | Process study requests, enter text-format results, manage reagent inventory |
| Pharmacy Personnel | Validate and dispense prescriptions, record controlled substance transactions |
| Admin/Reception Staff | Register patients, process payments, issue receipts and CFDI invoices, handle ARCO requests |
| Emergency Personnel | Triage patients (Red/Yellow/Green), register emergency consultations |

## Outcomes

| **Outcome** | **Description** | **Key Drivers** |
|-------------|-----------------|-----------------|
| **Centralized Immutable Patient Record** | One consolidated electronic medical record per patient accessible from any branch. Append-only event stream (`ClinicalEvent`) enforces NOM-004-SSA3-2012 immutability at the database level — no update or delete, permanent retention. Consultations, prescriptions, lab results, and attachments are always visible across services. | US-024, US-026, CRN-02, CRN-31 |
| **Residency-Level Safety Enforcement** | The system automatically blocks clinical actions that exceed a resident's training level. R1 cannot prescribe controlled medications under any circumstances. R1/R2 consultations require a documented supervisor. Every blocked action generates an immutable audit log entry. Enforcement at the service layer — not only the UI. | US-050, US-051, SEC-01, CRN-15, CRN-16 |
| **Operational Digitization** | Supply requests, workshop requests, and their approval/delivery workflows are fully digitized with complete audit trails. Managers create requests in the system; the Administrator approves/rejects with justification; automatic notifications flow at every step; inventory updates automatically on delivery confirmation. | NEC-01, NEC-02, US-009, US-015, AUD-01 |
| **Regulatory Compliance** | Full COFEPRIS traceability for controlled substances (8-field audit record per dispensation, immutable hash-chained log), NOM-004 immutable clinical records, LFPDPPP ARCO rights within 20 business days, NOM-024 informed consent management. Audit log tamper-evident: SHA-256 hash chain, INSERT-only DB role for audit tables. | CRN-17, CRN-18, CRN-31, CRN-32, CRN-33, CON-10, CON-11, CON-12 |
| **Financial Visibility and CFDI** | Automated income/expense/profitability reports per branch, service, and patient type. CFDI electronic invoicing integrated with SAT. Patient-type discounts (Students 30%, Workers 20%) applied automatically at payment time. Fixed-precision arithmetic on all monetary values — no floating-point errors. | NEC-05, NEC-06, NEC-07, CRN-42, CRN-08 |
| **Multi-Branch Scalability** | Single cloud deployment supports growth from 3 to 15+ branches with less than 10% performance degradation. New branch operational in under 1 hour. Branch context switch without logout in under 3 seconds. General Administrator has real-time cross-branch dashboard; local managers see only their branch. | NEC-13, ESC-01, ESC-02, ESC-03, CRN-29 |
| **Offline Resilience** | Critical modules (consultations, prescriptions, payments) operate fully offline using IndexedDB. Transparent mode transition in under 3 seconds. Upon reconnection: zero data loss, zero duplicates, partial failure resumption from exact cutoff. Delta-based inventory mutations enable deterministic conflict resolution when multiple branches modify the same supply offline. | US-076, REL-01, REL-02, USA-01, CRN-21, CRN-43, CRN-44 |

## Strategic Context

### 10 Incremental Deliveries (Scope)

| # | Delivery | Core Epics |
|---|----------|-----------|
| 1.0 | Access and base security | EP-01 |
| 2.0 | Inventory consultation | EP-02 (partial) |
| 3.0 | Supply request workflow | EP-03, EP-13 |
| 4.0 | Workshop request workflow | EP-04, EP-13 |
| 5.0 | Patient registration | EP-05 |
| 6.0 | Medical records (core) | EP-06, EP-11 |
| 7.0 | Pharmacy and Laboratory | EP-07, EP-08 |
| 8.0 | Payments and basic reports | EP-09, EP-10 partial |
| 9.0 | Material usage and complete inventory | EP-12, EP-02 complete |
| 10.0 | Advanced reports and final security | EP-10 complete, EP-14, EP-15 |

### Scope Boundaries

| In Scope | Out of Scope |
|----------|-------------|
| Electronic billing (CFDI) in version 1.0 | Native mobile applications (CON-01) |
| REST API for external integrations | Telemedicine / virtual consultations (CON-08) |
| Offline PWA operation | Academic evaluations for residents (CON-09) |
| Branch administration and multi-branch rotation | Diagnostic imaging (DICOM/PACS) — text-only lab results (CON-05) |

### Regulatory Framework

| Standard | Scope |
|----------|-------|
| **NOM-004-SSA3-2012** | Medical record structure, mandatory sections, permanence, immutability |
| **LFPDPPP** | Patient PII protection, ARCO rights (20-business-day response), audit trail of record access |
| **COFEPRIS** | Controlled substance traceability (prescriber, dispenser, patient, lot, quantity, date/time) |
| **NOM-024-SSA3-2012** | Signed informed consent management lifecycle |
| **SAT / CFDI** | Electronic invoice generation and cancellation via SAT web services |
