#!/usr/bin/env bash
# run-scalability.sh — Orchestrate SICEB scalability tests (S4.5)
#
# Usage:
#   ./tests/scalability/run-scalability.sh [--branches N] [--skip-seed] [--http-only] [--ws-only]
#
# Prerequisites:
#   - Docker Compose running (docker compose up -d)
#   - k6 installed (https://k6.io/docs/get-started/installation/)
#   - psql available for seeding

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
DB_URL="${DB_URL:-postgresql://siceb:siceb_dev_password@localhost:5432/siceb}"
RESULTS_DIR="$SCRIPT_DIR/results"
HTTP_VUS="${HTTP_VUS:-10}"
HTTP_DURATION="${HTTP_DURATION:-30s}"
WS_VUS="${WS_VUS:-150}"
WS_DURATION="${WS_DURATION:-60s}"

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
  echo "ERROR: k6 not found. Install from https://k6.io/docs/get-started/installation/"
  echo "  Arch/Manjaro: yay -S k6"
  echo "  macOS: brew install k6"
  exit 1
fi

if ! command -v psql &>/dev/null; then
  echo "ERROR: psql not found. Install postgresql-client."
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

# Step 1: Seed data
if [ "$SKIP_SEED" = false ]; then
  echo ""
  echo "--- Step 1: Seeding $BRANCH_COUNT branches ---"
  PATIENTS_PER_BRANCH=$(( 50010 / BRANCH_COUNT + 1 ))
  psql "$DB_URL" \
    -v branch_count="$BRANCH_COUNT" \
    -v patients_per_branch="$PATIENTS_PER_BRANCH" \
    -v items_per_branch=1000 \
    -v deltas_per_item=5 \
    -f "$SCRIPT_DIR/seed-branches.sql"
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
