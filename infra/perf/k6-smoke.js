/**
 * k6 smoke / light load – FlyPro Analytics (Phase 6 · Step 20)
 * Usage: k6 run -e BASE_URL=http://localhost:8080 infra/perf/k6-smoke.js
 */
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 10,
  duration: '30s',
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<2000'],
  },
};

const BASE = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  const health = http.get(`${BASE}/actuator/health`);
  check(health, {
    'health 200': (r) => r.status === 200,
  });

  const info = http.get(`${BASE}/actuator/info`);
  check(info, {
    'info ok': (r) => r.status === 200 || r.status === 401 || r.status === 403,
  });

  sleep(0.5);
}
