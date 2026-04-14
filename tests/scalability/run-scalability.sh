#!/usr/bin/env bash
# run-scalability.sh — Orchestrate SICEB scalability tests (S4.5)
#
# Usage:
#   ./tests/scalability/run-scalability.sh [--branches N] [--skip-seed] [--http-only] [--ws-only]
#
# Prerequisites:
#   - Docker Compose running (docker compose up -d)
#   - k6 installed (https://k6.io/docs/get-started/installation/)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Defaults
BRANCH_COUNT=15
SKIP_SEED=false
HTTP_ONLY=false
WS_ONLY=false
BASE_URL="${BASE_URL:-http://localhost:8080}"
WS_URL="${WS_URL:-ws://localhost:8080}"
RESULTS_DIR="$SCRIPT_DIR/results"
HTTP_VUS="${HTTP_VUS:-10}"
HTTP_DURATION="${HTTP_DURATION:-30s}"
WS_VUS="${WS_VUS:-150}"
WS_DURATION="${WS_DURATION:-60s}"
COMPOSE_CMD="${COMPOSE_CMD:-docker compose}"

# Parse arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --branches) BRANCH_COUNT="$2"; shift 2 ;;
    --skip-seed) SKIP_SEED=true; shift ;;
    --http-only) HTTP_ONLY=true; shift ;;
    --ws-only) WS_ONLY=true; shift ;;
    --base-url) BASE_URL="$2"; shift 2 ;;
    --help) echo "Usage: $0 [--branches N] [--skip-seed] [--http-only] [--ws-only]"; exit 0 ;;
    *) echo "Unknown option: $1"; exit 1 ;;
  esac
done

# Preflight checks
echo "=== SICEB Scalability Test Suite (S4.5) ==="
echo "Branches: $BRANCH_COUNT | Base URL: $BASE_URL"
echo ""

if ! command -v k6 &>/dev/null; then
  echo "ERROR: k6 not found. Install: yay -S k6-bin (Arch) or brew install k6 (macOS)"
  exit 1
fi

# Check Docker Compose is running
if ! $COMPOSE_CMD -f "$PROJECT_ROOT/docker-compose.yml" ps --status running 2>/dev/null | grep -q "db"; then
  echo "ERROR: Docker Compose services not running. Run: docker compose up -d"
  exit 1
fi

# Check API is reachable
if ! curl -sf "${BASE_URL}/auth/login" -X POST -H 'Content-Type: application/json' \
     -d '{"username":"admin","password":"Admin123!"}' -o /dev/null 2>/dev/null; then
  echo "WARNING: API at ${BASE_URL} may not be reachable. Attempting anyway..."
fi

# Create results directory
mkdir -p "$RESULTS_DIR"
TIMESTAMP=$(date +%Y%m%d-%H%M%S)

# Step 1: Seed data via docker compose exec
if [ "$SKIP_SEED" = false ]; then
  echo ""
  echo "--- Step 1: Seeding $BRANCH_COUNT branches ---"
  PATIENTS_PER_BRANCH=$(( 50010 / BRANCH_COUNT + 1 ))

  # Generate SQL from template by replacing placeholders
  sed \
    -e "s/__BRANCH_COUNT__/$BRANCH_COUNT/g" \
    -e "s/__PATIENTS_PER_BRANCH__/$PATIENTS_PER_BRANCH/g" \
    -e "s/__ITEMS_PER_BRANCH__/1000/g" \
    -e "s/__DELTAS_PER_ITEM__/5/g" \
    "$SCRIPT_DIR/seed-branches.sql" \
    | $COMPOSE_CMD -f "$PROJECT_ROOT/docker-compose.yml" exec -T db psql -U siceb -d siceb
  echo "Seeding complete."
else
  echo "--- Skipping seed (--skip-seed) ---"
fi

# Step 2: HTTP load tests
if [ "$WS_ONLY" = false ]; then
  echo ""
  echo "--- Step 2: HTTP load tests ($HTTP_VUS VUs, $HTTP_DURATION) ---"
  k6 run \
    --env BASE_URL="$BASE_URL" \
    --env VUS="$HTTP_VUS" \
    --env DURATION="$HTTP_DURATION" \
    --out json="$RESULTS_DIR/http-${BRANCH_COUNT}br-${TIMESTAMP}.json" \
    --summary-export="$RESULTS_DIR/http-${BRANCH_COUNT}br-${TIMESTAMP}-summary.json" \
    "$SCRIPT_DIR/http-load.js"
  echo "HTTP results: $RESULTS_DIR/http-${BRANCH_COUNT}br-${TIMESTAMP}-summary.json"
fi

# Step 3: WebSocket load tests
if [ "$HTTP_ONLY" = false ]; then
  echo ""
  echo "--- Step 3: WebSocket load tests ($WS_VUS VUs, $WS_DURATION) ---"

  # Check file descriptor limit
  CURRENT_ULIMIT=$(ulimit -n)
  REQUIRED_ULIMIT=$(( WS_VUS * 2 + 100 ))
  if [ "$CURRENT_ULIMIT" -lt "$REQUIRED_ULIMIT" ]; then
    echo "WARNING: File descriptor limit ($CURRENT_ULIMIT) may be too low for $WS_VUS connections."
    echo "  Run: ulimit -n $REQUIRED_ULIMIT"
  fi

  k6 run \
    --env BASE_URL="$BASE_URL" \
    --env WS_URL="$WS_URL" \
    --env WS_VUS="$WS_VUS" \
    --env WS_DURATION="$WS_DURATION" \
    --out json="$RESULTS_DIR/ws-${BRANCH_COUNT}br-${TIMESTAMP}.json" \
    --summary-export="$RESULTS_DIR/ws-${BRANCH_COUNT}br-${TIMESTAMP}-summary.json" \
    "$SCRIPT_DIR/ws-load.js"
  echo "WebSocket results: $RESULTS_DIR/ws-${BRANCH_COUNT}br-${TIMESTAMP}-summary.json"
fi

echo ""
echo "=== Scalability tests complete ==="
echo "Results in: $RESULTS_DIR/"
echo "Next: compile results into scalability-report.md"
