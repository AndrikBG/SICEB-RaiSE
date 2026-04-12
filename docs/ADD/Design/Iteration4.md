# Iteration 4 — Multi-Branch Operations and Inventory Management

## Goal

Enable multi-branch operations: branch registration, active branch selection with context switching, branch-scoped inventory views, and real-time inventory updates across branches. Validate the multi-tenant scalability model for network growth from 3 to 15 branches. Adopt command/delta-based inventory mutations as the foundation for deterministic offline conflict resolution.

With clinical workflows (Iteration 2) and security infrastructure (Iteration 3) in place, this iteration activates the multi-branch dimension. Branch selection (US-074, rank 2) and branch registration (US-071, rank 3) are the second and third highest-ranked primary user stories. Inventory management (US-004, rank 7) directly supports daily clinical operations and is a prerequisite for pharmacy dispensation in Iteration 5. Inventory mutations are designed from the start as delta commands (CRN-44) following the offline-aware conventions from Iteration 1 (CRN-43), ensuring that when Iteration 6 introduces offline sync, inventory conflict resolution is deterministic and requires no data-layer rework.

**Business objective:** Gestión de Inventario — Rigorous control of medical supplies, materials, and medications across all branches.

---

## Step 2: Iteration Drivers

### Primary User Stories

| Driver | Type | Description | Why this iteration |
|---|---|---|---|
| **US-074** | Primary US (rank 2) | Select active branch — context switch for multi-branch users; supports SEC-02, ESC-02 | Rank 2 primary US; enables branch-scoped data views required by inventory, clinical care, and pharmacy modules |
| **US-071** | Primary US (rank 3) | Register new branch — enables network expansion; supports SEC-02, ESC-02 | Rank 3 primary US; prerequisite for multi-branch scalability validation |
| **US-004** | Primary US (rank 7) | Complete inventory view for admin — total visibility of clinical resources; supports PER-01 | Rank 7 primary US; core inventory visibility requirement, prerequisite for pharmacy (Iteration 5) |

### Supporting User Stories

| Driver | Type | Description | Why this iteration |
|---|---|---|---|
| **US-005** | US (HIGH) | Service Manager sees ONLY the inventory of their service | Branch + service scoping; validates RBAC and tenant isolation against inventory data |
| **US-064** | US (HIGH) | Configure tariffs by medical service with base price | Tariff configuration is branch-scoped and must be in place before payment registration (Iteration 5) |

### Quality Attribute Scenarios

| Driver | Type | Description | Why this iteration |
|---|---|---|---|
| **PER-01** | QA Scenario (High/High) | Inventory updates reflected across all views in less than 2 seconds | One of the 6 High/High scenarios; requires real-time propagation infrastructure (WebSocket) |
| **ESC-01** | QA Scenario | New branch fully operational in less than 1 hour | Defines measurable scalability guarantee for branch onboarding |
| **ESC-02** | QA Scenario (High/High) | Growth from 3 to 15 branches with <10% performance degradation | One of the 6 High/High scenarios; validates multi-tenant model under load |
| **ESC-03** | QA Scenario | Staff branch-context switch in less than 3 seconds without logout | Defines UX performance for multi-branch users switching context |

### Architectural Concerns

| Driver | Type | Description | Why this iteration |
|---|---|---|---|
| **CRN-24** | Concern | Multi-tenant model must sustain network growth without performance degradation | Core scalability concern; tenant isolation validated at inventory and branch management level |
| **CRN-35** | Concern | Inventory consistency under concurrent edits from multiple branches | Concurrent modifications to the same supply need a conflict resolution policy |
| **CRN-44** | Concern | Inventory mutations must be modeled as delta commands for deterministic offline conflict resolution | Following CRN-43 conventions; delta commands enable Iteration 6 to apply changes deterministically |

---

## Step 3: Elements to Refine

| Element | Current State | Refinement Action |
|---|---|---|
| **Branch Management platform module (BM)** | Defined as a platform module with brief responsibilities. No internal decomposition, no schema, no onboarding workflow. | **Decompose internally** into: `BranchRegistrationService`, `BranchOnboardingOrchestrator`, `BranchContextService`, `BranchConfigurationStore`. |
| **Inventory domain module (INV)** | Defined as a domain module referencing delta commands. No internal decomposition. | **Fully decompose** into CQRS structure: `InventoryCommandHandler`, `InventoryDeltaStore`, `StockMaterializationTrigger`, `BranchInventoryReadModel`, `LowStockAlertProjection`, `ExpirationAlertProjection`. |
| **Billing & Payments — Tariff subsystem** | Brief responsibility for tariff configuration. No internal design. | **Refine** with `TariffManagementService` using temporal effective-date pattern. |
| **API Server — WebSocket infrastructure** | Mentioned but not designed. | **Design** `RealtimeEventPublisher` and `WebSocketSecurityInterceptor` with STOMP topic routing. |
| **PWA Client** | No inventory, branch management, or tariff UI. | **Extend** with `BranchManagementView`, `InventoryDashboardView`, `ServiceInventoryView`, `TariffConfigurationView`, `InventoryRealtimeManager`. Update `BranchSelectionView` for mid-session switching. |
| **Cloud Database** | No inventory or tariff schema. | **Define** tables: `inventory_items`, `inventory_deltas` (partitioned by branch_id), `service_tariffs`, `branch_onboarding_status`. Add RLS policies and indexes. |
| **Security Middleware — TenantContextInjector** | No support for cross-branch inventory or mid-session switch. | **Extend** for admin cross-branch view and mid-session JWT re-issuance. |

---

## Step 4: Design Concepts

### Architectural Patterns

| Design Concept | Pros | Cons | Discarded Alternatives |
|---|---|---|---|
| **CQRS for Inventory bounded context** | Append-only delta log enables offline conflict resolution; read models independently optimizable; consistent with Clinical Care CQRS | Two models to maintain; eventual consistency (mitigated by trigger-based materialization) | CRUD with direct updates — prevents offline merge; Full event sourcing — excessive overhead |
| **Publish-Subscribe via WebSocket** | Sub-2s propagation; eliminates polling; tenant-scoped channels | Persistent connections consume resources; reconnection logic needed | Short polling — unacceptable latency; SSE — unidirectional; Long polling — connection churn |
| **Choreographed branch onboarding** | Steps independently testable and resumable; measurable progress | Orchestration concentrated in one service | Manual configuration — unmeasurable; Event choreography — harder to track |

### Externally Developed Components

| Design Concept | Pros | Cons | Discarded Alternatives |
|---|---|---|---|
| **Spring WebSocket with STOMP** | Native Spring Boot integration; structured topic management; JWT auth | STOMP sub-protocol layer | Raw WebSocket — custom routing; External message broker — operational overhead |
| **PostgreSQL trigger-based materialization** | Transactional and atomic; no application-level job | DB trigger complexity; PostgreSQL coupling | Application-level job — consistency gap; Materialized views — blocking REFRESH |
| **Temporal effective-date tariffs** | Preserves price history; simple query pattern | Temporal filter in queries; index design | Overwrite-in-place — loses history; Separate history table — duplication |

### Tactics

| Design Concept | Pros | Cons | Discarded Alternatives |
|---|---|---|---|
| **Optimistic concurrency with delta ordering** | Deterministic resolution; compatible with offline replay | Row contention under high concurrency | Pessimistic locks — offline-incompatible; CRDTs — premature complexity |
| **Database partitioning by branch_id** | Linear query scaling; partition pruning; smaller indexes | DDL complexity; cross-branch scan overhead | No partitioning — degrades at 15 branches; Sharding — excessive complexity |
| **Tenant-scoped WebSocket channels** | Prevents cross-branch leakage; linear connection scaling | JWT validation on CONNECT; wildcard routing for admins | Broadcast with client filtering — data leakage; Per-user channels — connection explosion |
| **Branch context switch via JWT re-issuance** | No re-auth; fresh JWT claims; RLS reset; cache isolation | Server round-trip; cache clearing causes brief reload | Logout/re-login — violates ESC-03; Client-side only — no enforcement |

---

## Step 5: Instantiation Decisions

| Instantiation Decision | Rationale |
|---|---|
| **`BranchRegistrationService` in Branch Management** — Branch CRUD with unique validation, audit events | Satisfies **US-071**, **ESC-01** |
| **`BranchOnboardingOrchestrator`** — 5-step idempotent workflow with progress tracking | Satisfies **ESC-01** (<1 hour) |
| **`BranchContextService`** — Mid-session switch with JWT re-issuance, no re-auth | Satisfies **US-074**, **ESC-03**, **SEC-02** |
| **`InventoryCommandHandler`** — 5 delta command types with `SELECT ... FOR UPDATE` | Satisfies **CRN-44**, **CRN-35**, **CRN-43** |
| **`InventoryDeltaStore`** — Append-only with idempotency key validation | Satisfies **CRN-44**, **CRN-43** |
| **`StockMaterializationTrigger`** — PostgreSQL AFTER INSERT trigger + pg_notify | Satisfies **CRN-44**, **CRN-35**, **PER-01** |
| **`BranchInventoryReadModel`** — Admin cross-branch and service-scoped views | Satisfies **US-004**, **US-005**, **PER-01** |
| **`LowStockAlertProjection`** and **`ExpirationAlertProjection`** | Satisfies **US-004**, **US-005** |
| **`RealtimeEventPublisher`** — pg_notify listener → STOMP publisher | Satisfies **PER-01**, **SEC-02**, **ESC-02** |
| **`WebSocketSecurityInterceptor`** — JWT auth on CONNECT, subscription authorization | Satisfies **SEC-02**, **PER-01** |
| **`TariffManagementService`** — Temporal effective-date pattern, DECIMAL(19,4) | Satisfies **US-064**, **CRN-42** |
| **Database partitioning** for `inventory_items` and `inventory_deltas` by `branch_id` | Satisfies **ESC-02**, **CRN-24** |
| **RLS policies** on inventory and tariff tables | Satisfies **SEC-02** |
| **PWA components** — BranchManagementView, InventoryDashboardView, ServiceInventoryView, TariffConfigurationView, InventoryRealtimeManager | Satisfies **US-004**, **US-005**, **US-064**, **US-074**, **PER-01**, **ESC-03** |
| **New permissions** — branch:manage, branch:read, inventory:read_all, inventory:read_service, tariff:manage, tariff:read | Satisfies **SEC-02**, **CRN-15** |

---

## Step 6: Views, Interfaces, and Design Decisions

### Diagrams Created / Updated

| Diagram | Section in Architecture.md | Description |
|---|---|---|
| Branch Management Module Internals | Section 6.1.5 | Component diagram decomposing BM into BranchRegistrationService, BranchOnboardingOrchestrator, BranchContextService, BranchConfigurationStore with responsibility table |
| Inventory Module Internals | Section 6.1.6 | CQRS component diagram with command side (InventoryCommandHandler, InventoryDeltaStore, StockMaterializationTrigger) and read side (BranchInventoryReadModel, LowStockAlertProjection, ExpirationAlertProjection) with responsibility table |
| WebSocket Real-Time Event Infrastructure | Section 6.1.7 | Component diagram for RealtimeEventPublisher, WebSocketSecurityInterceptor, STOMP topic routing with responsibility table |
| PWA Branch and Inventory Components | Section 6.2.3 | Component diagram for BranchManagementView, InventoryDashboardView, ServiceInventoryView, TariffConfigurationView, InventoryRealtimeManager, updated BranchSelectionView with responsibility table |
| SD-02: Branch Context Selection (updated) | Section 7 | Added Iteration 4 note referencing SD-12 for mid-session switching |
| SD-10: Register New Branch with Onboarding | Section 7 | Full onboarding workflow sequence diagram (US-071, ESC-01) |
| SD-11: Inventory Delta with Real-Time Propagation | Section 7 | End-to-end delta command → trigger → pg_notify → WebSocket → UI sequence (CRN-44, PER-01, CRN-35) |
| SD-12: Branch Context Switch Without Logout | Section 7 | Mid-session switch with JWT re-issuance, cache clearing, WebSocket re-subscription (US-074, ESC-03) |
| SD-13: Admin Views Cross-Branch Inventory | Section 7 | Cross-branch admin view with RLS bypass and admin WebSocket channel (US-004, ESC-02) |

### Interfaces Defined

#### Command Interfaces (Branch Management)

| Command | Endpoint | Key Drivers |
|---|---|---|
| RegisterBranch | `POST /branches` | US-071, ESC-01 |
| DeactivateBranch | `POST /branches/:branchId/deactivate` | US-071 |
| SwitchBranch | `POST /session/branch` | US-074, ESC-03, SEC-02 |

#### Query Interfaces (Branch Management)

| Query | Endpoint | Key Drivers |
|---|---|---|
| ListBranches | `GET /branches` | US-071, US-074 |
| GetBranch | `GET /branches/:branchId` | US-071 |
| GetOnboardingStatus | `GET /branches/:branchId/onboarding` | ESC-01 |

#### Command Interfaces (Inventory)

| Command | Endpoint | Key Drivers |
|---|---|---|
| IncrementStock | `POST /inventory/increments` | CRN-44, CRN-43 |
| DecrementStock | `POST /inventory/decrements` | CRN-44, CRN-43, CRN-35 |
| AdjustStock | `POST /inventory/adjustments` | CRN-44 |
| SetMinimumThreshold | `PUT /inventory/:itemId/threshold` | US-004, US-005 |
| UpdateExpiration | `PUT /inventory/:itemId/expiration` | US-004 |

#### Query Interfaces (Inventory)

| Query | Endpoint | Key Drivers |
|---|---|---|
| GetBranchInventory | `GET /inventory` | US-004, US-005, PER-01, ESC-02 |
| GetInventoryItem | `GET /inventory/:itemId` | US-004, US-005 |
| SearchInventory | `GET /inventory/search` | US-004, US-005 |
| ExportInventory | `GET /inventory/export` | US-004, US-005 |

#### Tariff Management Interfaces

| Interface | Type | Endpoint | Key Drivers |
|---|---|---|---|
| CreateTariff | Command | `POST /tariffs` | US-064, CRN-42 |
| UpdateTariff | Command | `PUT /tariffs/:tariffId` | US-064, CRN-42 |
| GetActiveTariff | Query | `GET /tariffs/active` | US-064 |
| ListTariffs | Query | `GET /tariffs` | US-064 |
| SearchTariffs | Query | `GET /tariffs/search` | US-064 |

### Design Decisions

| Driver | Decision | Rationale | Discarded Alternatives |
|---|---|---|---|
| **CRN-44, CRN-35** | CQRS with append-only delta store and PostgreSQL trigger-based materialization | Deterministic offline conflict resolution; transactional stock consistency; consistent with Clinical Care CQRS | CRUD with direct updates; application-level materialization; full event sourcing |
| **PER-01, ESC-02** | WebSocket publish-subscribe via Spring STOMP with tenant-scoped channels and pg_notify bridge | Sub-2-second push propagation; linear scaling; transport-layer data segmentation | Short polling; SSE; broadcast with client filtering |
| **US-071, ESC-01** | Choreographed 5-step idempotent onboarding workflow with progress tracking | Each step testable and resumable; measurable progress | Manual configuration; event choreography |
| **US-074, ESC-03** | Branch context switch via JWT re-issuance without re-authentication | <3 seconds; RLS enforcement; cache isolation | Logout/re-login; client-side only |
| **ESC-02, CRN-24** | PostgreSQL declarative partitioning by branch_id | Linear scaling; partition pruning; smaller indexes | No partitioning; database sharding |
| **CRN-35** | Optimistic concurrency with SELECT ... FOR UPDATE and timestamp-ordered delta application | Deterministic serialization; offline-compatible | Pessimistic locks; CRDTs; application-level locking |
| **US-064** | Temporal effective-date tariff pattern with immutable history | Past charges unaffected; auditable; DECIMAL(19,4) precision | Overwrite-in-place; separate history table |
| **SEC-02** | RLS on inventory and tariff tables with admin_reporting bypass; WebSocket subscription authorization | Three-layer defense-in-depth; consistent with Iteration 3 model | Application-level filtering only |

---

## Step 7: Analysis of Design and Iteration Goal Achievement

| Driver | Analysis Result |
|---|---|
| **US-074** — Select active branch | **Satisfied.** `BranchContextService` handles mid-session switching with JWT re-issuance. SD-12 demonstrates the full flow. `BranchSelectionView` updated for context switching. |
| **US-071** — Register new branch | **Satisfied.** `BranchRegistrationService` + `BranchOnboardingOrchestrator` with 5-step idempotent workflow. SD-10 demonstrates the full flow. `BranchManagementView` in PWA. |
| **US-004** — Complete inventory view (Admin) | **Satisfied.** `BranchInventoryReadModel` with cross-branch admin view via `admin_reporting` RLS bypass. SD-13 demonstrates the flow. `InventoryDashboardView` with filters, sorting, search, export. |
| **US-005** — Service Manager sees only their inventory | **Satisfied.** `BranchInventoryReadModel` with service-scoped view via RLS + service_id filtering. `ServiceInventoryView` in PWA. Unauthorized access returns 403 and is logged. |
| **US-064** — Configure tariffs | **Satisfied.** `TariffManagementService` with temporal effective-date pattern. Interfaces defined in §8.11. `TariffConfigurationView` in PWA. |
| **PER-01** — Inventory updates <2 seconds | **Satisfied.** End-to-end real-time propagation: trigger → pg_notify → RealtimeEventPublisher → STOMP → InventoryRealtimeManager → UI. SD-11 demonstrates the flow. No polling. |
| **ESC-01** — New branch operational <1 hour | **Satisfied.** `BranchOnboardingOrchestrator` with 5 automated steps and progress tracking. SD-10 demonstrates the workflow. Each step is idempotent and resumable. |
| **ESC-02** — 3 to 15 branches <10% degradation | **Satisfied.** PostgreSQL declarative partitioning by `branch_id` ensures per-branch queries are independent of total branch count. Partition pruning and parallel scans. Tenant-scoped WebSocket channels scale linearly. |
| **ESC-03** — Branch switch <3 seconds without logout | **Satisfied.** JWT re-issuance via `BranchContextService` without re-authentication. SD-12 demonstrates the flow. Cache clearing + WebSocket re-subscription included. |
| **CRN-24** — Multi-tenant scalability | **Satisfied.** Database partitioning by `branch_id` ensures query performance scales linearly. RLS enforcement consistent across all tenant-scoped tables. WebSocket channels scale linearly. |
| **CRN-35** — Inventory consistency under concurrent edits | **Satisfied.** `SELECT ... FOR UPDATE` for within-branch serialization. Timestamp-ordered delta application for cross-branch conflicts. Trigger-based materialization ensures transactional consistency. |
| **CRN-44** — Delta-based inventory mutations | **Satisfied.** All five inventory command types persist append-only deltas to `InventoryDeltaStore`. Current stock derived by trigger, not direct updates. Compatible with offline replay in Iteration 6. |

### Summary

| Status | Count | Drivers |
|---|---|---|
| **Satisfied** | 12 | US-074, US-071, US-004, US-005, US-064, PER-01, ESC-01, ESC-02, ESC-03, CRN-24, CRN-35, CRN-44 |
| **Partially Satisfied** | 0 | — |
| **Not Satisfied** | 0 | — |

All 12 drivers for Iteration 4 have been satisfied. The multi-branch operations architecture — including the Branch Management module with onboarding orchestration, the CQRS Inventory module with delta-based mutations and trigger-based materialization, the WebSocket real-time event infrastructure, database partitioning for scalability, temporal tariff management, and the extended PWA components — is now in place. Downstream iterations can safely depend on: the inventory module for pharmacy dispensation with stock verification (Iteration 5), the delta-based mutation model for offline conflict resolution (Iteration 6), and the multi-branch scalability model for consolidated reporting (Iteration 7).
