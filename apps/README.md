# Applications

Runnable code for the SICEB stack.

| Path | Description |
|------|-------------|
| [`backend/`](backend/) | Spring Boot API (Maven). Run from repo root: `cd apps/backend && mvn spring-boot:run`. |
| [`frontend/`](frontend/) | React PWA (pnpm / Vite). Run: `cd apps/frontend && pnpm dev`. |

Docker Compose at the repository root builds both services (`api`, `pwa`) from these directories.
