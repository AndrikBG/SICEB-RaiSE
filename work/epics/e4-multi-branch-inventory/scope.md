# Scope — E4: Multi-Branch Operations and Inventory

## Objective

Enable full branch management with automated onboarding, branch context switching via JWT re-issuance, CQRS-based delta inventory mutations (CRN-44), real-time WebSocket propagation (PER-01), temporal tariff configuration, and validate multi-tenant scalability from 3 to 15 branches (ESC-02).

## In Scope

- Branch registration, deactivation, 5-step idempotent onboarding (ESC-01)
- Branch context switch without re-auth (ESC-03)
- Inventory CQRS: delta commands (5 types), append-only delta store, PostgreSQL trigger materialization + pg_notify
- WebSocket real-time: STOMP channels, JWT subscription auth, PWA subscription manager
- Tariff management: temporal effective-date pattern, DECIMAL(19,4)
- DB partitioning by branch_id, RLS on new tables, admin_reporting bypass
- 7 new permission keys
- PWA views: branch management, inventory dashboard, service inventory, tariff config
- Scalability validation: 3→15 branches <10% degradation

## Out of Scope

- Offline sync for inventory (Phase 6)
- Supply request workflow (Phase 5)
- Pharmacy dispensing and stock deduction (Phase 5)
- Horizontal WebSocket scaling (single-instance sufficient for ~150 connections)

## Drivers

US-071, US-074, US-004, US-005, US-064, PER-01, ESC-01, ESC-02, ESC-03, CRN-24, CRN-35, CRN-44

## Done Criteria

- [ ] Branch CRUD + onboarding < 1 hour
- [ ] Context switch < 3 seconds, no re-auth
- [ ] 5 delta command types working with idempotency
- [ ] Inventory updates propagated via WebSocket < 2 seconds
- [ ] Tariffs with effective-date pattern, immutable history
- [ ] RLS + partitioning on inventory tables
- [ ] PWA views functional for all roles
- [ ] Scalability report: 3→15 branches validated

## Stories

| # | Story | Size | Activities | Dependencies |
|---|-------|------|-----------|-------------|
| S4.1 | Branch management + context switching | M | A4.1, A4.5.2, A4.6 | None |
| S4.2 | Inventory CQRS + real-time propagation | L | A4.2, A4.3, A4.5.1, A4.5.2 | S4.1 |
| S4.3 | Tariff configuration | S | A4.4 | S4.1 |
| S4.4 | PWA views | M | A4.7 | S4.1, S4.2, S4.3 |
| S4.5 | Scalability validation | S | A4.8 | S4.1, S4.2 |

## Implementation Plan

### Sequencing Strategy: Dependency-driven + Risk-first

S4.1 unblocks everything. S4.2 is critical path and highest risk (partitioning, triggers, pg_notify, WebSocket security). S4.3 is small and parallelizable with S4.2 if team capacity exists; otherwise sequential after S4.2 since it's on the PWA critical path. S4.4 integrates all backend work. S4.5 validates the epic hypothesis.

### Sequence

| Order | Story | Rationale | Enables |
|-------|-------|-----------|---------|
| 1 | **S4.1** Branch management + context switching | Walking skeleton — proves branch write operations work with existing multi-tenant infra. Unblocks all other stories. | S4.2, S4.3, S4.4, S4.5 |
| 2 | **S4.2** Inventory CQRS + real-time | Risk-first — largest story, most unknowns (partitioning DDL, PG triggers, pg_notify→STOMP bridge, WebSocket JWT auth). Critical path to PWA. | S4.4, S4.5 |
| 3 | **S4.3** Tariff configuration | Quick win — small scope, proven patterns (CRUD + temporal query). Completes backend API surface for PWA. | S4.4 |
| 4 | **S4.4** PWA views | Integration story — consumes all backend APIs. E2E integration checkpoint. | S4.5 |
| 5 | **S4.5** Scalability validation | Validation — needs real data from S4.1+S4.2. Closes the epic hypothesis. | Epic done |

### Parallel Opportunities

- **S4.2 + S4.3** could run in parallel (different codebase areas: `domain.inventory` vs `domain.billing`). Single developer: sequential as shown.
- **S4.4 PWA** can start branch views while S4.3 finishes (branch API from S4.1 is available first).

### Milestones

#### M1: Walking Skeleton (after S4.1) -- DONE 2026-04-12
- [x] Branch registered via `POST /branches` with audit event
- [x] 5-step onboarding completes in <1 hour
- [x] Branch context switch via `POST /session/branch` returns new JWT in <3 seconds (40ms measured)
- [x] RLS enforced on new tables (branch_onboarding_status, branch_service_catalog)
- [x] Permissions seeded and verified via existing role admin UI
- **Demo:** Register new branch → onboarding → switch to it → see empty inventory context

#### M2: Core Backend Complete (after S4.2 + S4.3)
- [x] 5 delta command types working with idempotency enforcement
- [x] StockMaterializationTrigger materializes stock transactionally
- [x] pg_notify → STOMP push delivers inventory changes to subscribed clients
- [x] WebSocket JWT auth rejects unauthorized subscriptions
- [x] Tariff CRUD with effective-date resolution working
- [x] Partitioning validated: queries use partition pruning (EXPLAIN ANALYZE)
- **Demo:** Increment stock via API → see materialized stock → receive WebSocket event → query tariff

#### M3: E2E Integration (after S4.4) — PAT-E-539
- [ ] All PWA views render and interact with real backend (Docker Compose, real DB)
- [ ] Branch management: register, onboard, switch — full flow in browser
- [ ] Inventory dashboard: delta mutation → real-time grid update via WebSocket
- [ ] Tariff config: create, resolve active, view history
- [ ] Role-gated: admin sees cross-branch, service manager sees own service only
- [ ] Cross-story contract validation: auth headers, payload schemas, WebSocket channels
- **Demo:** Full user journey: login → select branch → view inventory → make stock adjustment → see real-time update → switch branch → see different inventory

#### M4: Epic Complete (after S4.5)
- [ ] Scalability report: 3→15 branches with <10% query degradation
- [ ] WebSocket: ~150 concurrent connections sustained
- [ ] Patient search sub-1s with >50K records (carried from Phase 2)
- [ ] All done criteria checked
- [ ] Retrospective completed
- **Gate:** `/rai-epic-close`

### Progress Tracking

| Story | Status | Branch | Started | Completed | Notes |
|-------|--------|--------|---------|-----------|-------|
| S4.1 | Done | `story/s4.1/branch-management` | 2026-04-11 | 2026-04-12 | 5 tasks, 110 tests, 0.83x velocity |
| S4.2 | Done | `story/s4.2/inventory-cqrs-realtime` | 2026-04-12 | 2026-04-12 | 7 tasks, 180 tests, 0.89x velocity |
| S4.3 | Done | `story/s4.3/tariff-configuration` | 2026-04-13 | 2026-04-13 | 3 tasks, 202 tests, 1.11x velocity |
| S4.4 | Pending | — | — | — | |
| S4.5 | Pending | — | — | — | |

| Milestone | Status | Verified |
|-----------|--------|----------|
| M1: Walking Skeleton | Done | 2026-04-12 — all AC verified via Docker |
| M2: Core Backend | Done | 2026-04-13 — tariff CRUD completes backend surface |
| M3: E2E Integration | Pending | — |
| M4: Epic Complete | Pending | — |

### Sequencing Risks

| # | Risk | Mitigation |
|---|------|------------|
| 1 | S4.2 is large (L) and could stall the critical path | Decompose into tasks early via `/rai-story-plan`; if blocked on triggers, advance S4.3 |
| 2 | No Testcontainers infrastructure — S4.2 needs DB integration tests for triggers/RLS/partitioning | Add Testcontainers setup as first task of S4.2 (gap G2 from Phase 3) |
| 3 | No load testing tool exists — S4.5 can't execute without k6/JMeter | Introduce k6 in S4.5 first task; don't defer tooling to end |

## Source of Truth

> [`docs/ADD/implementation/phase4-inventory.md`](../../../docs/ADD/implementation/phase4-inventory.md)
