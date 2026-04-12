# Iteration 1 — Establish Overall System Structure

## Goal

Define the high-level decomposition of SICEB into containers and modules, establishing technology choices, interaction patterns, the multi-branch tenant model, cross-cutting technical conventions, and offline-aware design conventions that all subsequent iterations will refine and inherit.

This is a greenfield system. No design decisions can be made about individual quality attributes or user stories until the fundamental architecture (layers, containers, module boundaries) is in place. All five technical constraints are addressed here since they shape the technology stack. Additionally, because offline synchronization (US-076) is the highest-ranked driver and a deep cross-cutting concern, mandatory design conventions must be established now so that modules built in Iterations 2–5 are inherently compatible with offline operation, avoiding costly retrofit when Iteration 6 designs the detailed sync protocol.

---

## Step 2: Iteration Drivers

### Architectural Concerns

| Driver | Description | Why this iteration |
|---|---|---|
| **CRN-25** | Overall system structure | Defines the top-level containers: PWA, REST API, cloud DB, local storage |
| **CRN-26** | Functionality-to-module allocation | Allocates the 18 epics into cohesive, loosely coupled modules |
| **CRN-27** | Dependency management | Establishes dependency rules to prevent circular dependencies between modules |
| **CRN-29** | Multi-branch architecture | Determines the single-deployment, tenant-isolated multi-branch model |
| **CRN-41** | UTC timestamps | Convention must be established before any data is persisted |
| **CRN-42** | Currency handling | Fixed-precision arithmetic must be in place before any financial entity is designed |
| **CRN-43** | Offline-aware design conventions | Must be established before any domain module is built, so that Iterations 2–5 produce sync-compatible code without requiring retrofit |

### Technical Constraints

| Driver | Description | Why this iteration |
|---|---|---|
| **CON-01** | PWA with Hybrid Cloud (SaaS) | Shapes the entire technology stack; native mobile apps are excluded |
| **CON-02** | HTTPS / Secure WebSocket | Determines communication infrastructure between all clients and server |
| **CON-03** | Browser compatibility (last 2 versions of Chrome, Edge, Safari, Firefox) | Constrains frontend framework and API choices |
| **CON-04** | REST API for external integrations | Shapes the backend's interface layer |
| **CON-05** | No DICOM/PACS; text-only lab results | Bounds the data model scope — no binary medical imaging |

---

## Step 3: Elements to Refine

| Element | Current State | Refinement Action |
|---|---|---|
| **SICEB (whole system)** | Undecomposed black-box — only the context diagram exists, showing external actors and communication protocols. | **Top-down decomposition** into containers (PWA frontend, REST API backend, cloud database, local offline storage) and into internal modules (mapping the 18 epics to cohesive, loosely coupled components). |

Since this is Iteration 1 of a greenfield development, the only element to refine is the entire SICEB system.

---

## Step 4: Design Concepts

### Architectural Patterns

| Design Concept | Pros | Cons | Discarded Alternatives |
|---|---|---|---|
| **Three-Tier Web Architecture** — PWA as the presentation tier, REST API as the application tier, cloud database as the data tier. *Addresses: CRN-25, CON-01, CON-04* | Well-understood separation of concerns; each tier scales independently; PWA clients are stateless and cacheable; REST API provides a single integration surface | Network latency between tiers; API tier is a single point of failure unless load-balanced; requires API versioning discipline | **Two-Tier (client ↔ DB direct):** No security boundary, exposes DB credentials to client. **Serverless-first:** Vendor lock-in, cold-start latency incompatible with real-time clinical workflows |
| **Modular Monolith** — Single deployable unit internally organized into well-bounded domain modules with explicit public interfaces. *Addresses: CRN-26, CRN-27* | Simple deployment and operations for a small team; easier cross-module transactions; module boundaries can evolve toward microservices; single process debugging | Scaling is all-or-nothing; a failure in one module can cascade; requires discipline to maintain boundaries | **Microservices:** Excessive operational complexity for a small team, distributed transaction overhead. **Unstructured Monolith:** No module boundaries, prevents future decomposition |
| **Domain-Driven Module Decomposition** — Map the 18 epics into cohesive modules organized around business domains. *Addresses: CRN-26, CRN-27* | High cohesion within modules; clear ownership and team alignment; prevents circular dependencies through dependency inversion | Requires careful boundary identification; cross-cutting concerns span multiple modules; initial overhead in defining interfaces | **Technical-Layer Decomposition:** Low cohesion — a single feature change touches every layer. **Feature-based flat decomposition:** Overlapping boundaries, no explicit dependency direction |
| **Shared Database with Tenant Discriminator Column** — All branches share one database; every tenant-scoped table includes a `branch_id` column. *Addresses: CRN-29* | Low operational cost; simplified cross-branch reporting; easier schema migrations; straightforward backup strategy | Row-level security must be rigorously enforced; noisy-neighbor risk; all branches share connection pool | **Database-per-tenant:** High operational overhead. **Schema-per-tenant:** Complex migrations across N schemas |

### Externally Developed Components

| Design Concept | Pros | Cons | Discarded Alternatives |
|---|---|---|---|
| **SPA Framework with PWA Capabilities** — Service Worker registration, Web App Manifest, and IndexedDB for local storage. *Addresses: CON-01, CON-03* | Native-like user experience; installable on desktops and tablets; Service Workers enable offline caching and background sync | Initial bundle size requires optimization; complex state management when blending online and offline data | **Server-Side Rendered MPA:** Poor offline support, full page reloads. **Native Mobile Apps:** Explicitly excluded by CON-01 |
| **Cloud-Hosted SaaS Deployment** — Single cloud deployment serving all branches through a unified URL. *Addresses: CON-01, CON-02* | Centralized management and updates; TLS handled at infrastructure level; auto-scaling available; single deployment pipeline | Internet dependency for online operations; vendor-specific configuration | **On-Premises per Branch:** High operational burden. **Self-hosted VMs:** Requires sysadmin expertise the clinic likely lacks |
| **HTTPS + REST API with JSON** — All client-server communication over TLS 1.2+. WebSocket channel reserved for real-time push. *Addresses: CON-02, CON-04* | Industry standard; stateless and cacheable; excellent tooling; WebSocket supplement enables real-time without polling | REST alone is request-response; WebSocket adds connection management complexity | **GraphQL:** Steeper learning curve, harder to cache. **gRPC-Web:** Limited browser support, requires proxy |

### Tactics

| Design Concept | Pros | Cons | Discarded Alternatives |
|---|---|---|---|
| **UTC Timestamps with Presentation-Layer Conversion** — All timestamps stored in UTC; conversion to `America/Mexico_City` at the UI only. *Addresses: CRN-41* | Eliminates timezone ambiguity during sync; consistent chronological ordering; simplifies audit log correlation | Developers must consistently apply the convention; debugging requires mental UTC-to-local translation | **Local Timezone Storage:** Creates sync conflicts during DST transitions. **Dual Storage (UTC + local):** Data redundancy, risk of drift |
| **Fixed-Precision Decimal for Currency** — `DECIMAL(19,4)` in the database, `BigDecimal` equivalent in code, banker's rounding. *Addresses: CRN-42* | Zero floating-point rounding errors; accurate tax and discount calculations; auditable financial results | Slightly more verbose arithmetic code; serialization requires explicit format control | **IEEE 754 float/double:** Accumulates rounding errors. **Integer Cents:** Loses sub-cent precision for IVA proration |
| **Offline-Aware Design Conventions** — Four mandatory rules for all modules: (1) UUID-only identifiers via `EntityId` — no auto-increment sequences; (2) every write operation must be idempotent — retrying the same command produces the same result; (3) business validations must be executable against locally cached data (role, permissions, residency level travel in the JWT or are cached at login); (4) shared-state mutations (inventory) must be modeled as intent-based delta commands, not absolute state transfers. *Addresses: CRN-43* | Ensures Iterations 2–5 produce modules inherently compatible with offline sync; eliminates costly data-layer retrofit in Iteration 6; delta commands enable deterministic conflict resolution for concurrent branch operations | Developers must internalize and follow conventions consistently; requires code review enforcement; delta-based mutations add slight complexity to inventory read-model reconstruction | **No conventions (design sync later):** High risk of expensive retrofit — business validations, ID generation, and inventory patterns built for online-only would need rewriting. **Full offline implementation in Iteration 1:** Premature — no domain models exist yet to validate sync strategies against |

---

## Step 5: Instantiation Decisions

| Instantiation Decision | Rationale |
|---|---|
| **Container: SICEB PWA Client** — SPA delivered as a PWA with Web App Manifest, Service Worker, and IndexedDB. Targets last 2 versions of Chrome, Edge, Safari, Firefox on desktop and tablet. | Instantiates the Presentation Tier and SPA+PWA concept. Satisfies **CON-01**, **CON-03**, and prepares for offline-first (CRN-21, Iteration 6). |
| **Container: SICEB API Server** — Modular monolith exposing REST API over HTTPS. Single deployable artifact on cloud PaaS. OpenAPI-documented endpoints. | Instantiates the Application Tier, Modular Monolith, and REST+HTTPS concepts. Satisfies **CRN-25**, **CON-02**, **CON-04**. |
| **Container: SICEB Cloud Database** — Single PostgreSQL instance on managed cloud service. All tenant-scoped tables include `branch_id`. Row-level filtering at the repository layer. | Instantiates the Data Tier and Shared Database with Tenant Discriminator. Satisfies **CRN-29**. |
| **Container: SICEB Local Storage** — IndexedDB in each PWA client, mirroring a branch-scoped subset of cloud data. Managed by Service Worker and sync queue manager. | Instantiates the local data store for offline operation. Satisfies **CRN-25** and prepares for CRN-21, CRN-36 (Iteration 6). |
| **Domain Module: Clinical Care** — Patients, medical records (append-only), consultations, vital signs, attachments. | Groups the highest business-value functionality into a single cohesive module. Addresses **CRN-26**. |
| **Domain Module: Prescriptions** — Prescription creation, items, prescriber validation, status lifecycle. Depends on Clinical Care. | Separated from Clinical Care to isolate prescriber validation rules. Addresses **CRN-26**, **CRN-27**. |
| **Domain Module: Pharmacy** — Medication catalog, dispensation, controlled substance traceability. Depends on Prescriptions and Inventory. | Isolates regulatory-sensitive COFEPRIS traceability. Addresses **CRN-26**, **CRN-27**. |
| **Domain Module: Laboratory** — Study requests, prepayment verification, text-only results. Depends on Clinical Care and Billing. | Text-only results per **CON-05**. Addresses **CRN-26**. |
| **Domain Module: Inventory** — Branch-scoped stock tracking, thresholds, expiration, low-stock alerts. No outgoing domain dependencies. | Designed as a dependency leaf to prevent cycles. Addresses **CRN-26**, **CRN-27**. |
| **Domain Module: Supply Chain** — Supply requests, approval workflow, delivery, confirmation. Depends on Inventory. | Isolates the multi-step approval workflow from inventory tracking. Addresses **CRN-26**. |
| **Domain Module: Scheduling** — Appointments, physician agendas, cancellations. Depends on Clinical Care. | Distinct bounded context with its own lifecycle rules. Addresses **CRN-26**. |
| **Domain Module: Billing & Payments** — Payments, tariffs, receipts, future CFDI. Uses `DECIMAL(19,4)`. Depends on Clinical Care, Pharmacy, Laboratory. | Centralizes all financial logic. Addresses **CRN-26**, **CRN-42**. |
| **Domain Module: Reporting** — Financial and operational reports. Read-only queries across Billing, Inventory, Clinical Care. | Separated from Billing to isolate read-heavy reporting. Addresses **CRN-26**. |
| **Domain Module: Training** — Workshops, approval workflow, attendance. Standalone. | Self-contained domain with minimal dependencies. Addresses **CRN-26**. |
| **Platform Module: Identity & Access** — Authentication, authorization, RBAC, user and role management. All domain modules depend on this. | Cross-cutting security extracted as a platform module. Addresses **CRN-26**, **CRN-27**. |
| **Platform Module: Branch Management** — Branch CRUD, active branch selection, `branch_id` injection into request context. | Instantiates the tenant discriminator enforcement mechanism. Addresses **CRN-29**. |
| **Platform Module: Audit & Compliance** — Centralized append-only audit log. Write-only sink — domain modules push events, Audit never calls back. | Zero reverse dependencies eliminates cycle risk. Addresses **CRN-26**, **CRN-27**. |
| **Platform Module: Synchronization** — Offline sync queue, conflict detection, data reconciliation. Detailed design deferred to Iteration 6. | Isolated as a platform concern because it interacts with every domain module's data. Addresses **CRN-26**. |
| **Shared Kernel** — `Money(DECIMAL(19,4), MXN)`, `UtcDateTime` (always UTC), `EntityId` (UUID — no auto-increment permitted), `IdempotencyKey` (client-generated key for safe command retry), standardized error codes. | Instantiates CRN-41, CRN-42, and supports CRN-43 rules (1) and (2) as reusable value objects. |
| **Dependency Rule: Acyclic Module Graph** — Domain modules depend only on Platform modules and Shared Kernel. Cross-domain dependencies follow Clinical Care → Prescriptions → Pharmacy → Inventory. | Directly instantiates **CRN-27**. Enforced at build time and review time. |
| **Enforcement: Automated Architecture Tests in CI** — The CI pipeline must include static architecture tests (e.g., ArchUnit or equivalent) that automatically reject any pull request introducing auto-increment primary keys, direct database queries in business validation logic, cyclic inter-module dependencies, or non-idempotent write endpoints. | Prevents erosion of CRN-43 conventions during Iterations 2–5 without relying solely on manual code review. Supports **CRN-27** and **CRN-43**. |
| **Offline-Aware Design Convention: UUID-Only Identifiers** — All entity identifiers use `EntityId` (UUID v7) from the Shared Kernel. No database auto-increment sequences are permitted for any entity primary key. | Prevents ID collisions when multiple branches create records offline and sync simultaneously. Instantiates rule (1) of **CRN-43**. |
| **Offline-Aware Design Convention: Idempotent Write Operations** — Every command handler and API write endpoint must be idempotent: re-submitting the same request (identified by a client-generated idempotency key) produces the same result without side effects. | Enables safe retry during sync — partial failures can be resumed without creating duplicates. Instantiates rule (2) of **CRN-43**. Prepares for REL-02 (Iteration 6). |
| **Offline-Aware Design Convention: Local-First Business Validations** — Business rule validations (role permissions, residency level restrictions, prescriber authorization) must execute against user context data available in the JWT token and locally cached reference data, not against live database queries. | Ensures RBAC and regulatory validations (US-050, US-051) function identically online and offline without code changes. Instantiates rule (3) of **CRN-43**. Prepares for CRN-16 (Iteration 6). |
| **Offline-Aware Design Convention: Delta-Based Inventory Mutations** — All inventory state changes are recorded as intent-based delta commands (e.g., `DecrementStock(item, quantity, branch, timestamp)`) rather than absolute state transfers (e.g., `SetStock(item, 13)`). The current stock is a materialized view derived from applying the ordered sequence of deltas. | Enables deterministic conflict resolution when multiple branches modify the same supply concurrently while offline. Instantiates rule (4) of **CRN-43**. Prepares for CRN-35 (Iteration 4) and CRN-44. |

---

## Step 6: Views, Interfaces, and Design Decisions

### Diagrams Created

| Diagram | Section in Architecture.md | Description |
|---|---|---|
| Context Diagram | Section 2 | Imported from Architectural Drivers — shows SICEB as a single system with external actors |
| C4 Container Diagram | Section 5 | Four containers (PWA Client, API Server, Cloud Database, Local Storage) with protocols and external actors |
| API Server Component Diagram | Section 6.1 | 10 domain modules, 4 platform modules, Shared Kernel with acyclic dependency graph |
| PWA Client Component Diagram | Section 6.2 | 5 internal components (UI, State Management, API Client, Service Worker, Local Storage Manager) |
| SD-01: Authenticated API Request Flow | Section 7 | Standard request lifecycle showing authentication, tenant context injection, and query scoping |
| SD-02: Branch Context Selection | Section 7 | Login, branch selection, and tenant isolation establishment flow |

### Design Decisions

| Driver | Decision | Rationale | Discarded Alternatives |
|---|---|---|---|
| **CRN-25** | Decompose SICEB into four containers: PWA Client, API Server, Cloud Database, Local Storage | Three-tier architecture provides clear separation of concerns; each tier can evolve independently; Local Storage container enables offline operation | Two-tier with direct DB access; Serverless-first |
| **CRN-26** | Organize the API Server into 10 domain modules, 4 platform modules, and a Shared Kernel following domain-driven decomposition | High cohesion within modules aligned with business domains; clear ownership boundaries; modules can be independently tested and maintained | Technical-layer decomposition; Unstructured monolith |
| **CRN-27** | Enforce an acyclic directed dependency graph: Clinical Care → Prescriptions → Pharmacy → Inventory | Prevents circular dependencies; dependency direction mirrors business process flow; enforceable at build time | Unrestricted module access; Event-only coupling |
| **CRN-29** | Shared database with `branch_id` tenant discriminator column; single deployment; row-level filtering at the repository layer | Low operational cost; simplified migrations; enables cross-branch reporting; aligns with small-team capacity | Database-per-tenant; Schema-per-tenant |
| **CRN-41** | Store all timestamps in UTC using `UtcDateTime` value type; convert to `America/Mexico_City` at the UI only | Eliminates timezone ambiguity during offline sync; consistent audit log ordering | Local timezone storage; Dual storage |
| **CRN-42** | Use `DECIMAL(19,4)` and `Money` value type with banker's rounding | Zero floating-point errors; matches tax authority precision for IVA proration | IEEE 754 float/double; Integer cents |
| **CON-01** | Build frontend as a PWA with Service Worker, Web App Manifest, and IndexedDB | Installable; offline-capable; single codebase for all platforms | Native mobile apps; Server-rendered MPA |
| **CON-02** | TLS 1.2+ at cloud load balancer; all endpoints require HTTPS; WebSocket secured with WSS | Industry-standard transport security; centralized certificate management | Application-level encryption only; Self-signed certificates |
| **CON-03** | Target last 2 versions of Chrome, Edge, Safari, Firefox; standard Web APIs with progressive enhancement | Consistent experience on clinic devices; avoids polyfill overhead | Support all browser versions; Single-browser target |
| **CON-04** | REST API with JSON payloads documented via OpenAPI; versioned endpoints | Standard interface; stateless and cacheable; excellent tooling | GraphQL; gRPC |
| **CON-05** | Laboratory results stored as text fields only; no binary imaging pipeline | Simplifies data model; avoids PACS complexity and storage costs | DICOM/PACS support; File-based image storage |
| **CRN-43** | Establish four mandatory offline-aware design conventions: (1) UUID-only identifiers, (2) idempotent write operations, (3) local-first business validations, (4) delta-based inventory mutations | Ensures all modules built in Iterations 2–5 are inherently compatible with offline sync; eliminates the risk of costly data-layer retrofit in Iteration 6; the conventions are lightweight enough to adopt now without premature sync implementation | No conventions — defer all offline concerns to Iteration 6; Full offline implementation in Iteration 1 — premature without domain models |

---

## Step 7: Analysis of Design and Iteration Goal Achievement

| Driver | Analysis Result |
|---|---|
| **CRN-25** — Overall system structure | **Satisfied.** Decomposed into four containers with defined responsibilities, technologies, and protocols. |
| **CRN-26** — Functionality-to-module allocation | **Satisfied.** 10 domain modules, 4 platform modules, and Shared Kernel with documented responsibilities. |
| **CRN-27** — Dependency management | **Satisfied.** Acyclic directed dependency graph defined and documented in the component diagram. |
| **CRN-29** — Multi-branch architecture | **Satisfied.** Single-deployment, tenant-isolated model with `branch_id` discriminator. SD-02 illustrates the flow. |
| **CRN-41** — UTC timestamps | **Satisfied.** `UtcDateTime` value type defined in Shared Kernel. Convention documented in design decisions. |
| **CRN-42** — Currency handling | **Satisfied.** `Money` value type with `DECIMAL(19,4)` and banker's rounding defined in Shared Kernel. |
| **CON-01** — PWA with Hybrid Cloud / SaaS | **Satisfied.** PWA Client container defined with Service Worker, Manifest, and IndexedDB. |
| **CON-02** — HTTPS / Secure WebSocket | **Satisfied.** TLS 1.2+ at load balancer; all communication uses HTTPS/REST and WSS. |
| **CON-03** — Browser compatibility | **Satisfied.** Targets last 2 versions of Chrome, Edge, Safari, Firefox with progressive enhancement. |
| **CON-04** — REST API for external integrations | **Satisfied.** REST API with JSON and OpenAPI documentation. External systems connect through this interface. |
| **CON-05** — No DICOM/PACS; text-only lab results | **Satisfied.** Laboratory module stores text-only results. Binary imaging explicitly excluded. |
| **CRN-43** — Offline-aware design conventions | **Satisfied.** Four mandatory conventions established: UUID-only identifiers, idempotent writes, local-first validations, and delta-based inventory mutations. Documented as instantiation decisions and enforced via code review. |

### Summary

| Status | Count | Drivers |
|---|---|---|
| **Satisfied** | 12 | CRN-25, CRN-26, CRN-27, CRN-29, CRN-41, CRN-42, CRN-43, CON-01, CON-02, CON-03, CON-04, CON-05 |
| **Partially Satisfied** | 0 | — |
| **Not Satisfied** | 0 | — |

All 12 drivers for Iteration 1 have been satisfied. The foundational system structure and offline-aware design conventions are now in place for subsequent iterations.
