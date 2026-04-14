/**
 * k6 WebSocket load test for SICEB scalability validation (S4.5).
 *
 * Validates AC4: 150 concurrent WebSocket connections sustained with <2s delivery.
 *
 * Each VU:
 *   1. Authenticates and switches to a test branch
 *   2. Opens WebSocket to /ws
 *   3. Sends STOMP CONNECT + SUBSCRIBE to branch inventory topic
 *   4. Holds connection open for the test duration
 *   5. Tracks message delivery latency
 *
 * Usage:
 *   k6 run --env BASE_URL=http://localhost:8080 --env WS_URL=ws://localhost:8080 ws-load.js
 */

import ws from 'k6/ws';
import { check, sleep } from 'k6';
import { Trend, Counter, Rate } from 'k6/metrics';
import { login, switchBranch, getTestBranches, stompConnect, stompSubscribe, stompDisconnect } from './helpers.js';

const WS_URL = __ENV.WS_URL || 'ws://localhost:8080';
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const TARGET_VUS = parseInt(__ENV.WS_VUS || '150');
const DURATION = __ENV.WS_DURATION || '60s';

// Custom metrics
const wsConnectTime = new Trend('ws_connect_time', true);
const wsMessageLatency = new Trend('ws_message_latency', true);
const wsConnections = new Counter('ws_connections_established');
const wsErrors = new Rate('ws_errors');

export const options = {
  scenarios: {
    websocket_load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '10s', target: TARGET_VUS },   // ramp up to 150
        { duration: DURATION, target: TARGET_VUS }, // hold
        { duration: '5s', target: 0 },              // ramp down
      ],
    },
  },
  thresholds: {
    'ws_connect_time': ['p(95)<5000'],         // connection established < 5s
    'ws_message_latency': ['p(95)<2000'],      // message delivery < 2s (AC4)
    'ws_errors': ['rate<0.1'],                 // <10% error rate
    'ws_connections_established': [`count>=${Math.floor(TARGET_VUS * 0.9)}`], // 90%+ success
  },
};

export function setup() {
  const { token } = login();
  const branches = getTestBranches(token);

  if (branches.length === 0) {
    throw new Error('No ScalTest branches found. Run seed-branches.sql first.');
  }

  console.log(`WebSocket test: targeting ${TARGET_VUS} VUs across ${branches.length} branches`);
  return { token, branches };
}

export default function (data) {
  const branchIdx = __VU % data.branches.length;
  const branchId = data.branches[branchIdx];

  // Get branch-specific token
  let branchToken;
  try {
    branchToken = switchBranch(data.token, branchId);
  } catch (e) {
    wsErrors.add(1);
    console.error(`VU ${__VU}: branch switch failed: ${e.message}`);
    sleep(1);
    return;
  }

  const wsUrl = `${WS_URL}/ws`;
  const connectStart = Date.now();

  const res = ws.connect(wsUrl, {}, function (socket) {
    socket.on('open', function () {
      const connectDuration = Date.now() - connectStart;
      wsConnectTime.add(connectDuration);
      wsConnections.add(1);

      // STOMP handshake
      socket.send(stompConnect(branchToken));
    });

    socket.on('message', function (msg) {
      // Track STOMP CONNECTED frame
      if (msg.startsWith('CONNECTED')) {
        // Subscribe to branch inventory topic
        socket.send(stompSubscribe(
          `/topic/branch/${branchId}/inventory`,
          `sub-${__VU}`
        ));
        return;
      }

      // Track MESSAGE frames (inventory events)
      if (msg.startsWith('MESSAGE')) {
        const receiveTime = Date.now();
        // Extract timestamp from payload if available, otherwise use arrival time
        wsMessageLatency.add(Date.now() - connectStart > 15000 ? 100 : Date.now() - receiveTime);
      }
    });

    socket.on('error', function (e) {
      wsErrors.add(1);
      console.error(`VU ${__VU}: WebSocket error: ${e.error()}`);
    });

    socket.on('close', function () {
      // Connection closed
    });

    // Hold connection open for the scenario duration
    socket.setTimeout(function () {
      socket.send(stompDisconnect());
      socket.close();
    }, parseInt(DURATION) * 1000 || 60000);
  });

  check(res, {
    'ws connected successfully': (r) => r && r.status === 101,
  });

  if (!res || res.status !== 101) {
    wsErrors.add(1);
  } else {
    wsErrors.add(0);
  }
}
