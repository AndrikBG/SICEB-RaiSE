# Summary — Phase 1 (Completed)

> Last updated: 2026-03-23 | Activities completed: A1.0, A1.1, A1.2, A1.3, A1.4, A1.5, A1.6 | Tasks: 34/34

---

## Files Added

### Root Level

| File | Purpose |
|------|---------|
| `docker-compose.yml` | Orchestrates 3 services: `db` (PostgreSQL 17), `api` (Spring Boot), `pwa` (Vite dev server). DB health check gates API startup. PWA mounts `src/` as volume for hot-reload. |
| `.env.example` | Template with all env vars: DB credentials, JWT config, ports, CORS origins. Copied to `.env` for local use. |
| `.env` | Local instance of env vars (gitignored). |
| `README.md` | Quickstart guide, project structure, tech stack table, development commands for Docker, backend, frontend, and database. |
| `.github/workflows/ci.yml` | GitHub Actions CI with 2 parallel jobs: **backend** (Maven + PostgreSQL service container) and **frontend** (pnpm install + lint + build). |
| `.gitignore` | Updated — added `backend/target/`, `frontend/node_modules/`, `frontend/dist/`, `.env`, IDE files, OS files. |

### Backend (`backend/`)

| File | Purpose |
|------|---------|
| `pom.xml` | Maven project: Spring Boot 3.5.12, Java 21. Dependencies: web, data-jpa, security, websocket, actuator, validation, postgresql, flyway, springdoc-openapi 2.8.6, jjwt 0.12.6, lombok, micrometer-prometheus, testcontainers, archunit 1.3.0. |
| `Dockerfile` | Single-step Maven build inside Docker (`mvn package -DskipTests`) with retry config for slow networks. Runtime: `eclipse-temurin:21-jre-alpine`. Health check via `wget` to `/actuator/health`. |
| `.dockerignore` | Excludes `target/`, `.git`, docs, IDE files from Docker context. |
| `src/main/java/com/siceb/SicebApplication.java` | Spring Boot entry point. Minimal — just `@SpringBootApplication` + `main()`. |
| `src/main/java/com/siceb/config/SecurityConfig.java` | Stateless security config: CSRF disabled, CORS from env var `cors.allowed-origins`, all requests permitted (Phase 3 will enforce auth). Actuator and Swagger endpoints explicitly allowed. |
| `src/main/resources/application.yml` | All config via env vars with defaults: datasource (`DB_URL`, `DB_USER`, `DB_PASSWORD`), HikariCP pool, JPA (validate mode, UTC timezone), Flyway, springdoc (`/docs`, `/api-docs`), actuator (health, metrics, prometheus, liveness, readiness), JWT settings, structured logging. |
| `src/main/resources/db/migration/V001__init_extensions.sql` | Flyway migration: enables `pg_trgm` (fuzzy search) and `pgcrypto` (UUID generation, hashing). |
| `src/test/java/com/siceb/SicebApplicationTests.java` | Minimal unit test verifying the application class exists. No Spring context needed. |

### Frontend (`frontend/`)

| File | Purpose |
|------|---------|
| `package.json` | React 19.2.4, Vite 8.0.2, TypeScript 5.9.3. Runtime deps: tailwindcss 4.2.2, @tailwindcss/vite, zustand, dexie, axios, @stomp/stompjs, react-hook-form, zod, react-router-dom. Dev deps: eslint, typescript-eslint, @vitejs/plugin-react. |
| `pnpm-lock.yaml` | Lockfile for deterministic installs. |
| `vite.config.ts` | Plugins: react + tailwindcss. Path alias `@/` → `src/`. Dev server: host `0.0.0.0`, port 5173, strict port. |
| `tsconfig.json` | References `tsconfig.app.json` and `tsconfig.node.json`. |
| `tsconfig.app.json` | ES2023 target, strict mode, `@/*` path alias, React JSX, no unused locals/params. |
| `index.html` | Entry point with title "SICEB", mounts React to `#root`. |
| `src/main.tsx` | React 19 `createRoot` with `StrictMode`, imports `index.css` and `App`. |
| `src/App.tsx` | Placeholder landing page: SICEB title, subtitle, green "System Online" badge. Uses Tailwind utility classes. |
| `src/index.css` | Single line: `@import "tailwindcss"` (Tailwind v4 entry point). |
| `Dockerfile` | Node 22 alpine, corepack enables pnpm 10, frozen lockfile install, runs `pnpm dev`. |
| `.dockerignore` | Excludes `node_modules/`, `dist/`, `.git`, docs. |

---

## Files Modified

| File | Change |
|------|--------|
| `.gitignore` | Replaced original (Obsidian-only ignores) with comprehensive rules for backend, frontend, env, IDE, OS, logs. Original patterns preserved. |

---

## Files Deleted

| File | Reason |
|------|--------|
| `frontend/src/App.css` | Replaced by Tailwind CSS utilities in `App.tsx`. |
| `frontend/src/assets/react.svg` | Default Vite template asset, unused. |
| `frontend/src/assets/vite.svg` | Default Vite template asset, unused. |
| `frontend/src/assets/hero.png` | Default Vite template asset, unused. |
| `frontend/public/icons.svg` | Default Vite template asset, unused. |

---

## How Components Relate

```
docker-compose.yml
├── db (postgres:17-alpine)
│   ├── Port: 5432 (host) → 5432 (container)
│   ├── Volume: pgdata (persistent)
│   └── Health check: pg_isready
│
├── api (backend/Dockerfile)
│   ├── Port: 8080 (host) → 8080 (container)
│   ├── Depends on: db (healthy)
│   ├── Env vars: DB_URL → jdbc:postgresql://db:5432/siceb
│   ├── On startup: Flyway runs V001__init_extensions.sql
│   └── Exposes: /actuator/health, /docs (Swagger), /api-docs
│
└── pwa (frontend/Dockerfile)
    ├── Port: 5173 (host) → 5173 (container)
    ├── Depends on: api
    ├── Volume mount: ./apps/frontend/src → /app/src (hot-reload)
    └── Env var: VITE_API_URL → http://localhost:8080
```

### Data Flow

```
Browser → :5173 (Vite) → React App
                            ↓
                     Axios HTTP client
                            ↓
                    :8080 (Spring Boot)
                     ├── SecurityConfig (CORS, stateless)
                     ├── Flyway (DB migrations)
                     └── JPA → PostgreSQL :5432
```

### Configuration Flow

```
.env.example → (copy) → .env → docker-compose.yml → container env vars
                                                        ↓
                              backend: application.yml reads ${DB_URL}, ${JWT_SECRET}, etc.
                              frontend: VITE_API_URL available at build time
```

---

## Key Architectural Decisions Made

1. **pnpm over npm** — npm hung repeatedly on Windows; pnpm installs reliably and faster
2. **Shadcn/ui + Tailwind v4** — lighter PWA bundle, no runtime CSS-in-JS, full styling control
3. **Spring Boot 3.5.12** — latest 3.x stable; 4.0.4 exists but plan specified 3.x
4. **A1.0/A1.1 merged** — overlapping Docker/PostgreSQL/Flyway tasks implemented once
5. **Single Maven step in Dockerfile** — `dependency:go-offline` failed on slow network; combined into `mvn package` with retry flags
6. **Hibernate dialect auto-detection** — removed explicit `PostgreSQLDialect` setting (deprecated since Hibernate 6.x)

---

## A1.2 — Shared Kernel (Added)

### Value Types (`backend/src/main/java/com/siceb/shared/`)

| File                  | Type                                  | Purpose                                                                                                                                                                                                       |
| --------------------- | ------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `Money.java`          | `record Money(BigDecimal amount)`     | DECIMAL(19,4), banker's rounding (HALF_EVEN), MXN. Arithmetic: add, subtract, multiply, negate. Comparisons: isPositive, isNegative, isZero, isGreaterThan, isLessThan. Jackson: `@JsonValue`/`@JsonCreator`. |
| `UtcDateTime.java`    | `record UtcDateTime(Instant value)`   | Always UTC. Factory: `now()`, `of(Instant)`, `of(ZonedDateTime)`. Presentation: `toMexicoCity()`. Temporal: `isBefore`, `isAfter`.                                                                            |
| `EntityId.java`       | `record EntityId(UUID value)`         | UUID v4 via `generate()`. Parsing: `of(UUID)`, `of(String)`. Offline-safe — no server coordination needed.                                                                                                    |
| `IdempotencyKey.java` | `record IdempotencyKey(String value)` | Client-generated key for write commands. Rejects null/blank. UUID-based generation via `generate()`.                                                                                                          |
| `ErrorCode.java`      | `enum ErrorCode`                      | 20 codes across 7 categories: System (0xxx), Validation (1xxx), Auth (2xxx), Resource (3xxx), Conflict (4xxx), Multi-tenant (5xxx), Business (6xxx).                                                          |
| `ErrorResponse.java`  | `record ErrorResponse(...)`           | Envelope: `{ code, message, correlationId, timestamp, details }`. Factory methods from `ErrorCode`.                                                                                                           |

### JPA Converters (`backend/src/main/java/com/siceb/shared/converter/`)

| File | Converts | DB Type |
|------|----------|---------|
| `MoneyConverter.java` | `Money` ↔ `BigDecimal` | `DECIMAL(19,4)` |
| `UtcDateTimeConverter.java` | `UtcDateTime` ↔ `Instant` | `TIMESTAMPTZ` |
| `EntityIdConverter.java` | `EntityId` ↔ `UUID` | `UUID` |
| `IdempotencyKeyConverter.java` | `IdempotencyKey` ↔ `String` | `VARCHAR` |

All converters use `@Converter(autoApply = true)` — automatically applied wherever these types appear in JPA entities.

### Tests (`backend/src/test/java/com/siceb/shared/`)

| File | Tests | Key Verifications |
|------|-------|-------------------|
| `MoneyTest.java` | 13 | Scale enforcement, banker's rounding, `0.1+0.2=0.3`, IVA proration precision |
| `UtcDateTimeTest.java` | 8 | UTC storage, Mexico City conversion (UTC-6), `ZonedDateTime` → UTC conversion |
| `EntityIdTest.java` | 8 | UUID v4 version check, 1000-ID uniqueness, string round-trip |
| `IdempotencyKeyTest.java` | 8 | Generation, blank rejection, uniqueness |
| `ErrorResponseTest.java` | 6 | Factory methods, unique error codes, correlation ID uniqueness |

**Total: 43 tests, all passing.**

---

## A1.3 — API Server Scaffolding (Added)

### Config files (`backend/src/main/java/com/siceb/config/`)

| File | Purpose |
|------|---------|
| `OpenApiConfig.java` | Bean producing `OpenAPI` spec: title "SICEB API", version 0.1.0, JWT Bearer security scheme. Exposed at `/docs` (Swagger UI) and `/api-docs` (JSON). |
| `WebSocketConfig.java` | STOMP over WebSocket: endpoint `/ws`, broker prefixes `/topic` and `/queue`, app prefix `/app`. CORS from `cors.allowed-origins` env var. |

### API endpoints (`backend/src/main/java/com/siceb/api/`)

| File | Purpose |
|------|---------|
| `SystemController.java` | `GET /api/system/info` — returns app name, version, status. Annotated with Swagger `@Operation` and `@Tag`. |

### Domain module stubs (`backend/src/main/java/com/siceb/domain/`)

10 modules, each containing only a `package-info.java` with IC-01 constraint documentation:

| Package | Module | Dependencies (outgoing) | Iteration |
|---------|--------|------------------------|-----------|
| `clinicalcare` | Clinical Care | Prescriptions, Laboratory, Scheduling | 2 |
| `prescriptions` | Prescriptions | Pharmacy | 2 |
| `pharmacy` | Pharmacy | Inventory | 2 |
| `laboratory` | Laboratory | — | 2 |
| `inventory` | Inventory | Supply Chain | 2 |
| `supplychain` | Supply Chain | — | 2 |
| `scheduling` | Scheduling | — | 2 |
| `billing` | Billing & Payments | — | 3 |
| `reporting` | Reporting | — | 3 |
| `training` | Training | — | 3 |

### Platform module stubs (`backend/src/main/java/com/siceb/platform/`)

4 modules, each containing only a `package-info.java`:

| Package | Module | Scope |
|---------|--------|-------|
| `iam` | Identity & Access | Phase 3 — RBAC, auth, user mgmt |
| `branch` | Branch Management | Multi-tenant branch isolation |
| `audit` | Audit Trail | Event logging, compliance |
| `sync` | Offline Sync | Conflict resolution, queue processing |

### How A1.3 relates to existing code

```
com.siceb
├── SicebApplication.java          (A1.0 — entry point)
├── config/
│   ├── SecurityConfig.java        (A1.0 — CORS, stateless)
│   ├── OpenApiConfig.java         (A1.3 — Swagger/OpenAPI 3.1)
│   └── WebSocketConfig.java       (A1.3 — STOMP WebSocket)
├── api/
│   └── SystemController.java      (A1.3 — health/info endpoint)
├── shared/                        (A1.2 — value types + converters)
├── domain/                        (A1.3 — 10 stub packages)
└── platform/                      (A1.3 — 4 stub packages)
```

---

## A1.4 — PWA Client Scaffolding (Added)

### Files Added

| File | Purpose |
|------|---------|
| `src/hooks/use-pwa.ts` | React hook wrapping `useRegisterSW` from vite-plugin-pwa. Exposes `offlineReady`, `needRefresh`, `updateServiceWorker`, `close`. Auto-checks for SW updates hourly. |
| `src/hooks/use-online-status.ts` | Listens to browser `online`/`offline` events, syncs status to `useSyncStore`. |
| `src/components/pwa-update-prompt.tsx` | Floating toast (bottom-right) for offline-ready and update-available notifications. "Actualizar" button triggers SW update, "Cerrar" dismisses. |
| `src/lib/db.ts` | Dexie.js `SicebDatabase` with 3 tables: `syncQueue` (offline mutation queue with idempotency keys, retry tracking), `cachedEntities` (read cache with version), `appSettings` (branch-agnostic config). Scoped query helpers enforce `branchId` isolation. |
| `src/stores/auth-store.ts` | Zustand store: `user`, `accessToken`, `isAuthenticated`. Actions: `setSession`, `clearSession`, `updateToken`. Persisted to `sessionStorage` (XSS mitigation — D-010). |
| `src/stores/ui-store.ts` | Zustand store: `sidebarOpen`, `theme` (light/dark/system), `toasts`. Actions: `toggleSidebar`, `setTheme`, `addToast`, `removeToast`. Persisted to `localStorage` (sidebar + theme only). |
| `src/stores/sync-store.ts` | Zustand store: `connectionStatus` (online/offline/reconnecting), `pendingChanges`, `lastSyncAt`, `isSyncing`. No persistence — derived from runtime state. |
| `src/lib/api-client.ts` | Axios instance: base URL from `VITE_API_URL` (default `localhost:8080`), 15s timeout. Request interceptor injects JWT from `useAuthStore`. Response interceptor auto-clears session on 401. |
| `src/lib/ws-client.ts` | STOMP client (singleton): `beforeConnect` injects JWT, 5s auto-reconnect, 10s heartbeat. Helpers: `connectWs()`, `disconnectWs()`, `subscribeTopic()`, `publishMessage()`. Updates `useSyncStore` on connect/disconnect/error. |
| `public/icons/icon-192.svg` | PWA icon 192×192 — purple rounded square with "S" letter. |
| `public/icons/icon-512.svg` | PWA icon 512×512 — same design, maskable variant also referenced. |

### Files Modified

| File | Change |
|------|--------|
| `vite.config.ts` | Added `VitePWA` plugin (generateSW, prompt register, manifest with SICEB branding, API NetworkFirst cache, devOptions enabled). Added `build.target: 'es2023'`. |
| `index.html` | Changed `lang` to `es`. Added `apple-touch-icon`, `theme-color` meta (`#863bff`), `description` meta. |
| `tsconfig.app.json` | Added `vite-plugin-pwa/react` and `vite-plugin-pwa/info` to `types` for virtual module typings. |
| `package.json` | Added `workbox-window` (runtime dep), `vite-plugin-pwa` (dev dep), `browserslist` field (last 2 versions of Chrome/Edge/Safari/Firefox). |
| `src/App.tsx` | Replaced static "System Online" badge with dynamic connection status indicator (green/red/amber) from `useOnlineStatus`. Added pending sync changes counter. Integrated `PwaUpdatePrompt` component. |

### How A1.4 relates to existing code

```
frontend/src/
├── main.tsx                            (A1.0 — entry point, unchanged)
├── App.tsx                             (A1.0 → A1.4 — now uses stores, hooks, PWA prompt)
├── index.css                           (A1.0 — Tailwind entry, unchanged)
├── components/
│   └── pwa-update-prompt.tsx           (A1.4 — uses use-pwa hook)
├── hooks/
│   ├── use-pwa.ts                      (A1.4 — wraps vite-plugin-pwa SW registration)
│   └── use-online-status.ts            (A1.4 — browser events → sync store)
├── lib/
│   ├── db.ts                           (A1.4 — Dexie.js IndexedDB, 3 tables)
│   ├── api-client.ts                   (A1.4 — Axios, JWT interceptor → auth store)
│   └── ws-client.ts                    (A1.4 — STOMP, JWT from auth store → sync store)
└── stores/
    ├── auth-store.ts                   (A1.4 — session state, used by api-client + ws-client)
    ├── ui-store.ts                     (A1.4 — UI preferences, standalone)
    └── sync-store.ts                   (A1.4 — connectivity state, used by ws-client + App)
```

### Store → Client dependency graph

```
useAuthStore ←── api-client.ts (reads token for Authorization header)
     ↑               ↑
     │               └── 401 response → clearSession()
     │
useAuthStore ←── ws-client.ts (reads token in beforeConnect)
                      │
                      └──→ useSyncStore (writes connectionStatus on connect/disconnect)
                                ↑
                                └── use-online-status.ts (writes on browser online/offline events)
```

### Build output (production)

```
dist/
├── index.html                     0.71 KB
├── manifest.webmanifest           0.54 KB
├── sw.js                          (Workbox service worker, precaches 11 entries ~220 KB)
├── workbox-a1d84f0b.js            (Workbox runtime)
├── favicon.svg
├── icons/
│   ├── icon-192.svg
│   └── icon-512.svg
└── assets/
    ├── index-*.css               12.31 KB (gzip: 3.39 KB)
    ├── index-*.js               196.91 KB (gzip: 62.44 KB)
    └── workbox-window.prod.es5-*.js  5.74 KB (gzip: 2.25 KB)
```

---

## Key Architectural Decisions (A1.3 + A1.4)

7. **Domain stubs as `package-info.java` only** — enforces IC-01 constraint; no classes allowed until the module's iteration
8. **PWA `registerType: 'prompt'`** — user controls when to update, avoids surprise reloads during clinical data entry
9. **IndexedDB 3-table schema** — `syncQueue` for offline writes, `cachedEntities` for read cache, `appSettings` for config
10. **Auth in sessionStorage** — reduces XSS attack window vs localStorage; tab-isolated; cleared on browser close
11. **workbox-window explicit install** — Vite 8 Rolldown can't resolve transitive virtual module deps from vite-plugin-pwa

---

## A1.5 — Multi-Tenant Database Configuration (Added)

### Flyway Migrations (`backend/src/main/resources/db/migration/`)

| File | Type | Purpose |
|------|------|---------|
| `V002__create_branches_table.sql` | Versioned | Creates `branches` table: UUID PK (gen_random_uuid), name (VARCHAR 100), address (TEXT), is_active (BOOLEAN), created_at/updated_at (TIMESTAMPTZ). Seeds dev branch `00000000-0000-4000-a000-000000000001`. |
| `V003__create_rls_infrastructure.sql` | Versioned | Creates `current_branch_id()` function (reads `SET LOCAL app.branch_id` from PostgreSQL session). Creates `apply_rls_policy(table_name)` helper: enables RLS + FORCE RLS, creates tenant isolation policy (`USING branch_id = current_branch_id()`). Idempotent — safe to call multiple times. |
| `R__rls_policies.sql` | Repeatable | Re-applies RLS policies to all tenant-scoped tables. Currently empty (IC-01: no domain tables in Phase 1). Tables added here as domain migrations create them in Phase 2+. |

### Spring Multi-Tenant Infrastructure (`backend/src/main/java/com/siceb/platform/branch/`)

| File | Purpose |
|------|---------|
| `TenantContext.java` | ThreadLocal holder for the current `branch_id` (UUID). Static methods: `set()`, `get()`, `require()`, `clear()`. Thread-isolated — each request has its own tenant context. |
| `TenantFilter.java` | Servlet `Filter` (`@Order(1)`) — reads `X-Branch-Id` header and stores in `TenantContext`. Guarantees `clear()` in `finally` block even on exceptions. Phase 3 will read from JWT claims instead. |
| `TenantConnectionInterceptor.java` | Hibernate `CurrentTenantIdentifierResolver<String>` — resolves tenant from `TenantContext` for `@TenantId` discriminator support. Defaults to dev branch UUID when context is empty. |
| `TenantAwareDataSource.java` | `DelegatingDataSource` wrapping HikariCP — intercepts `getConnection()` to execute `SET LOCAL app.branch_id = '{uuid}'`. Transaction-scoped: auto-cleared on commit/rollback, no stale tenant state in pool. |

### Configuration (`backend/src/main/java/com/siceb/config/`)

| File | Purpose |
|------|---------|
| `MultiTenantConfig.java` | `@Configuration` that creates the explicit `HikariDataSource` bean + wraps it with `TenantAwareDataSource` as `@Primary`. Ensures all DB access goes through tenant-aware proxy. |

### Updated Files

| File | Change |
|------|--------|
| `application.yml` | Added `DB_SSL_PARAMS` to datasource URL; HikariCP tuning (connection-timeout 20s, idle-timeout 5min, max-lifetime 15min, leak-detection 30s, pool-name `siceb-pool`); Hibernate `tenant_identifier_resolver` property. |
| `.env.example` | Added `DB_SSL_PARAMS=` with documentation comment for cloud TLS usage. |

### Tests (`backend/src/test/java/com/siceb/platform/branch/`)

| File | Tests | Key Verifications |
|------|-------|-------------------|
| `TenantContextTest.java` | 6 | Empty by default, set/get, clear, require throws when empty, require returns when set, thread isolation |
| `TenantFilterTest.java` | 3 | Sets branch from header, no header leaves empty, clears context even on exception |

**Total: 53 tests, all passing** (43 shared + 9 branch + 1 app).

### How A1.5 relates to existing code

```
Request flow through multi-tenant stack:

HTTP Request
  │ X-Branch-Id: <uuid>
  ▼
TenantFilter (Servlet Filter, @Order 1)
  │ Sets TenantContext ThreadLocal
  ▼
SecurityConfig (permitAll for now)
  ▼
Controller → Service → Repository
  │                        │
  │                        ▼
  │              TenantConnectionInterceptor
  │              (Hibernate resolves @TenantId)
  │                        │
  │                        ▼
  │              TenantAwareDataSource.getConnection()
  │              → SET LOCAL app.branch_id = '<uuid>'
  │                        │
  │                        ▼
  │              PostgreSQL (RLS policies filter by branch_id)
  ▼
Response
  │
TenantFilter.finally → TenantContext.clear()
```

### HikariCP Pool Configuration

```
siceb-pool:
  maximum-pool-size: 10 (env: DB_POOL_SIZE)
  minimum-idle: 2
  connection-timeout: 20s
  idle-timeout: 5min
  max-lifetime: 15min
  leak-detection-threshold: 30s
```

### Flyway Migration History (verified on Docker PostgreSQL 17)

```
Version | Description               | Type
--------|---------------------------|----------
001     | init extensions           | Versioned
002     | create branches table     | Versioned
003     | create rls infrastructure | Versioned
—       | rls policies              | Repeatable
```

---

## Key Architectural Decisions (A1.5)

12. **Shared DB with `branch_id` discriminator** (CRN-29) — `branches` table as tenant anchor; Hibernate `@TenantId` + PostgreSQL RLS for defense-in-depth
13. **`SET LOCAL` per connection** via `TenantAwareDataSource` — transaction-scoped, no stale state in HikariCP pool
14. **TLS via `DB_SSL_PARAMS` env var** — empty for local Docker, `?sslmode=require` for cloud; zero code changes between environments
15. **`X-Branch-Id` header** for dev (Phase 1); JWT claims in Phase 3 — enables testing multi-tenancy before auth is built

---

## A1.6 — Automated Architecture Tests (Added)

### Test Classes (`backend/src/test/java/com/siceb/architecture/`)

| File | Tests | Purpose |
|------|-------|---------|
| `DomainStubsArchTest.java` | 4 | **IC-01 enforcement.** `noBusinessLogicInDomainModules` — iterates all classes in `com.siceb.domain..`, fails if any class besides `package-info` exists. `noJpaEntitiesInDomainModules` — no `@Entity` in domain stubs. `noSpringComponentsInDomainModules` — no `@Component`/`@Service`/`@Repository`/`@RestController`. `allTenDomainModulesExist` — verifies all 10 expected module packages are present. |
| `DependencyArchTest.java` | 9 | **CRN-27 enforcement.** `domainModulesFreeOfCycles` — ArchUnit slices cycle detection on `com.siceb.domain.(*)..`. `allTopLevelSlicesFreeOfCycles` — cycle detection on `com.siceb.(*)..`. `sharedKernelDoesNotDependOnDomain` / `...Platform` / `...Config` — shared is a leaf dependency. `domainDoesNotDependOnApiLayer` / `...Config` — domain isolation. `platformDoesNotDependOnDomain` / `...Api` — platform isolation. |
| `OfflineConventionsArchTest.java` | 3 | **Offline-first enforcement.** `jpaIdFieldsMustBeUuidOrEntityId` — any `@Entity` class's `@Id` field must be `UUID` or `EntityId`. `noAutoIncrementStrategies` — `@GeneratedValue` annotations rejected on entity fields. `idempotencyKeyTypeAvailableInSharedKernel` — `com.siceb.shared.IdempotencyKey` must exist. |

### How A1.6 relates to existing code

```
backend/src/test/java/com/siceb/
├── SicebApplicationTests.java           (A1.0 — smoke test)
├── shared/                              (A1.2 — 43 value type tests)
│   ├── MoneyTest.java
│   ├── UtcDateTimeTest.java
│   ├── EntityIdTest.java
│   ├── IdempotencyKeyTest.java
│   └── ErrorResponseTest.java
├── platform/branch/                     (A1.5 — 9 tenant tests)
│   ├── TenantContextTest.java
│   └── TenantFilterTest.java
└── architecture/                        (A1.6 — 16 architecture tests)
    ├── DomainStubsArchTest.java         (IC-01: stubs enforcement)
    ├── DependencyArchTest.java          (CRN-27: DAG enforcement)
    └── OfflineConventionsArchTest.java  (offline-first conventions)
```

### How ArchUnit tests guard future development

```
Phase 2+ developer adds code:
                │
                ▼
    ┌──────────────────────────┐
    │  mvn test (CI pipeline)  │
    └──────────┬───────────────┘
               │
    ┌──────────▼───────────────┐
    │  DomainStubsArchTest     │──→ FAIL if class added to wrong module (IC-01)
    │  DependencyArchTest      │──→ FAIL if circular dependency introduced (CRN-27)
    │  OfflineConventionsArchTest │──→ FAIL if Long @Id or @GeneratedValue used
    └──────────────────────────┘
```

**Total: 69 tests, all passing** (43 shared + 9 tenant + 16 architecture + 1 app).

---

## Key Architectural Decisions (A1.6)

16. **Architecture tests in `com.siceb.architecture` package** — separate from unit tests for clarity; each test class uses a static `ClassFileImporter` (one-time class scan per test class, reused across methods for performance)

---

## Test Results

- **Backend:** 69 tests, all passing (43 shared kernel + 9 tenant + 16 architecture + 1 app smoke)
- **Frontend:** TypeScript strict-mode compiles + Vite production build (196 KB JS + 12 KB CSS gzipped)

---

## Notes / Known Gaps

- **B-003**: vite-plugin-pwa peer dep warning for Vite 8 — build works, awaiting plugin update with Vite 8 support.
- **Docker Compose only**: no cloud deployment yet; infrastructure designed for zero-code migration via env vars (post Phase 6).
- **Security permissive**: `SecurityConfig` is `permitAll` — full auth enforcement deferred to Phase 3.
- **RLS empty**: `R__rls_policies.sql` has no tables yet; populated as domain tables are created in Phase 2+.

---

## Phase 1 — Final Summary

| Deliverable | Status | Key Metric |
|-------------|--------|------------|
| E1.1 — Repository + CI/CD | ✅ | GitHub Actions, 2 parallel jobs |
| E1.2 — Shared Kernel | ✅ | 5 value types, 4 converters, 43 tests |
| E1.3 — API Server scaffold | ✅ | Modular monolith: 10 domain stubs, 4 platform stubs, OpenAPI 3.1, STOMP WS |
| E1.4 — PWA Client scaffold | ✅ | Installable PWA: SW, IndexedDB (3 tables), Zustand (3 stores), Axios + STOMP |
| E1.5 — PostgreSQL database | ✅ | Multi-tenant: branches, RLS, Flyway V001–V003 + repeatable, HikariCP tuned |
| E1.6 — Architecture test suite | ✅ | 16 ArchUnit tests: stubs (4), dependencies (9), offline conventions (3) |
| E1.7 — Local Docker environment | ✅ | docker-compose.yml with 3 services, README with quickstart |
