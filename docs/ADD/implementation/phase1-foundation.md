# Phase 1 — Foundation and System Structure

> **Spec:** `requeriments1.md` | **Status:** ✅ Completed
> **Drivers:** CRN-25, CRN-26, CRN-27, CRN-29, CRN-41, CRN-42, CRN-43, CON-01 to CON-05

**Goal:** Establish the complete project infrastructure, decompose the system into its four containers, create domain modules as stubs, implement the Shared Kernel, and configure CI/CD with automated architecture tests.

> **Environment:** Everything runs **locally with Docker**. Architecture is cloud-ready (env vars, no local paths, portable images) for migration to cloud without code changes.

---

## A1.0 — Local Docker Environment ✅

- [x] **T1.0.1** Create `docker-compose.yml` with services: PostgreSQL, API Server (Spring Boot) and PWA Client (Vite dev server)
  - `docker compose up` starts the full local stack; services communicate with each other
- [x] **T1.0.2** Configure PostgreSQL in Docker with persistent volume, user, database, and extensions (pg_trgm, pgcrypto)
  - DB accessible at `localhost:5432`; data persists across restarts
- [x] **T1.0.3** Create Dockerfile for API Server (Spring Boot) with multi-stage build
  - Image builds and runs the application; configurable via environment variables
- [x] **T1.0.4** Create Dockerfile for PWA Client (Node + Vite) with hot-reload for development
  - PWA accessible at `localhost:5173`; changes reflected without rebuild
- [x] **T1.0.5** Configure all application settings via environment variables (DB_URL, DB_USER, DB_PASSWORD, JWT_SECRET, etc.)
  - Zero hardcoded values; `.env` for local, platform variables for cloud
- [x] **T1.0.6** Create initialization script that runs Flyway migrations on API Server startup
  - Schema ready automatically on `docker compose up`
- [x] **T1.0.7** Document in README.md the commands to start, stop, and reset the local environment
  - Any developer can start the project with a single command

---

## A1.1 — Development Environment and CI/CD Setup ✅

- [x] **T1.1.1** Configure monorepo with directory structure for PWA Client and API Server
  - Repository accessible; structure reflects the container decomposition
- [x] **T1.1.2** Configure CI/CD pipeline with build, lint, test, and deploy stages (GitHub Actions)
  - Pipeline runs on every push; artifacts generated
- [x] **T1.1.3** Configure functional local development environment via Docker Compose (cloud provisioned post Phase 6)
  - `docker compose up` starts full stack; CI/CD runs against containers — *completed as part of A1.0*
- [x] **T1.1.4** Configure PostgreSQL in Docker with initial schema and Flyway migrations
  - Database accessible on localhost; migrations run without errors; `branch_id` schema deferred to A1.5
- [x] **T1.1.5** Configure static analysis and linting tools for frontend and backend
  - ESLint + TypeScript strict mode (frontend); Maven compiler plugin (backend); enforced in CI

## A1.2 — Shared Kernel Implementation ✅

- [x] **T1.2.1** Implement value type `Money(DECIMAL(19,4), MXN)` with banker's rounding
  - 13 unit tests: precision, banker's rounding (HALF_EVEN), arithmetic ops, no floating-point errors
- [x] **T1.2.2** Implement value type `UtcDateTime` (always UTC, conversion to `America/Mexico_City` only in presentation)
  - 8 unit tests: UTC storage, Mexico City conversion, DST handling, ISO 8601 format
- [x] **T1.2.3** Implement value type `EntityId` based on UUID (no auto-increment)
  - 8 unit tests: UUID v4 generation, uniqueness (1000 IDs), string parsing
- [x] **T1.2.4** Implement value type `IdempotencyKey` for safe replay of offline operations
  - 8 unit tests: generation, uniqueness, blank/null rejection
- [x] **T1.2.5** Define standardized error code catalog
  - 6 unit tests: ErrorCode enum (20 codes across 7 categories), ErrorResponse envelope with correlationId; unique codes verified

## A1.3 — API Server Scaffolding (Modular Monolith) ✅

- [x] **T1.3.1** Create base API Server structure as modular monolith with layer separation
  - Package tree: `com.siceb.{domain,platform,shared,config,api}` reflecting component diagram
- [x] **T1.3.2** Create stubs for 10 domain modules (Clinical Care, Prescriptions, Pharmacy, Laboratory, Inventory, Supply Chain, Scheduling, Billing & Payments, Reporting, Training)
  - `package-info.java` per module with IC-01 constraint, dependency docs, iteration schedule
- [x] **T1.3.3** Configure dependency rules between modules per directed acyclic graph (CRN-27)
  - Package structure enforces DAG; ArchUnit enforcement comes in A1.6
- [x] **T1.3.4** Implement REST API exposure with auto-generated OpenAPI documentation
  - OpenAPI 3.1 at `/api-docs`, Swagger UI at `/docs`; JWT security scheme; `SystemController` verified
- [x] **T1.3.5** Configure Secure WebSocket for real-time events
  - STOMP over `/ws` endpoint; broker on `/topic`, `/queue`; app prefix `/app`; CORS configured

## A1.4 — PWA Client Scaffolding ✅

- [x] **T1.4.1** Create PWA project with React + Vite, Service Worker and Web App Manifest
  - vite-plugin-pwa 1.2.0 with Workbox `generateSW`; `registerType: 'prompt'` for user-controlled updates
  - Web App Manifest with SICEB branding (`theme_color: #863bff`, standalone display, SVG icons 192/512)
  - `PwaUpdatePrompt` component for offline-ready and update notifications
  - Service Worker precaches 11 entries (220 KB); API calls cached with `NetworkFirst` strategy
- [x] **T1.4.2** Configure IndexedDB (Dexie.js) as Local Storage with `branch_id` isolation
  - `SicebDatabase` with 3 tables: `syncQueue`, `cachedEntities`, `appSettings`
  - All tenant-scoped tables indexed by `branchId`; compound indexes for `[branchId+entityType]`, `[branchId+status]`
  - Scoped query helpers: `scopedSyncQueue()`, `scopedCachedEntities()`, `pendingSyncCount()`
- [x] **T1.4.3** Implement base State Management structure (Zustand)
  - `useAuthStore`: user session, JWT token, persist to `sessionStorage`
  - `useUiStore`: sidebar, theme (light/dark/system), toasts; persist to `localStorage`
  - `useSyncStore`: connection status (online/offline/reconnecting), pending changes, sync state
  - `useOnlineStatus` hook: listens to browser online/offline events
- [x] **T1.4.4** Implement API Client (Axios + @stomp/stompjs)
  - Axios instance (`api-client.ts`): base URL via `VITE_API_URL`, 15s timeout, JWT interceptor, auto-logout on 401
  - STOMP client (`ws-client.ts`): `beforeConnect` injects JWT, auto-reconnect (5s), heartbeat 10s; `subscribeTopic()` and `publishMessage()` helpers
- [x] **T1.4.5** Verify compatibility with latest 2 versions of Chrome, Edge, Safari and Firefox
  - Browserslist config: `last 2 Chrome/Edge/Safari/Firefox versions`; `build.target: 'es2023'`; production build verified (196 KB JS + 12 KB CSS gzipped)

## A1.5 — Multi-Tenant Database Configuration ✅

- [x] **T1.5.1** Design base PostgreSQL schema with `branch_id` on all tenant-scoped tables
  - V002 migration: `branches` table (UUID PK, name, address, is_active, timestamps) — tenant anchor for CRN-29
  - Seed data: dev branch `00000000-0000-4000-a000-000000000001`; all future tenant-scoped tables FK to `branches.id`
- [x] **T1.5.2** Configure Row-Level Security (RLS) at the database level
  - V003 migration: `current_branch_id()` function reads `SET LOCAL app.branch_id`; `apply_rls_policy()` helper enables RLS + creates tenant isolation policy on any table
  - Repeatable migration `R__rls_policies.sql` — re-applies policies when tables are added (Phase 2+)
  - Spring side: `TenantContext` (ThreadLocal), `TenantFilter` (reads `X-Branch-Id` header; JWT in Phase 3), `TenantAwareDataSource` (wraps HikariCP, injects `SET LOCAL` per connection)
  - `TenantConnectionInterceptor` (Hibernate `CurrentTenantIdentifierResolver`) for `@TenantId` discriminator support
  - Defense-in-depth: Hibernate discriminator filtering + PostgreSQL RLS
  - 9 unit tests: TenantContext (6) + TenantFilter (3)
- [x] **T1.5.3** Configure connection pool and TLS encryption for SQL communication
  - HikariCP: pool name `siceb-pool`, 20s connect timeout, 5min idle, 15min max lifetime, 30s leak detection
  - TLS via `DB_SSL_PARAMS` env var (empty for local Docker; `?sslmode=require` for cloud) — appended to JDBC URL
- [x] **T1.5.4** Implement versioned migration framework (Flyway)
  - 3 versioned migrations (V001–V003) + 1 repeatable (R__rls_policies)
  - Naming convention: `V{NNN}__description.sql` (versioned), `R__description.sql` (repeatable)
  - Flyway history table verified; all 4 migrations execute in order on fresh DB

## A1.6 — Automated Architecture Tests ✅

- [x] **T1.6.1** Implement ArchUnit tests verifying domain modules are empty stubs (IC-01)
  - `DomainStubsArchTest`: 4 tests — no business logic, no @Entity, no Spring components in domain stubs; all 10 modules present
  - CI rejects PRs that introduce domain logic before its iteration
- [x] **T1.6.2** Implement ArchUnit tests verifying acyclic dependency graph (CRN-27)
  - `DependencyArchTest`: 9 tests — domain cycle-free, top-level cycle-free, shared→nothing, domain→no api/config, platform→no domain/api
  - Circular dependencies detected as build error
- [x] **T1.6.3** Implement tests verifying offline-aware conventions: UUID-only IDs, IdempotencyKey
  - `OfflineConventionsArchTest`: 3 tests — @Id must be UUID/EntityId, no @GeneratedValue, IdempotencyKey exists in shared
  - Violations detected automatically in CI

---

## Deliverables

- [x] **E1.1** Repository configured with CI/CD — Monorepo with functional pipeline
- [x] **E1.2** Shared Kernel implemented — Value types Money, UtcDateTime, EntityId, IdempotencyKey with tests (43 tests pass)
- [x] **E1.3** API Server scaffold — Modular monolith with 10 domain stubs, 4 platform stubs, OpenAPI 3.1, WebSocket STOMP
- [x] **E1.4** PWA Client scaffold — Installable PWA with Service Worker, IndexedDB (Dexie.js, 3 tables), Zustand (3 stores), Axios + STOMP client
- [x] **E1.5** PostgreSQL database — Multi-tenant schema (branches table, RLS infrastructure, Flyway V001–V003 + repeatable), HikariCP tuning, TLS-ready via env var
- [x] **E1.6** Architecture test suite — ArchUnit validating stubs (4 tests), dependencies (9 tests), offline conventions (3 tests) — 16 tests total
- [x] **E1.7** Local Docker environment — Functional `docker-compose.yml` with PostgreSQL, API Server and PWA Client; README with instructions

---

## Notes and Decisions

| # | Date | Decision | Context |
|---|------|----------|---------|
| D-003 | 2026-03-23 | UI Library: **Shadcn/ui + Tailwind CSS v4** | Lighter bundle for PWA, no runtime CSS-in-JS, full styling control via Tailwind. Ant Design discarded: heavier bundle, less PWA-friendly |
| D-004 | 2026-03-23 | Package manager: **pnpm** (replaces npm) | npm install hung repeatedly on Windows; pnpm resolved in ~3 min. Faster installs, stricter dependency resolution, disk-efficient |
| D-005 | 2026-03-23 | CI/CD platform: **GitHub Actions** | Two parallel jobs: backend (Maven + Testcontainers PostgreSQL) and frontend (pnpm + lint + build) |
| D-006 | 2026-03-23 | **A1.0 and A1.1 merged** — overlapping tasks (Docker Compose, PostgreSQL, Flyway) implemented once in A1.0; A1.1 counterparts marked completed by reference | Avoids duplicate work; monorepo structure and CI/CD created as prerequisites for Docker setup |
| D-007 | 2026-03-23 | Spring Boot **3.5.12** (latest 3.x); explicit Hibernate dialect removed (auto-detected since Hibernate 6.x) | Spring Boot 4.0.4 available but plan specifies 3.x; staying on 3.5.x for stability |
| D-008 | 2026-03-23 | PWA: **vite-plugin-pwa 1.2.0** with `registerType: 'prompt'` | User controls when to update; Workbox `generateSW` mode for zero-config SW. Peer dep warning for Vite 8 (supports up to 7.x) — build works, tracked for upgrade |
| D-009 | 2026-03-23 | IndexedDB schema: **3 tables** (syncQueue, cachedEntities, appSettings) | syncQueue for offline-first mutations; cachedEntities for read cache with version tracking; appSettings for branch-agnostic config |
| D-010 | 2026-03-23 | Auth token stored in **sessionStorage** (not localStorage) | Reduces XSS token-theft window; tab-isolated sessions; cleared on browser close |
| D-011 | 2026-03-23 | **workbox-window** installed explicitly as dependency | Vite 8's Rolldown bundler doesn't resolve virtual module transitive deps from vite-plugin-pwa; explicit install resolves build error |
| D-012 | 2026-03-23 | Multi-tenant: **shared DB with `branch_id` discriminator** (CRN-29) | Single `branches` table as tenant anchor; all tenant-scoped tables will FK to it. Hibernate `@TenantId` + PostgreSQL RLS as defense-in-depth |
| D-013 | 2026-03-23 | RLS approach: **`SET LOCAL app.branch_id`** per connection via `TenantAwareDataSource` | Transaction-scoped — auto-cleared on commit/rollback, no stale state in pool. `current_branch_id()` SQL function reads the setting for RLS policies |
| D-014 | 2026-03-23 | TLS: **`DB_SSL_PARAMS` env var** appended to JDBC URL | Empty for local Docker (no TLS overhead), `?sslmode=require` for cloud. Avoids code changes between environments |
| D-015 | 2026-03-23 | Tenant header: **`X-Branch-Id`** (Phase 1 dev); migrates to **JWT claims** in Phase 3 | Allows testing multi-tenancy before auth is implemented |
| D-016 | 2026-03-23 | ArchUnit tests in **`com.siceb.architecture`** package — 3 test classes (stubs, deps, offline) | Separate `architecture` test package isolates structural rules from unit tests; static `ClassFileImporter` shared per class for performance |

### Verified Stack Versions

| Component | Version |
|-----------|---------|
| Spring Boot | 3.5.12 |
| Java | 21 (Eclipse Temurin) |
| PostgreSQL | 17-alpine |
| React | 19.2.4 |
| Vite | 8.0.2 |
| TypeScript | 5.9.3 |
| Tailwind CSS | 4.2.2 |
| pnpm | 10.28.1 |
| Docker Compose | v2 (Docker 29.1.3) |
| vite-plugin-pwa | 1.2.0 |
| workbox-window | 7.4.0 |
| Dexie.js | 4.3.x |
| Zustand | 5.0.x |
| Axios | 1.13.x |
| @stomp/stompjs | 7.3.x |

