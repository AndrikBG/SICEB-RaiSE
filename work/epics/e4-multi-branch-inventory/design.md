# Design — E4: Multi-Branch Operations and Inventory

> Source of truth for architecture decisions: `docs/ADD/implementation/phase4-inventory.md` (D-036 to D-043)
> This document captures the Gemba (current state), story decomposition, target components, and risks.

---

## Gemba (Current State)

| Component | State | Notes |
|-----------|-------|-------|
| `Branch` entity | Read-only | Exists in `platform.branch.entity`, no setters, no registration service |
| `BranchRepository` | Exists | In `platform.iam.repository` — needs relocation to `platform.branch` |
| `TenantFilter` | JWT-based | Reads `activeBranchId` from principal, sets `TenantContext` |
| `TenantAwareDataSource` | Functional | `SET LOCAL app.branch_id` per connection |
| `WebSocketConfig` | Basic STOMP | No JWT auth on CONNECT, no tenant-scoped channels |
| `inventory` module | Stub | `package-info.java` only (IC-01) |
| `RLS infrastructure` | Ready | `apply_rls_policy()` helper, `admin_reporting` role with BYPASSRLS |
| `AuthenticationService` | Functional | JWT issuance exists — needs `reissueBranchToken()` for context switch |
| Permissions seed | 40 keys | Needs 7 new keys for branch/inventory/tariff |

## Key Design Decisions (from docs/ADD)

| # | Decision | Pattern |
|---|----------|---------|
| D-036 | CQRS append-only `InventoryDeltaStore` + PG trigger materialization | Same as ClinicalEventStore (Phase 2) |
| D-037 | WebSocket via Spring STOMP + `pg_notify` bridge | Tenant-scoped channels |
| D-038 | 5-step idempotent branch onboarding with progress tracking | Choreographed, resumable |
| D-039 | Branch switch via JWT re-issuance, no re-auth | RLS + cache isolation |
| D-040 | PostgreSQL declarative partitioning by `branch_id` | Partition pruning for scalability |
| D-041 | `SELECT ... FOR UPDATE` + timestamp-ordered delta application | Concurrency serialization |
| D-042 | Temporal effective-date tariff pattern | Immutable history |
| D-043 | RLS on inventory/tariff + WebSocket subscription auth | Three-layer defense-in-depth |

No new ADRs needed — D-036 to D-043 are well-reasoned and consistent with Phases 1-3 patterns.

## Story Decomposition

### S4.1 — Branch Management and Context Switching (M)

**Objective:** Full branch lifecycle + mid-session switch without re-auth.

**Activities:** A4.1 (branch module), A4.6 (permissions), A4.5.2 (RLS on new tables)

**Delivers:**
- `BranchRegistrationService`: register + deactivate
- `BranchOnboardingOrchestrator`: 5-step idempotent workflow
- `BranchContextService`: JWT re-issuance for branch switch (ESC-03)
- Flyway migrations: `branch_onboarding_status`, `branch_service_catalog`, `branches.onboarding_complete`
- Permission seed: `branch:manage`, `branch:read`
- REST: `POST/GET /branches`, `POST /branches/:id/deactivate`, `POST /session/branch`
- RLS policies on new tables

**Dependencies:** None (builds on existing branch/IAM infrastructure)

**Done:** Branch created, onboarding < 1 hour, context switch < 3 seconds.

---

### S4.2 — Inventory CQRS and Real-Time Propagation (L)

**Objective:** Delta-based inventory mutations with trigger materialization and WebSocket push.

**Activities:** A4.2 (inventory CQRS), A4.3 (WebSocket), A4.5.1 (partitioning), A4.5.2 (RLS)

**Delivers:**
- Flyway: `inventory_items` + `inventory_deltas` tables (partitioned by `branch_id`)
- `StockMaterializationTrigger`: PG `AFTER INSERT` trigger + `pg_notify`
- `InventoryCommandHandler`: 5 delta types (increment, decrement, adjust, threshold, expiration)
- `InventoryDeltaStore`: append-only, idempotent by key
- `BranchInventoryReadModel`: admin cross-branch + service-scoped views
- `LowStockAlertProjection` + `ExpirationAlertProjection`
- `RealtimeEventPublisher`: `pg_notify` → STOMP
- `WebSocketSecurityInterceptor`: JWT auth on CONNECT + subscription authorization
- PWA `InventoryRealtimeManager`: subscription lifecycle
- Permission seed: `inventory:read_all`, `inventory:read_service`, `inventory:adjust`
- REST: `POST /inventory/increments|decrements|adjustments`, `PUT /inventory/:id/threshold|expiration`, `GET /inventory`, `GET /inventory/:id`, `GET /inventory/search`

**Dependencies:** S4.1 (branch onboarding creates partitions)

**Done:** 5 delta types working, idempotent, WebSocket < 2 seconds, RLS enforced.

---

### S4.3 — Tariff Configuration (S)

**Objective:** Temporal effective-date pricing with immutable history.

**Activities:** A4.4 (tariff management)

**Delivers:**
- Flyway: `service_tariffs` table with RLS
- `TariffManagementService`: create, update (new effective-date entry), resolve active
- Permission seed: `tariff:manage`, `tariff:read`
- REST: `POST/PUT/GET /tariffs`, `GET /tariffs/active`, `GET /tariffs/search`

**Dependencies:** S4.1 (branch + service catalog)

**Done:** Tariffs with effective-date, DECIMAL(19,4), immutable history.

---

### S4.4 — PWA Views (M)

**Objective:** Frontend for branch management, inventory, and tariffs.

**Activities:** A4.7 (all PWA components)

**Delivers:**
- `BranchManagementView`: CRUD + onboarding progress
- `InventoryDashboardView`: admin cross-branch grid, filters, color-coded status, Excel export
- `ServiceInventoryView`: service-scoped, pre-filtered
- `TariffConfigurationView`: catalog, create/update with effective-date
- `BranchSelectionView` update: mid-session switch with cache cleanup
- React Router: `/branches`, `/inventory`, `/tariffs`

**Dependencies:** S4.1, S4.2, S4.3 (all backend APIs must exist)

**Done:** All views functional, role-gated, real-time updates working.

---

### S4.5 — Scalability Validation (S)

**Objective:** Prove 3→15 branch scaling with <10% degradation.

**Activities:** A4.8 (load tests + carried items)

**Delivers:**
- Load test suite (k6 or JMeter)
- Per-branch query latency with partition pruning
- Cross-branch admin query performance
- WebSocket ~150 connections validated
- [Carried] Patient search sub-1s with >50K records
- Performance baseline document

**Dependencies:** S4.1, S4.2 (needs branch + inventory data to test)

**Done:** Scalability report with measured metrics.

## Dependency Graph

```
S4.1 (Branch)
  ├──→ S4.2 (Inventory) ──→ S4.4 (PWA)
  ├──→ S4.3 (Tariff) ─────→ S4.4 (PWA)
  └──→ S4.5 (Scalability)
         ↑
       S4.2 ──────────────→ S4.5
```

No cycles. S4.1 is the critical path.

## Target Components (new packages)

```
com.siceb
├── platform/branch/
│   ├── entity/Branch.java              (MODIFY — add setters, onboarding fields)
│   ├── repository/BranchRepository.java (MOVE from platform.iam.repository)
│   ├── service/
│   │   ├── BranchRegistrationService.java    (NEW)
│   │   ├── BranchOnboardingOrchestrator.java (NEW)
│   │   └── BranchContextService.java         (NEW)
│   └── store/BranchConfigurationStore.java   (NEW)
├── domain/inventory/
│   ├── model/
│   │   ├── InventoryItem.java          (NEW — @Entity)
│   │   ├── InventoryDelta.java         (NEW — @Entity, append-only)
│   │   └── DeltaType.java             (NEW — enum)
│   ├── command/                        (NEW — 5 command records)
│   ├── repository/
│   │   ├── InventoryItemRepository.java    (NEW)
│   │   └── InventoryDeltaRepository.java   (NEW)
│   ├── service/
│   │   ├── InventoryCommandHandler.java    (NEW)
│   │   └── InventoryDeltaStore.java        (NEW)
│   └── readmodel/
│       ├── BranchInventoryReadModel.java   (NEW)
│       ├── LowStockAlertProjection.java    (NEW)
│       └── ExpirationAlertProjection.java  (NEW)
├── domain/billing/
│   ├── model/ServiceTariff.java        (NEW — @Entity)
│   ├── repository/TariffRepository.java (NEW)
│   └── service/TariffManagementService.java (NEW)
├── config/
│   └── WebSocketConfig.java            (MODIFY — add security interceptor)
└── api/
    ├── BranchController.java           (NEW)
    ├── InventoryController.java        (NEW)
    └── TariffController.java           (NEW)
```

## Risks

| # | Risk | Likelihood | Impact | Mitigation |
|---|------|-----------|--------|------------|
| R1 | Partition DDL requires elevated DB privileges for app role | High | Medium | Test with Testcontainers; document required GRANT for production |
| R2 | pg_notify 8KB payload limit exceeded | Low | Medium | Validate payload size in integration test; strip to essential fields |
| R3 | No load testing infrastructure exists | High | High | Add k6 in S4.5; define baseline before claiming ESC-02 pass |

## Parking Lot

| Item | Origin | Priority | Promote When |
|------|--------|----------|-------------|
| Offline inventory sync | E4 out-of-scope | High | Phase 6 starts |
| Supply request workflow | E4 out-of-scope | High | Phase 5 starts |
| Horizontal WebSocket scaling | E4 risk assessment | Low | >150 concurrent connections needed |
| BranchRepository relocation from iam to branch | Gemba analysis | Medium | S4.1 implementation |
| Integration tests with Testcontainers | Phase 3 gap (G2) | High | Before S4.2 (RLS + triggers need DB tests) |
