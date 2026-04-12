---
type: architecture_context
project: "SICEB"
status: active
tech_stack:
  backend: "Spring Boot 3.5 / Java 21 / PostgreSQL 17"
  frontend: "React 19 / TypeScript / Vite / Tailwind / Shadcn-ui"
  pwa: "Service Worker / IndexedDB (Dexie.js) / Background Sync"
  infra: "Docker Compose (dev) / GitHub Actions CI"
external_dependencies:
  - SAT (CFDI electronic invoicing)
  - Academic institution API (future)
  - Insurers API (future)
users:
  - General Director
  - General Administrator
  - Service Managers
  - Attending Physicians
  - Residents R1-R4
  - Lab Personnel
  - Pharmacy Personnel
  - Admin/Reception Staff
  - Emergency Personnel
governed_by:
  - NOM-004-SSA3-2012
  - LFPDPPP
  - COFEPRIS
  - NOM-024-SSA3-2012
---

# System Context: SICEB

---

> **Diagrama de contexto C4 original (Mermaid):** [`docs/ADD/Design/Architecture.md#arch-02-context`](../../docs/ADD/Design/Architecture.md)
> **Stakeholders completos con responsabilidades:** [`docs/Requirements/Vision_Scope_SICEB.md#stakeholders`](../../docs/Requirements/Vision_Scope_SICEB.md)
> **Interfaces externas (IOP-01, IOP-02):** [`docs/ADD/Design/Architecture.md#arch-08-interfaces`](../../docs/ADD/Design/Architecture.md)
> Para reconciliar drift ejecuta `/rai-docs-update`.

---

## Overview

SICEB is a Hybrid Cloud SaaS application for the Private Wellness Integrated Clinics Network. A single cloud deployment serves multiple physical branches. Each branch accesses the system via PWA in a web browser (desktop/tablet). Critical modules operate offline; data synchronizes when connectivity resumes. All inter-system communication uses HTTPS or Secure WebSocket.

## Context Diagram

```
┌────────────────────────────────────────────────────────┐
│              Private Wellness Clinics Network          │
│                                                        │
│  ┌─────────────┐   HTTPS/WSS   ┌──────────────────┐   │
│  │  Branch A   │◄─────────────►│                  │   │
│  │  (PWA)      │               │   SICEB Cloud    │   │
│  └─────────────┘               │   (Spring Boot   │   │
│                                │   + PostgreSQL)  │   │
│  ┌─────────────┐   HTTPS/WSS   │                  │   │
│  │  Branch N   │◄─────────────►│                  │   │
│  │  (PWA)      │               └──────┬───────────┘   │
│  └─────────────┘                      │               │
└──────────────────────────────────────┼────────────────┘
                                        │ REST API
                          ┌─────────────▼──────────────┐
                          │      External Systems       │
                          │  SAT (CFDI) · Academic ·   │
                          │  Insurers (future)          │
                          └────────────────────────────┘
```

## Users (11 Roles)

| Role | Branch Scope | Key Restrictions |
|------|-------------|-----------------|
| General Director | All branches (read) | Reports only |
| General Administrator | All branches | Full inventory, branch management, approvals |
| Service Manager | Own service/branch | Own service inventory, supply/workshop requests |
| Attending Physician | Assigned branches | No restrictions — full prescribing rights |
| Resident R4 | Assigned branches | Can prescribe controlled medications, supervise R1–R3 |
| Resident R3 | Assigned branches | No controlled medications; supervise R1–R2 |
| Resident R2 | Assigned branches | Limited prescribing; occasional supervision |
| Resident R1 | Assigned branches | Mandatory supervision; cannot prescribe controlled medications |
| Lab Personnel | Own branch | Lab module only |
| Pharmacy Personnel | Own branch | Pharmacy module only |
| Admin/Reception Staff | Own branch | Patient registration, payments |

## External Interfaces

| System | Direction | Protocol | Description |
|--------|-----------|----------|-------------|
| SAT (Servicio de Administración Tributaria) | Outbound | HTTPS/SOAP | CFDI electronic invoice generation and cancellation (CON-07, CRN-08) |
| Academic Institution System | Inbound/Outbound | REST | Query resident training activity data (IOP-01) |
| Insurers | Outbound | REST | Future integration for billing (IOP-02) |
| Branch PWA clients | Bidirectional | HTTPS + Secure WebSocket | Primary user interface, real-time inventory updates, offline sync |
