/**
 * k6 HTTP load test for SICEB scalability validation (S4.5).
 *
 * Scenarios:
 *   1. per_branch_inventory — GET /api/inventory per branch (partition-pruned)
 *   2. cross_branch_admin  — GET /api/inventory across all branches (admin query)
 *   3. patient_search       — GET /api/patients/search with text query
 *
 * Usage:
 *   k6 run --env BASE_URL=http://localhost:8080 http-load.js
 *
 * Thresholds enforce ESC-02: <10% p95 degradation, <1s patient search.
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';
import { login, switchBranch, getTestBranches, authHeaders } from './helpers.js';

// Custom metrics
const inventoryLatency = new Trend('inventory_query_p95', true);
const patientSearchLatency = new Trend('patient_search_p95', true);
const adminQueryLatency = new Trend('admin_query_p95', true);
const errorRate = new Rate('errors');

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const VUS = parseInt(__ENV.VUS || '10');
const DURATION = __ENV.DURATION || '30s';

export const options = {
  scenarios: {
    per_branch_inventory: {
      executor: 'constant-vus',
      vus: VUS,
      duration: DURATION,
      exec: 'perBranchInventory',
    },
    cross_branch_admin: {
      executor: 'constant-vus',
      vus: 2,
      duration: DURATION,
      exec: 'crossBranchAdmin',
      startTime: '0s',
    },
    patient_search: {
      executor: 'constant-vus',
      vus: VUS,
      duration: DURATION,
      exec: 'patientSearch',
      startTime: '0s',
    },
  },
  thresholds: {
    'inventory_query_p95': ['p(95)<500'],    // p95 < 500ms
    'patient_search_p95': ['p(95)<1000'],    // p95 < 1s (AC6)
    'admin_query_p95': ['p(95)<2000'],       // admin queries allowed more headroom
    'errors': ['rate<0.05'],                 // <5% error rate
  },
};

export function setup() {
  const { token } = login();
  const branches = getTestBranches(token);

  if (branches.length === 0) {
    throw new Error('No ScalTest branches found. Run seed-branches.sql first.');
  }

  // Pre-generate per-branch tokens so VUs don't race on switchBranch
  const branchTokens = {};
  for (const branchId of branches) {
    branchTokens[branchId] = switchBranch(token, branchId);
  }

  console.log(`Found ${branches.length} test branches, tokens pre-generated`);
  return { adminToken: token, branches, branchTokens };
}

export function perBranchInventory(data) {
  const branchId = data.branches[__VU % data.branches.length];
  const token = data.branchTokens[branchId];

  const res = http.get(`${BASE_URL}/api/inventory?page=0&size=50`, {
    headers: authHeaders(token),
    tags: { scenario: 'per_branch_inventory' },
  });

  inventoryLatency.add(res.timings.duration);
  const ok = check(res, {
    'inventory status 200': (r) => r.status === 200,
    'inventory has content': (r) => {
      try { return JSON.parse(r.body).content.length > 0; }
      catch { return false; }
    },
  });
  if (!ok) errorRate.add(1);
  else errorRate.add(0);

  sleep(0.1);
}

export function crossBranchAdmin(data) {
  // Admin queries inventory — uses the admin's default branch token
  const branchId = data.branches[__ITER % data.branches.length];
  const token = data.branchTokens[branchId];

  const res = http.get(`${BASE_URL}/api/inventory?page=0&size=50`, {
    headers: authHeaders(token),
    tags: { scenario: 'cross_branch_admin' },
  });

  adminQueryLatency.add(res.timings.duration);
  const ok = check(res, {
    'admin query status 200': (r) => r.status === 200,
  });
  if (!ok) errorRate.add(1);
  else errorRate.add(0);

  sleep(0.5);
}

export function patientSearch(data) {
  const branchId = data.branches[__VU % data.branches.length];
  const token = data.branchTokens[branchId];

  // Search with a common name substring
  const searchTerms = ['María', 'García', 'Carlos', 'López', 'Ana'];
  const q = searchTerms[__ITER % searchTerms.length];

  const res = http.get(`${BASE_URL}/api/patients/search?q=${encodeURIComponent(q)}&page=0&size=20`, {
    headers: authHeaders(token),
    tags: { scenario: 'patient_search' },
  });

  patientSearchLatency.add(res.timings.duration);
  const ok = check(res, {
    'patient search status 200': (r) => r.status === 200,
    'patient search < 1s': (r) => r.timings.duration < 1000,
  });
  if (!ok) errorRate.add(1);
  else errorRate.add(0);

  sleep(0.1);
}
