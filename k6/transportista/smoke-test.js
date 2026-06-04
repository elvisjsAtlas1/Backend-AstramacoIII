import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = 'http://localhost:8080';
const LOGIN_URL = `${BASE_URL}/api/auth/login`;
const TRANSPORTISTAS_URL = `${BASE_URL}/api/transportistas`;

const LOGIN_PAYLOAD = JSON.stringify({
  username: 'elvis.apaza',
  password: '76371922'
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
    nombre: `Smoke${uniqueId}`,
    apellidos: `Test${uniqueId}`,
    dni: `87654321${uniqueId}`.slice(-8),
    edad: 25,
    tipoTransporte: 'CAMIONERO',
    placa: `SMK${uniqueId}`.slice(-7),
    vehiculoInfo: 'Moto de prueba',
    capacidad: 0,
    estado: 'ACTIVO'
  });

  let createRes = http.post(TRANSPORTISTAS_URL, createPayload, params);
  check(createRes, { 'POST status 200': (r) => r.status === 200 });

  let id = null;
  try { id = JSON.parse(createRes.body).id; } catch(e) {}

  if (id) {
    let deleteRes = http.del(`${TRANSPORTISTAS_URL}/${id}`, null, params);
    check(deleteRes, { 'DELETE status 200': (r) => r.status === 200 });
  }
  sleep(1);
}