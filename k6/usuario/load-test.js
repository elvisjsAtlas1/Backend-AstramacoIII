import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = 'http://localhost:8080';
const LOGIN_URL = `${BASE_URL}/api/auth/login`;
const USERS_URL = `${BASE_URL}/api/usuarios`;

const LOGIN_PAYLOAD = JSON.stringify({
  username: 'admin',
  password: 'admin123'
});

export const options = {
  vus: 1,
  duration: '5s',
};

export function setup() {
  const loginRes = http.post(LOGIN_URL, LOGIN_PAYLOAD, {
    headers: { 'Content-Type': 'application/json' }
  });

  check(loginRes, { 'Login exitoso': (r) => r.status === 200 });

  if (loginRes.status !== 200) {
    console.error('Error en login:', loginRes.body);
    return { token: null };
  }

  const token = JSON.parse(loginRes.body).token;
  return { token };
}

export default function (data) {
  const { token } = data;
  if (!token) return;

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    }
  };

  const uniqueId = `${__VU}_${__ITER}_${Date.now()}`;
  const createPayload = JSON.stringify({
    username: `loadtest_${uniqueId}`,
    password: 'Prueba123!',
    rol: 'USER'  // ✅ CAMBIADO: antes era 'CLIENTE'
  });

  let createRes = http.post(USERS_URL, createPayload, params);

  check(createRes, {
    'POST /api/usuarios - status 200': (r) => r.status === 200,
    'POST - respuesta tiene id': (r) => {
      try {
        return JSON.parse(r.body).id !== undefined;
      } catch (e) {
        return false;
      }
    },
  });

  let userId = null;
  try {
    userId = JSON.parse(createRes.body).id;
    console.log(`Usuario creado con ID: ${userId}`);
  } catch (e) {
    console.error('Error al parsear respuesta:', e.message);
  }

  if (userId) {
    let deleteRes = http.del(`${USERS_URL}/${userId}`, null, params);
    check(deleteRes, {
      'DELETE /api/usuarios/{id} - status 204': (r) => r.status === 204,
    });
  }

  sleep(1);
}