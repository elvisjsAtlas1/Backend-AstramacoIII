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
  vus: 20,
  duration: '20s',
};

export function setup() {
  const loginRes = http.post(LOGIN_URL, LOGIN_PAYLOAD, {
    headers: { 'Content-Type': 'application/json' }
  });

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
    username: `smoketest_${uniqueId}`,
    password: 'Prueba123!',
    rol: 'USER'  // ✅ CAMBIADO
  });

  let createRes = http.post(USERS_URL, createPayload, params);

  check(createRes, {
    'POST status 200': (r) => r.status === 200,
  });

  let userId = null;
  try {
    userId = JSON.parse(createRes.body).id;
  } catch (e) {}

  if (userId) {
    let deleteRes = http.del(`${USERS_URL}/${userId}`, null, params);
    check(deleteRes, {
      'DELETE status 204': (r) => r.status === 204,
    });
  }

  sleep(1);
}