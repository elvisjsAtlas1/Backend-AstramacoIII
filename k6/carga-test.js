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
    // 2. Probar endpoints GET de cargas (solo lectura)
    const transportistaId = Math.floor(Math.random() * 10) + 1;

    // Endpoint 1: Obtener carga de un transportista específico
    const cargaRes = http.get(`${BASE_URL}/api/cargas/${transportistaId}`, {
      headers: {
        'Authorization': `Bearer ${token}`,
      },
    });

    check(cargaRes, {
      'GET carga status': (r) => r.status === 200 || r.status === 404,
    });

    // Endpoint 2: Listar todas las cargas (solo ADMIN)
    const listarRes = http.get(`${BASE_URL}/api/cargas`, {
      headers: {
        'Authorization': `Bearer ${token}`,
      },
    });

    check(listarRes, {
      'GET listar cargas': (r) => r.status === 200,
    });
  }

  sleep(1);
}