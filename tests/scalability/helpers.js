/**
 * Shared helpers for k6 scalability tests.
 * Auth, STOMP frame builders, and branch data management.
 */

import http from 'k6/http';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const USERNAME = __ENV.USERNAME || 'admin';
const PASSWORD = __ENV.PASSWORD || 'Admin123!';

// Cache token + branches after first login
let _cachedToken = null;
let _cachedBranches = [];

/**
 * Authenticate and return { token, branches }.
 * Caches result for the VU lifetime.
 */
export function login() {
  if (_cachedToken) {
    return { token: _cachedToken, branches: _cachedBranches };
  }

  const res = http.post(`${BASE_URL}/auth/login`, JSON.stringify({
    username: USERNAME,
    password: PASSWORD,
  }), {
    headers: { 'Content-Type': 'application/json' },
  });

  if (res.status !== 200) {
    throw new Error(`Login failed: ${res.status} ${res.body}`);
  }

  const body = JSON.parse(res.body);
  _cachedToken = body.accessToken;
  _cachedBranches = (body.user.branches || []).map(b => b.branchId || b.id);

  return { token: _cachedToken, branches: _cachedBranches };
}

/**
 * Switch branch context and return a new JWT for that branch.
 */
export function switchBranch(token, branchId) {
  const res = http.post(`${BASE_URL}/api/session/branch`, JSON.stringify({
    branchId: branchId,
  }), {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
  });

  if (res.status !== 200) {
    throw new Error(`Branch switch failed: ${res.status} ${res.body}`);
  }

  return JSON.parse(res.body).accessToken;
}

/**
 * Fetch list of test branches (ScalTest-Branch-*) from the branches API.
 */
export function getTestBranches(token) {
  const res = http.get(`${BASE_URL}/api/branches?includeInactive=false`, {
    headers: { 'Authorization': `Bearer ${token}` },
  });

  if (res.status !== 200) {
    throw new Error(`List branches failed: ${res.status}`);
  }

  const branches = JSON.parse(res.body);
  return branches
    .filter(b => b.name && b.name.startsWith('ScalTest-Branch-'))
    .map(b => b.branchId);
}

/**
 * Get auth headers for a given token.
 */
export function authHeaders(token) {
  return {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json',
  };
}

/**
 * Build a STOMP CONNECT frame with JWT auth.
 */
export function stompConnect(token) {
  return 'CONNECT\n'
    + 'accept-version:1.2\n'
    + 'heart-beat:0,0\n'
    + `Authorization:Bearer ${token}\n`
    + '\n\0';
}

/**
 * Build a STOMP SUBSCRIBE frame.
 */
export function stompSubscribe(destination, subscriptionId) {
  return 'SUBSCRIBE\n'
    + `id:${subscriptionId || 'sub-0'}\n`
    + `destination:${destination}\n`
    + '\n\0';
}

/**
 * Build a STOMP DISCONNECT frame.
 */
export function stompDisconnect() {
  return 'DISCONNECT\n\n\0';
}

export { BASE_URL };
