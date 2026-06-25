import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = 'http://localhost:8080';
const LOGIN_URL = `${BASE_URL}/api/auth/login`;
const PEDIDOS_URL = `${BASE_URL}/api/pedidos`;

const LOGIN_PAYLOAD = JSON.stringify({
  username: 'admin',
  password: 'admin123'
});

export const options = {
  stages: [
    { duration: '10s', target: 100 },
    { duration: '10s', target: 300 },
    { duration: '10s', target: 600 },
    { duration: '10s', target: 1000 },
    { duration: '10s', target: 0 },
  ],
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

export default function stressTest (data) {
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
    transportistaId: 1,
    clienteNombre: `Stress ${uniqueId}`,
    clienteTelefono: `777${uniqueId}`.slice(0, 9),
    direccionEnvio: `Calle Principal ${uniqueId}`,
    tipoTransporte: 'CAMIONERO',
    material: 'PANDERETA',
    cantidad: 2,
    montoTotal: 200,
    adelanto: 40,
    piso: 2,
    horaEnvio: '2026-06-04T12:00:00'
  });

  let createRes = http.post(PEDIDOS_URL, createPayload, params);
  check(createRes, { 'POST status 200': (r) => r.status === 200 });

  let id = null;
  try { id = JSON.parse(createRes.body).id; } catch(e) {}

  if (id) {
    let deleteRes = http.del(`${PEDIDOS_URL}/${id}`, null, params);
    check(deleteRes, { 'DELETE status 200': (r) => r.status === 200 });
  }
  sleep(1);
}