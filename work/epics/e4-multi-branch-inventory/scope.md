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

## Planned Stories

> Detailed breakdown in `/rai-epic-plan`. Tentative grouping:

| # | Story | Activities |
|---|-------|-----------|
| S4.1 | Branch management + context switching | A4.1, A4.5 (partial), A4.6 |
| S4.2 | Inventory CQRS + real-time | A4.2, A4.3, A4.5 (partial) |
| S4.3 | Tariff management + PWA views | A4.4, A4.7 |
| S4.4 | Scalability validation + carried items | A4.8 |

## Source of Truth

> [`docs/ADD/implementation/phase4-inventory.md`](../../../docs/ADD/implementation/phase4-inventory.md)
