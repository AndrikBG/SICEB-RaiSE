# Phase 7 — Reporting, Integrations, and Operational Resilience

> **Status:** 🔲 Not started  
> **Drivers:** PER-02, REL-03, MNT-01, MNT-02, USA-03, IOP-01, IOP-02, CRN-04, CRN-06, CRN-08, CRN-09, CRN-11, CRN-19  
> **Depends on:** Phases 5 and 6 complete

**Goal:** Implement consolidated financial reporting, external integrations (CFDI, academic), API versioning, fault isolation, DB migration strategy, and backup/recovery objectives.

---

## A7.1 — Reporting module

- [ ] **T7.1.1** Consolidated cross-branch financial reports in under 10 seconds (PER-02)  
- [ ] **T7.1.2** Operational dashboards per branch and consolidated  
- [ ] **T7.1.3** Read-only access to Billing, Inventory, and Clinical Care  

## A7.2 — CFDI integration

- [ ] **T7.2.1** Integration with SAT web services (CRN-08) via payment interfaces (MNT-02)  
- [ ] **T7.2.2** CFDI issuance strategy during network outages (CRN-09)  
- [ ] **T7.2.3** CFDI voucher storage and lookup  

## A7.3 — External integrations

- [ ] **T7.3.1** REST API for external academic system: resident training data (IOP-01)  
- [ ] **T7.3.2** Training module: workshops, approval, attendance  
- [ ] **T7.3.3** New endpoints without modifying existing ones (IOP-02)  

## A7.4 — API versioning and extensibility

- [ ] **T7.4.1** REST API versioning strategy (CRN-11)  
- [ ] **T7.4.2** New medical services via configuration, no code (MNT-01)  
- [ ] **T7.4.3** Scheduling module: appointments, physician agenda, cancellations  

## A7.5 — Operational resilience

- [ ] **T7.5.1** Fault isolation between modules (REL-03)  
- [ ] **T7.5.2** DB migrations for 10+ incremental releases (CRN-04)  
- [ ] **T7.5.3** Backup frequency, RPO, and RTO (CRN-06)  
- [ ] **T7.5.4** Unified exception handling: recoverable vs fatal (CRN-19)  
- [ ] **T7.5.5** [Carried] Strict PostgreSQL privilege separation: `siceb_migration` vs `siceb_app` roles for INSERT-only immutability  

## A7.6 — Tablet responsiveness

- [ ] **T7.6.1** PWA functional on 10-inch tablets (USA-03)  
- [ ] **T7.6.2** Usability testing on real devices  

---

## Deliverables

- [ ] **E7.1** Reporting — Cross-branch financials &lt;10s, dashboards  
- [ ] **E7.2** CFDI — SAT invoicing, offline strategy, vouchers  
- [ ] **E7.3** Integrations — Academic API, Training module  
- [ ] **E7.4** Versioned API — Versioning, config-driven services, Scheduling  
- [ ] **E7.5** Resilience — Isolation, migrations, backup RPO/RTO, exceptions  
- [ ] **E7.6** Tablets — Full validation on 10-inch  
- [ ] **E7.7** [Carried] PostgreSQL hardening — Segmented transactional roles (INSERT-only)  

---

## Notes and decisions

<!-- Record decisions, issues, and resolutions during this phase. -->
