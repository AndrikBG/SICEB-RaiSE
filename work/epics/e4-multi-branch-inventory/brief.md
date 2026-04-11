# Epic E4 — Multi-Branch Operations and Inventory

## Hypothesis

Enabling full branch management with CQRS-based delta inventory, real-time WebSocket propagation, and temporal tariff configuration will allow the clinic network to scale from 3 to 15 branches with <10% performance degradation.

## Success Metrics

- Branch onboarding completes in <1 hour (ESC-01)
- Branch context switch without re-auth in <3 seconds (ESC-03)
- Inventory changes reflected across views in <2 seconds (PER-01)
- Per-branch queries unaffected by total branch count (ESC-02)
- Delta commands idempotent — zero duplicates on replay (CRN-43)

## Appetite

8 activities (A4.1–A4.8), ~17 tasks. Estimated 3-4 stories.

## Rabbit Holes

- Partition DDL automation: app DB role needs CREATE TABLE privileges
- pg_notify payload limit: 8000 bytes — validate JSON stays within
- Offline delta replay ordering depends on UTC clock sync (CRN-41) — Phase 6 concern
- SELECT ... FOR UPDATE conflicts with offline replay — Phase 6 will need different strategy

## Source of Truth

> Design, specifications, and task breakdown live in `docs/ADD/implementation/`.
> This file is a pointer, not a duplicate.

- **Phase plan:** [`docs/ADD/implementation/phase4-inventory.md`](../../../docs/ADD/implementation/phase4-inventory.md)
- **Requirements spec:** [`docs/ADD/implementation/requeriments4.md`](../../../docs/ADD/implementation/requeriments4.md)
- **Design decisions:** D-036 to D-043 in [`docs/ADD/implementation/progress.md`](../../../docs/ADD/implementation/progress.md)
- **Architecture:** [`docs/ADD/Design/Architecture.md`](../../../docs/ADD/Design/Architecture.md)
- **Iteration plan:** [`docs/ADD/Design/Iteration4.md`](../../../docs/ADD/Design/Iteration4.md)
