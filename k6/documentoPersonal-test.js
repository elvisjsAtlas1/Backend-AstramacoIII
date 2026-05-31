import http from 'k6/http';
import { sleep, check } from 'k6';

export const options = {
  stages: [
    { duration: '10s', target: 100 },
    { duration: '10s', target: 300 },
    { duration: '10s', target: 600 },
    { duration: '10s', target: 1000 },
    { duration: '10s', target: 0 },
  ],
};

const BASE_URL = 'http://localhost:8080';

export default function stressTest() {
  // 1. Login para obtener token
  const loginRes = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify({
    username: 'admin',
    password: 'admin123'
  }), {
    headers: { 'Content-Type': 'application/json' },
  });

  const token = loginRes.json('token');

  if (token) {
    // 2. Probar endpoint GET de documentos (solo lectura)
    const transportistaId = Math.floor(Math.random() * 10) + 1;
    const res = http.get(`${BASE_URL}/api/documentos/transportista/${transportistaId}`, {
      headers: {
        'Authorization': `Bearer ${token}`,
      },
    });

    check(res, {
      'status 200': (r) => r.status === 200 || r.status === 404, // 404 es aceptable
    });
  }

  sleep(1);
}