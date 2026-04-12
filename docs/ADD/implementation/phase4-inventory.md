# Phase 4 — Multi-Branch Operations and Inventory

> **Specification:** [`requeriments4.md`](requeriments4.md) | **Status:** 🔲 Not started  
> **Drivers:** US-071, US-074, US-004, US-005, US-064, PER-01, ESC-01, ESC-02, ESC-03, CRN-24, CRN-35, CRN-44  
> **Depends on:** Phase 3 complete

**Goal:** Enable full branch management with automated onboarding, branch context switching via JWT re-issuance, CQRS-based delta inventory mutations (CRN-44), real-time WebSocket propagation (PER-01), temporal tariff configuration, and validate multi-tenant scalability from 3 to 15 branches (ESC-02).

---

## A4.1 — Branch Management module

- [ ] **T4.1.1** BranchRegistrationService: `RegisterBranch` command with unique name/address validation, `isActive = true` on creation; `DeactivateBranch` sets `isActive = false`, preserves history, blocks new operations  
  - `POST /branches` — `name` (unique, non-empty), `address` required; `branchId` is UUID; emits audit event  
  - `POST /branches/:branchId/deactivate` — branch must exist and be active; users assigned to only this branch notified  
- [ ] **T4.1.2** BranchOnboardingOrchestrator: 5-step idempotent workflow triggered after branch registration (ESC-01 <1 hour)  
  - Step 1: Create database partitions for `inventory_items` and `inventory_deltas`  
  - Step 2: Seed default service catalog entries from organization template  
  - Step 3: Copy active tariff catalog to new branch  
  - Step 4: Initialize empty inventory items per service  
  - Step 5: Mark `onboarding_complete = true`  
  - Each step records progress in `branch_onboarding_status` table; idempotent and resumable on failure  
- [ ] **T4.1.3** BranchContextService: mid-session branch switch without re-authentication (ESC-03 <3 seconds)  
  - `POST /session/branch` — `branchId` required; validates user assigned to target branch via JWT `branchAssignments`  
  - Delegates to `AuthenticationService` for JWT re-issuance with updated `activeBranchId` claim  
  - Sets PostgreSQL RLS session variable `app.current_branch_id` to new branch  
  - Also handles initial post-login branch selection  
- [ ] **T4.1.4** BranchConfigurationStore: persistence layer for branch records, onboarding progress, service catalog  
  - `ListBranches` (`GET /branches`): filters `isActive`, `onboardingComplete`; paginated; admin sees all, non-admin sees only assigned  
  - `GetBranch` (`GET /branches/:branchId`): name, address, isActive, onboardingComplete, creation date, assigned user count  
  - `GetOnboardingStatus` (`GET /branches/:branchId/onboarding`): step-by-step progress; requires `branch:manage`  
- [ ] **T4.1.5** Database migrations: `branch_onboarding_status` table (branch_id FK, step_name, status, completed_at); `branch_service_catalog` table  
  - Update `branches` table with `onboarding_complete` boolean column  

## A4.2 — Inventory module (CQRS)

- [ ] **T4.2.1** Database schema: `inventory_items` and `inventory_deltas` tables partitioned by `branch_id` (PostgreSQL `PARTITION BY LIST`)  
  - `inventory_items`: item_id (UUID PK), branch_id, sku, name, category, service_id, current_stock, min_threshold, expiration_date, unit_of_measure, updated_at  
  - `inventory_deltas`: delta_id (UUID PK), item_id, branch_id, delta_type (INCREMENT/DECREMENT/ADJUST/THRESHOLD/EXPIRATION), quantity_change, absolute_quantity, reason, source_ref, staff_id, timestamp (TIMESTAMPTZ), idempotency_key (UNIQUE), sequence_number  
  - Partitions created by `BranchOnboardingOrchestrator` for each new branch  
- [ ] **T4.2.2** StockMaterializationTrigger: PostgreSQL `AFTER INSERT` trigger on `inventory_deltas`  
  - INCREMENT: adds `quantity_change` to `current_stock`  
  - DECREMENT: subtracts (raises error if result < 0)  
  - ADJUST: sets `current_stock` to `absolute_quantity`  
  - After materialization: `pg_notify('inventory_changes', json_payload)` with `branchId`, `itemId`, `deltaType`, `newStock`, `stockStatus`, `timestamp`  
  - All within same transaction as delta insertion — no eventual consistency gap  
- [ ] **T4.2.3** InventoryCommandHandler: processes five delta command types  
  - `IncrementStock` (`POST /inventory/increments`): itemId, quantity (positive), reason, sourceRef (optional — supply delivery, purchase order), idempotencyKey  
  - `DecrementStock` (`POST /inventory/decrements`): same fields + validation resulting stock >= 0; sourceRef links to consultation, dispensation  
  - `AdjustStock` (`POST /inventory/adjustments`): absoluteQuantity (non-negative), reason (mandatory justification); requires `inventory:adjust`  
  - `SetMinimumThreshold` (`PUT /inventory/:itemId/threshold`): threshold (non-negative); triggers re-evaluation of LowStockAlertProjection  
  - `UpdateExpiration` (`PUT /inventory/:itemId/expiration`): expirationDate (future or null); updates ExpirationAlertProjection  
  - Validates item exists and belongs to active branch; `SELECT ... FOR UPDATE` on inventory_items row for concurrency serialization (CRN-35)  
  - Emits audit events for all mutations  
- [ ] **T4.2.4** InventoryDeltaStore: append-only persistence layer  
  - Validates `idempotency_key` uniqueness — prevents duplicate writes during offline sync replay (CRN-43)  
  - Insertion triggers StockMaterializationTrigger  
- [ ] **T4.2.5** BranchInventoryReadModel: denormalized query-optimized projection  
  - Admin cross-branch view: aggregates all branches, grouped by service, filters by category/status/service/branch; uses `admin_reporting` RLS bypass (US-004)  
  - Service-scoped view: filtered by `branch_id` (RLS) + `service_id` for Service Managers (US-005)  
  - `GetBranchInventory` (`GET /inventory`): groupBy, filterStatus, filterCategory, filterService, sortBy, page, pageSize (default 50); partition-pruned <1s per branch  
  - `GetInventoryItem` (`GET /inventory/:itemId`): full detail; branch-scoped via RLS; <100ms  
  - `SearchInventory` (`GET /inventory/search`): name/SKU substring; indexed <500ms  
  - `ExportInventory` (`GET /inventory/export`): same filters; streaming Excel export server-side  
- [ ] **T4.2.6** LowStockAlertProjection: flags items as `OK` (stock >= threshold), `LOW_STOCK` (stock < threshold and > 0), `OUT_OF_STOCK` (stock = 0)  
  - Updated by stock materialization trigger; status in REST responses and WebSocket events  
- [ ] **T4.2.7** ExpirationAlertProjection: categorizes as `OK` (>30 days), `EXPIRING_SOON` (<=30 days), `EXPIRED` (past date)  
  - Computed daily via scheduled job and on-demand during inventory queries  
  - Color-coded in UI: OK = white, LOW_STOCK = yellow, EXPIRING_SOON = orange, EXPIRED = red  

## A4.3 — WebSocket real-time infrastructure (PER-01)

- [ ] **T4.3.1** Spring WebSocket + STOMP configuration: enable WebSocket endpoint with STOMP sub-protocol over WSS  
  - Native Spring Boot integration; structured topic management  
- [ ] **T4.3.2** RealtimeEventPublisher: persistent JDBC listener on `pg_notify('inventory_changes')`  
  - Deserializes JSON payload: branchId, itemId, deltaType, newStock, stockStatus, timestamp  
  - Publishes to `/topic/branch/{branchId}/inventory` for branch-scoped events  
  - Publishes to `/topic/admin/inventory` for admin cross-branch events  
  - Sub-second processing latency target for PER-01 (<2s end-to-end)  
- [ ] **T4.3.3** WebSocketSecurityInterceptor: JWT validation on STOMP CONNECT frame  
  - Extracts `activeBranchId` and `permissions` from JWT claims  
  - SUBSCRIBE authorization: users may only subscribe to `/topic/branch/{branchId}/inventory` if `branchId` matches `activeBranchId` or user holds `inventory:read_all`  
  - Admin wildcard to `/topic/admin/inventory` requires `inventory:read_all`  
  - Rejects unauthorized subscriptions with STOMP ERROR frame  
  - Periodic token expiry validation; disconnects on expired tokens  
  - Handles reconnection with re-authentication  
- [ ] **T4.3.4** PWA InventoryRealtimeManager: WebSocket subscription lifecycle in state management  
  - Subscribes on inventory view load; processes STOMP messages → UI re-renders  
  - Auto-reconnect with exponential backoff on disconnection  
  - On branch context switch: UNSUBSCRIBE old branch → SUBSCRIBE new branch channel  

## A4.4 — Tariff management (US-064)

- [ ] **T4.4.1** TariffManagementService: temporal effective-date pattern  
  - Each `ServiceTariff` carries `effectiveFrom` (TIMESTAMPTZ); active tariff resolved as `MAX(effectiveFrom) WHERE effectiveFrom <= NOW()`  
  - Historical tariffs immutable — price changes only affect future charges  
  - All prices stored as `DECIMAL(19,4)` per CRN-42; supports $0.00 for free services  
- [ ] **T4.4.2** Database schema: `service_tariffs` table  
  - tariff_id (UUID PK), service_id (FK), branch_id (FK), base_price (DECIMAL(19,4)), effective_from (TIMESTAMPTZ), created_by, created_at  
  - UNIQUE constraint on (service_id, branch_id, effective_from) to prevent overlapping dates  
  - RLS policy filtering by `app.current_branch_id`  
- [ ] **T4.4.3** REST interfaces:  
  - `CreateTariff` (`POST /tariffs`): serviceId, branchId, basePrice >= 0.00, effectiveFrom (current or future)  
  - `UpdateTariff` (`PUT /tariffs/:tariffId`): basePrice, effectiveFrom (must be future); creates new effective-date entry  
  - `GetActiveTariff` (`GET /tariffs/active?serviceId=X&branchId=Y`): resolves current active tariff  
  - `ListTariffs` (`GET /tariffs`): filters serviceId, branchId, includeHistorical; paginated  
  - `SearchTariffs` (`GET /tariffs/search`): service name substring  
  - All commands require `tariff:manage` permission (Director General / General Administrator)  

## A4.5 — Database partitioning and RLS extension

- [ ] **T4.5.1** PostgreSQL declarative partitioning (`PARTITION BY LIST`) on `inventory_items` and `inventory_deltas` by `branch_id`  
  - Partition pruning ensures per-branch query performance independent of total branch count  
  - Cross-branch admin queries use parallel partition scans  
  - New partitions created automatically by `BranchOnboardingOrchestrator` step 1  
- [ ] **T4.5.2** RLS policies on `inventory_items`, `inventory_deltas`, and `service_tariffs` filtering by `app.current_branch_id`  
  - Consistent with Iteration 3 SEC-02 model — defense-in-depth below application-level filtering  
- [ ] **T4.5.3** `admin_reporting` role with BYPASSRLS for cross-branch inventory views (US-004)  
  - Reuses existing `admin_reporting` role from Phase 3 (T3.4.2); extend SELECT grants to new tables  
- [ ] **T4.5.4** TenantContextInjector extension: support admin cross-branch view via `admin_reporting` RLS bypass; support mid-session branch switch resetting RLS session variable  

## A4.6 — Permissions

- [ ] **T4.6.1** Seed new permission keys via database migration:  
  - `branch:manage` — register, edit, deactivate branches (General Administrator)  
  - `branch:read` — view branch list and details (all authenticated users)  
  - `inventory:read_all` — view inventory across all branches (General Administrator, Director General)  
  - `inventory:read_service` — view inventory scoped to user's service (Service Manager, Physician)  
  - `inventory:adjust` — perform absolute stock adjustments (General Administrator)  
  - `tariff:manage` — create and update tariffs (General Administrator, Director General)  
  - `tariff:read` — view tariff catalog (all authenticated users)  
- [ ] **T4.6.2** Update system roles with new permissions in seeded role-permission mappings  

## A4.7 — PWA components

- [ ] **T4.7.1** BranchManagementView: branch CRUD UI (register with name/address, list with status, deactivate)  
  - Onboarding progress display with step-by-step completion indicators  
  - Only rendered for users with `branch:manage` via `RoleAwareRenderer` / `RequireAnyPermission`  
- [ ] **T4.7.2** InventoryDashboardView: full inventory grid for General Administrator (US-004)  
  - Grouped by service; filterable by category/status/service/branch; sortable by any column; searchable by name/SKU  
  - Paginated at 50 items/page; detail panel on item selection  
  - Color-coded status: OK = white, LOW_STOCK = yellow, EXPIRING_SOON = orange, EXPIRED = red  
  - Export to Excel button  
  - Real-time updates from `InventoryRealtimeManager`  
- [ ] **T4.7.3** ServiceInventoryView: service-scoped inventory for Service Managers (US-005)  
  - Same layout as InventoryDashboardView but pre-filtered to user's assigned service  
  - Items from other services never rendered (not merely hidden); unauthorized URL access → 403  
  - Only rendered for users with `inventory:read_service`  
- [ ] **T4.7.4** TariffConfigurationView: tariff CRUD UI (US-064)  
  - Tariff catalog with service name, code, current price, effective date  
  - Create new tariffs with effective-from date; update prices for future dates  
  - Non-negative DECIMAL validation; search across catalog  
  - Only rendered for users with `tariff:manage`  
- [ ] **T4.7.5** BranchSelectionView update: extended for mid-session context switching (ESC-03)  
  - On branch switch: (1) confirm unsaved work, (2) POST /session/branch, (3) store new JWT via SessionManager, (4) clear branch-scoped IndexedDB cache via LocalStorageManager, (5) trigger InventoryRealtimeManager re-subscription, (6) reload dashboard  
  - Single-branch users continue to auto-select and skip  
- [ ] **T4.7.6** React Router integration: add routes for /branches, /inventory, /tariffs  
  - Protected by `RequireAnyPermission` with respective permission keys  

## A4.8 — Scalability validation (ESC-02)

- [ ] **T4.8.1** Load tests: 3 to 15 branches with <10% performance degradation  
  - Measure per-branch query latency with partition pruning  
  - Measure cross-branch admin queries with parallel partition scans  
- [ ] **T4.8.2** WebSocket connection scaling: validate 15 branches x ~10 users = ~150 concurrent connections  
  - Tenant-scoped STOMP channels — no unnecessary message fan-out  
- [ ] **T4.8.3** Validate RLS and `branch_id` partitioned queries scale linearly  
- [ ] **T4.8.4** Document performance baseline for inventory queries and WebSocket latency  
- [ ] **T4.8.5** [Carried] Performance: sub-1s patient search with data seeding >50,000 records (deliverable E2.8 deferred)  

---

## Deliverables

- [ ] **E4.1** Branch management — BranchRegistrationService, BranchOnboardingOrchestrator (5-step idempotent), BranchContextService (switch <3s), BranchConfigurationStore  
- [ ] **E4.2** Inventory CQRS module — InventoryCommandHandler (5 delta types), InventoryDeltaStore (append-only, idempotent), StockMaterializationTrigger (AFTER INSERT + pg_notify), BranchInventoryReadModel, LowStockAlertProjection, ExpirationAlertProjection  
- [ ] **E4.3** WebSocket real-time — RealtimeEventPublisher (pg_notify → STOMP), WebSocketSecurityInterceptor (JWT + subscription auth), InventoryRealtimeManager; inventory reflected <2s (PER-01)  
- [ ] **E4.4** Tariff configuration — TariffManagementService with temporal effective-date pattern; DECIMAL(19,4); CRUD + search  
- [ ] **E4.5** Database infrastructure — Declarative partitioning by branch_id; RLS on inventory/tariff tables; admin_reporting bypass  
- [ ] **E4.6** Permissions — 7 new permission keys seeded; system roles updated  
- [ ] **E4.7** PWA views — BranchManagement, InventoryDashboard, ServiceInventory, TariffConfiguration, InventoryRealtimeManager, BranchSelectionView update  
- [ ] **E4.8** Scalability report — 3→15 branches with <10% degradation; WebSocket ~150 connections; partition pruning validated  
- [ ] **E4.9** [Carried] Clinical volume tests — Patient search certified sub-1s  

---

## Notes and decisions

<!-- Record decisions, issues, and resolutions during this phase. -->

### Key design decisions (from Architecture.md §9, Iteration 4)

| # | Decision | Rationale | Discarded |
|---|----------|-----------|-----------|
| D-036 | CQRS with append-only InventoryDeltaStore + PostgreSQL trigger-based materialization (CRN-44, CRN-35) | Delta commands enable deterministic offline conflict resolution; trigger guarantees transactional stock consistency; consistent with Clinical Care CQRS | CRUD with direct updates; application-level materialization; full event sourcing |
| D-037 | WebSocket publish-subscribe via Spring STOMP with tenant-scoped channels and pg_notify bridge (PER-01) | Sub-2s push propagation; eliminates polling; linear scaling; transport-layer data segmentation | Short polling; SSE; broadcast with client filtering; external broker |
| D-038 | Choreographed 5-step idempotent branch onboarding with progress tracking (ESC-01) | Steps independently testable and resumable; measurable progress; no external orchestrator | Manual configuration; event choreography |
| D-039 | Branch context switch via JWT re-issuance without re-authentication (ESC-03) | <3s; RLS enforcement; cache isolation | Logout/re-login; client-side only |
| D-040 | PostgreSQL declarative partitioning by branch_id (ESC-02, CRN-24) | Partition pruning; linear scaling; smaller per-partition indexes | No partitioning; database sharding |
| D-041 | Optimistic concurrency with SELECT ... FOR UPDATE + timestamp-ordered delta application (CRN-35) | Deterministic serialization; offline-compatible | Pessimistic locks; CRDTs; app-level locking |
| D-042 | Temporal effective-date tariff pattern with immutable history (US-064) | Past charges unaffected; auditable; DECIMAL(19,4) | Overwrite-in-place; separate history table |
| D-043 | RLS on inventory and tariff tables with admin_reporting bypass; WebSocket subscription authorization (SEC-02) | Three-layer defense-in-depth; consistent with Phase 3 model | Application-level filtering only |

### Risks / open items

- **Partition DDL automation:** `BranchOnboardingOrchestrator` must execute `CREATE TABLE ... PARTITION OF` dynamically — validate permissions for app DB role.  
- **pg_notify payload size:** PostgreSQL limits pg_notify payload to 8000 bytes — validate JSON payload stays within limits for inventory change events.  
- **WebSocket scaling:** single API Server instance handles ~150 connections; if horizontal scaling is needed in the future, STOMP broker relay (e.g., RabbitMQ) would be required.  
- **Trigger complexity:** StockMaterializationTrigger with pg_notify adds DB-level logic — ensure trigger function is covered by integration tests with Testcontainers.  
- **Offline delta replay ordering:** timestamp-ordered delta application assumes UTC clock synchronization (CRN-41); clock skew in offline mode may affect ordering — to be addressed in Phase 6.  
- **INSERT-only on inventory_deltas:** follow same pattern as clinical_events (T2.1.3) — DB triggers to prevent UPDATE/DELETE on delta table.  
