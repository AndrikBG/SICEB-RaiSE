# Project Constraints — SICEB

Constraints are design decisions that have already been made and are not negotiable. They limit the architect's design space.

---

## Technical Constraints

| ID | Constraint |
|---|---|
| **CON-01** | The system must be developed as a **Progressive Web App (PWA)** with Hybrid Cloud (SaaS) architecture; native mobile applications are excluded |
| **CON-02** | All communication between clients and the cloud server must use **HTTPS / Secure WebSocket** protocols |
| **CON-03** | The frontend must support the **latest two versions** of Chrome, Edge, Safari, and Firefox on desktop and tablet devices |
| **CON-04** | The backend must expose a **REST API** for all external integrations |
| **CON-05** | **No diagnostic imaging (DICOM/PACS)** is supported; laboratory results are captured in text format only |

---

## Business Constraints

| ID | Constraint |
|---|---|
| **CON-06** | The project must be delivered in **10 incremental releases**, prioritized by business value |
| **CON-07** | **Electronic invoicing (CFDI)** must be included in version 1.0, in addition to simple payment receipts |
| **CON-08** | **No telemedicine** (virtual consultations) is included in the project scope |
| **CON-09** | **No academic evaluations** for residents are included; only registration of participation in training activities |
| **CON-10** | The system must comply with **NOM-024-SSA3-2012** for the management of signed informed consents |
| **CON-11** | The system must comply with **LFPDPPP** (Federal Law for Protection of Personal Data), including ARCO rights processing within 20 business days and full audit trail of record access |
| **CON-12** | The system must support **COFEPRIS** audit requirements for controlled medication traceability (prescriber, dispenser, patient, medication, lot, quantity, date/time) |
