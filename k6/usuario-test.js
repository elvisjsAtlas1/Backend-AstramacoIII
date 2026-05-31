import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 10,
  duration: '20s',
};

const BASE_URL = 'http://localhost:8080';

export default function usuarioTest () {
  const uniqueId = `${__VU}_${__ITER}_${Date.now()}`;

  // 1. Login
  const loginPayload = JSON.stringify({
    username: 'admin',
    password: 'admin123',
  });

  const loginRes = http.post(`${BASE_URL}/api/auth/login`, loginPayload, {
    headers: { 'Content-Type': 'application/json' },
  });

  const loginOk = check(loginRes, {
    'Login status 200': (r) => r.status === 200,
  });

  if (!loginOk) {
    console.error(`Login falló: ${loginRes.status}`);
    return;
  }

  const token = loginRes.json('token');

  if (!token) {
    console.error('No se recibió token');
    return;
  }

  // 2. Crear usuario
  const usuarioData = {
    username: `test_${uniqueId}`,
    password: 'Test123!',
    rol: 'USER'
  };

  const createRes = http.post(`${BASE_URL}/api/usuarios`, JSON.stringify(usuarioData), {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
  });

  // ✅ Check corregido - acepta 200 o 201
  check(createRes, {
    'Usuario creado exitosamente (200 o 201)': (r) => r.status === 200 || r.status === 201,
    'Tiempo de respuesta < 500ms': (r) => r.timings.duration < 500,
  });

  sleep(1);
}