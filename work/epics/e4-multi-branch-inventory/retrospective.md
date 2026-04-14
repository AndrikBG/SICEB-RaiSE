# Epic Retrospective: E4 Multi-Branch Operations and Inventory

**Completed:** 2026-04-13
**Duration:** 3 days (started 2026-04-11)
**Stories:** 5 stories delivered

---

## Summary

Epic E4 delivered full multi-branch operations: branch management with 5-step onboarding, CQRS-based inventory with append-only delta store and PostgreSQL trigger materialization, real-time WebSocket propagation via pg_notify→STOMP bridge, temporal tariff configuration, 4 PWA views, and scalability validation from 3→15 branches. ESC-02 validated — all queries improved at 15 branches due to declarative partitioning with partition pruning.

## Metrics

| Metric | Value | Notes |
|--------|-------|-------|
| Stories Delivered | 5 | S4.1–S4.5, all sizes XS–L |
| Tasks Completed | 23 | across all stories |
| Tests Added | 205 | unit + integration (Testcontainers) |
| Average Velocity | 1.09x | weighted across stories |
| Calendar Days | 3 | 2026-04-11 to 2026-04-13 |
| Commits | 53 | on main branch |

### Story Breakdown

| Story | Size | Velocity | Key Learning |
|-------|:----:|:--------:|--------------|
| S4.1 Branch management + context switching | M | 0.83x | RLS infrastructure reuse accelerates new domain modules |
| S4.2 Inventory CQRS + real-time | L | 0.89x | PG triggers + pg_notify are powerful but require careful integration test design |
| S4.3 Tariff configuration | S | 1.11x | Temporal effective-date pattern is clean and reusable |
| S4.4 PWA views | M | 1.70x | TypeScript views with established API patterns flow fast |
| S4.5 Scalability validation | S | 0.90x | k6 + Testcontainers complementary: load tests need realistic stack, EXPLAIN needs fast feedback |

## What Went Well

- **Declarative partitioning worked exactly as designed** — partition pruning confirmed via EXPLAIN ANALYZE, zero degradation at 15 branches
- **RLS infrastructure from Phase 2 paid off** — `current_branch_id()` function reused across all new tables without modification
- **pg_notify→STOMP bridge** delivered real-time inventory updates end-to-end with clean separation (DB trigger → NOTIFY → Java LISTEN → STOMP publish)
- **Testcontainers singleton pattern** — consistent, fast integration tests for triggers, RLS, partitioning, and schema validation
- **CQRS with append-only deltas** — clean audit trail, idempotency enforcement via unique constraint, PG trigger materialization keeps stock consistent
- **PWA velocity (1.7x)** — established API client patterns + Angular Material components made frontend integration fast
- **k6 load testing** worked cleanly after initial auth race condition fix — reusable scripts for future phases

## What Could Be Improved

- **Docker Compose image staleness** — wasted time in S4.2 and S4.5 when containers didn't have latest migrations. Need an infra-preflight step for stories that depend on Docker Compose
- **psql meta-command portability** — `\gset` and `:'var'` don't work through docker exec pipes. Discovered during S4.5, forced seed script refactor to sed template substitution
- **WebSocket STOMP load testing** — k6's raw WebSocket module doesn't handle STOMP framing natively. Formal WS load test deferred to Phase 6. A dedicated STOMP k6 extension or Artillery would be needed
- **S4.1 underestimated (0.83x)** — 5-step onboarding had more moving parts than anticipated (audit events, permission seeding, concurrent onboarding guard)
- **Concurrent JWT race condition in k6** — all VUs sharing one token caused 500 errors. Pre-generating per-branch tokens in setup() solved it, but cost debugging time
- **Append-only trigger affects test cleanup** — `prevent_delta_mutation()` blocks DELETE on inventory_deltas. Tests must DROP partition tables instead. This surprised us in both S4.2 and S4.5

## Patterns Discovered

| ID | Pattern | Context |
|----|---------|---------|
| PAT-A-001 | SQL seed scripts must use template placeholders, not psql meta-commands | When seeding via docker exec or CI pipes |
| PAT-A-002 | Pre-generate per-branch tokens for concurrent load tests | k6 or any multi-VU load testing with JWT auth |
| PAT-E-539 | Integration stories MUST include real-infrastructure AC | When client/server developed separately with mocks |
| — | Append-only tables require DROP TABLE for test cleanup, not DELETE | Integration tests touching append-only delta stores |
| — | `jdbc.execute()` not `jdbc.update()` for VOID-returning PG functions | JUnit + JdbcTemplate calling PG functions |

## Process Insights

- **Risk-first sequencing validated** — S4.2 (L, highest risk) early caught the trigger/cleanup/pg_notify integration issues before they could cascade to later stories
- **Small stories after large ones build confidence** — S4.3 (1.11x) after S4.2 (0.89x) restored momentum and proved the patterns
- **PWA as integration story works well** — S4.4 consumed all backend APIs and served as the E2E checkpoint. Separating backend and frontend stories with a final integration story is a repeatable pattern
- **Scalability validation as final story is correct sequencing** — S4.5 needs real data from all prior stories; running it last maximizes coverage
- **Velocity improves within an epic** — S4.1 (0.83x) → S4.5 (0.90x) with S4.3 (1.11x) and S4.4 (1.70x) peaks. Pattern reuse and domain familiarity compound

## Decisions Made

| ID | Decision | Rationale |
|----|----------|-----------|
| D-037 | LIST partitioning by branch_id (not HASH or RANGE) | Exact-match queries, dynamic partition creation, clean partition pruning |
| D-038 | Append-only delta store with PG trigger materialization | Audit trail + idempotency + transactional consistency, no application-level sync |
| D-039 | pg_notify → STOMP bridge (not polling, not external MQ) | Zero additional infrastructure, sub-2s latency, sufficient for ~150 connections |
| D-040 | SimpleBroker (not RabbitMQ/Redis relay) | Single-instance sufficient for target scale; horizontal scaling deferred to Phase 6 |
| D-041 | Temporal effective-date pattern for tariffs | Immutable history, single query for active price, no UPDATE mutations |
| D-045 | k6 for load testing | JavaScript-based, native WebSocket support, CLI-first, lightweight |
| D-046 | Docker Compose for load tests, Testcontainers for EXPLAIN | Load tests need realistic stack; EXPLAIN only needs DB |
| D-047 | SQL seed scripts with sed template substitution | Portable across psql/docker exec/CI; deterministic and version-controlled |

## Artifacts

- **Scope:** `work/epics/e4-multi-branch-inventory/scope.md`
- **Stories:** `work/epics/e4-multi-branch-inventory/stories/`
- **Scalability report:** `work/epics/e4-multi-branch-inventory/scalability-report.md`
- **k6 scripts:** `tests/scalability/`
- **Migrations:** V014–V018 (branches, onboarding, service catalog, permissions, inventory)
- **Tests:** 205 (unit + integration)
- **PWA views:** 4 (branch management, inventory dashboard, service inventory, tariff config)

## Next Steps

- **Phase 5:** Supply request workflow and pharmacy dispensing (depends on E4 branch + inventory infra)
- **Phase 6:** Offline sync for inventory, horizontal WebSocket scaling
- **Deferred:** Formal WebSocket STOMP load test (150 concurrent connections) — architecture-validated, formal test when horizontal scaling is in scope
- **Deferred:** Browser E2E verification of PWA views in Docker Compose (M3 code complete, browser testing pending)
