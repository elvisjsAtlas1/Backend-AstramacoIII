import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 10,
  duration: '20s',
};

const BASE_URL = 'http://localhost:8080';

export default function () {
  const loginPayload = JSON.stringify({
    username: 'admin',
    password: 'admin123',
  });

  const loginRes = http.post(`${BASE_URL}/api/auth/login`, loginPayload, {
    headers: {
      'Content-Type': 'application/json',
    },
  });

  check(loginRes, {
    'login responde 200': (r) => r.status === 200,
  });

  const body = loginRes.json();
  const token = body.token || body.accessToken || body.jwt;

  check(token, {
    'token generado': (t) => t !== undefined && t !== null && t !== '',
  });

  const transportistasRes = http.get(`${BASE_URL}/api/transportistas`, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });

  check(transportistasRes, {
    'transportistas responde 200': (r) => r.status === 200,
  });

  sleep(1);
}