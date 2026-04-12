# SICEB — Sistema Integral de Control y Expedientes de Bienestar

Healthcare management system for multi-branch medical clinic networks. Built as a modular monolith with offline-first PWA capabilities.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| **Backend** | Spring Boot 3.5 · Java 21 · Spring Security · Spring Data JPA |
| **Frontend** | React 19 · TypeScript · Vite · Tailwind CSS · Shadcn/ui |
| **Database** | PostgreSQL 17 · Flyway migrations · Row-Level Security |
| **PWA** | Service Worker · IndexedDB (Dexie.js) · Background Sync |
| **State** | Zustand (client) · React Hook Form + Zod (forms) |
| **Infra** | Docker Compose (dev) · GitHub Actions CI |

## Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (v24+)
- [Node.js](https://nodejs.org/) (v22+) with [pnpm](https://pnpm.io/) (v10+)
- [Java 21](https://adoptium.net/) (for local backend development without Docker)
- [Maven 3.9+](https://maven.apache.org/) (for local backend development without Docker)

## Quick Start

```bash
# 1. Clone the repository
git clone <repo-url> && cd SICEB

# 2. Create your local environment file
cp .env.example .env

# 3. Start all services (PostgreSQL + API Server + PWA Client)
docker compose up --build

# 4. Access the application
#    PWA Client:  http://localhost:5173
#    API Server:  http://localhost:8080
#    API Docs:    http://localhost:8080/docs
#    Health:      http://localhost:8080/actuator/health
```

## Development Commands

### Full Stack (Docker)

```bash
docker compose up --build     # Start all services
docker compose down           # Stop all services
docker compose down -v        # Stop and remove volumes (resets DB)
docker compose logs -f api    # Follow API server logs
docker compose logs -f pwa    # Follow PWA client logs
```

### Backend Only

```bash
cd apps/backend
mvn spring-boot:run           # Run with default profile
mvn verify                    # Run all tests (unit + integration + architecture)
mvn test                      # Run unit tests only
```

### Frontend Only

```bash
cd apps/frontend
pnpm install                  # Install dependencies
pnpm dev                      # Start dev server with hot-reload
pnpm build                    # Production build
pnpm lint                     # Run ESLint
```

### Database

```bash
# Connect to PostgreSQL via Docker
docker compose exec db psql -U siceb -d siceb

# Reset database (removes all data)
docker compose down -v && docker compose up --build
```

## Project Structure

```
SICEB/
├── apps/
│   ├── backend/             # Spring Boot API Server (Modular Monolith)
│   │   ├── src/main/java/com/siceb/
│   │   │   ├── config/      # Security, CORS, WebSocket configuration
│   │   │   ├── shared/      # Shared Kernel (Money, EntityId, UtcDateTime)
│   │   │   ├── platform/    # Platform modules (IAM, Branch, Audit, Sync)
│   │   │   └── domain/      # Domain module stubs (10 bounded contexts)
│   │   ├── src/main/resources/
│   │   │   ├── application.yml
│   │   │   └── db/migration/  # Flyway SQL migrations
│   │   ├── Dockerfile
│   │   └── pom.xml
│   └── frontend/            # React PWA Client
│       ├── src/
│       ├── Dockerfile
│       └── package.json
├── docs/
│   ├── ADD/                 # Attribute-Driven Design (architecture, iterations)
│   └── Requirements/        # Vision, QA scenarios, user stories (US/)
├── docker-compose.yml       # Local development stack
├── .env.example             # Environment template
└── .github/workflows/       # CI/CD pipeline
```

## Environment Variables

All configuration is externalized via environment variables. See `.env.example` for the complete list. Zero hardcoded values — the same container images run in Docker local and cloud PaaS.

## Architecture

- **Modular Monolith**: 10 domain modules + 4 platform modules + Shared Kernel
- **Multi-tenant**: Single deployment with `branch_id` row-level isolation
- **Offline-first**: PWA with IndexedDB sync queue and conflict resolution
- **Cloud-ready**: Docker images portable to any PaaS (Cloud Run, ECS, App Service)

See [`docs/ADD/`](docs/ADD/) for full architectural documentation and [`docs/Requirements/README.md`](docs/Requirements/README.md) for requirements and user stories.
