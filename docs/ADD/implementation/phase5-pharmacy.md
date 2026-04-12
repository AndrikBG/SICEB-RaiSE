# Phase 5 — Pharmacy, Payments, and Regulatory Compliance

> **Status:** 🔲 Not started  
> **Drivers:** US-044, US-032, US-033, US-034, US-035, US-044, PER-04, SEC-03, AUD-01, AUD-02, USA-04, CRN-33, CRN-14, CRN-45  
> **Depends on:** Phases 2, 3, and 4 complete

**Goal:** Implement pharmacy dispensing with prescription validation, COFEPRIS traceability, payment recording, supply approval workflow, and asynchronous compensation for regulatory violations discovered after offline sync.

---

## A5.1 — Pharmacy module

- [ ] **T5.1.1** Medication catalog with controlled-substance flag  
- [ ] **T5.1.2** Dispensing with prior prescription validation (US-033)  
- [ ] **T5.1.3** Inventory check before dispensing (US-034)  
- [ ] **T5.1.4** Full validation in under 2 seconds: stock + prescriber permissions (PER-04)  
- [ ] **T5.1.5** Inventory deduction via delta command on dispense  
- [ ] **T5.1.6** PWA views: pending prescriptions (US-032), dispensing form  

## A5.2 — COFEPRIS traceability

- [ ] **T5.2.1** Immutable eight-field record per controlled dispense (SEC-03)  
- [ ] **T5.2.2** COFEPRIS report: 100% controlled transactions in under 30 seconds (AUD-02)  
- [ ] **T5.2.3** End-to-end trace: prescription → dispense → inventory (CRN-33)  

## A5.3 — Billing & payments module

- [ ] **T5.3.1** Payment recording for consultations, medications, and laboratory (US-044)  
- [ ] **T5.3.2** Tariff and patient-type discount application  
- [ ] **T5.3.3** Receipt generation  
- [ ] **T5.3.4** CFDI integration placeholder (implementation in Phase 7)  
- [ ] **T5.3.5** PWA payment recording views  

## A5.4 — Supply chain module

- [ ] **T5.4.1** Supply requests with justification — three steps / under 2 minutes (USA-04)  
- [ ] **T5.4.2** Multi-step workflow: request → approval → delivery → confirmation  
- [ ] **T5.4.3** Approval traceability: five fields, report under 15 seconds (AUD-01)  
- [ ] **T5.4.4** Inventory update on receipt (delta command)  
- [ ] **T5.4.5** PWA supply request views  

## A5.5 — Asynchronous offline compensation (CRN-45)

- [ ] **T5.5.1** Compensation protocol for operations rejected after sync  
- [ ] **T5.5.2** Priority alerts for offline regulatory violations  
- [ ] **T5.5.3** Audit record for each compensation  
- [ ] **T5.5.4** Supervisor review tasks for compensated operations  

---

## Deliverables

- [ ] **E5.1** Pharmacy module — Catalog, dispensing, validation, delta deduction  
- [ ] **E5.2** COFEPRIS traceability — Eight fields, report &lt;30s, full chain  
- [ ] **E5.3** Billing & payments — Payments, tariffs, discounts, receipts, CFDI placeholder  
- [ ] **E5.4** Supply chain — Requests, workflow, traceability, inventory  
- [ ] **E5.5** Offline compensation — Alerts, audit, supervisor tasks  

---

## Notes and decisions

<!-- Record decisions, issues, and resolutions during this phase. -->
