import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const errorRate = new Rate('errores');

const BASE_URL = 'http://localhost:8080';
const LOGIN_URL = `${BASE_URL}/api/auth/login`;
const TRANSPORTISTAS_URL = `${BASE_URL}/api/transportistas`;

const LOGIN_PAYLOAD = JSON.stringify({
  username: 'elvis.apaza',  // o 'admin' si prefieres
  password: '76371922'      // o 'admin123'
});

export const options = {
  stages: [
    { duration: '10s', target: 100 },
    { duration: '10s', target: 300 },
    { duration: '10s', target: 600 },
    { duration: '10s', target: 1000 },
    { duration: '10s', target: 0 },
  ],
  thresholds: {
    errores: ['rate<0.05'], // permitir hasta 5% de errores
  },
};

export function setup() {
  const loginRes = http.post(LOGIN_URL, LOGIN_PAYLOAD, {
    headers: { 'Content-Type': 'application/json' }
  });
  if (loginRes.status !== 200) {
    throw new Error(`Login falló: ${loginRes.body}`);
  }
  const token = JSON.parse(loginRes.body).token;
  return { token };
}

export default function (data) {
  const { token } = data;
  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    }
  };

  const uniqueId = `${__VU}_${__ITER}_${Date.now()}_${Math.random()}`; // más seguro
  const payload = {
    nombre: `Stress${uniqueId}`,
    apellidos: `Load${uniqueId}`,
    dni: `${Math.floor(Math.random() * 100000000)}`.slice(0, 8),
    edad: 30,
    tipoTransporte: 'CAMIONERO',
    placa: `P${Math.random().toString(36).substring(2, 8).toUpperCase()}`,
    vehiculoInfo: 'Prueba',
    capacidad: 0,
    estado: 'ACTIVO'
  };

  const createRes = http.post(TRANSPORTISTAS_URL, JSON.stringify(payload), params);
  const success = createRes.status === 200;
  errorRate.add(!success);

  if (!success) {
    console.error(`Fallo POST: status ${createRes.status}, body: ${createRes.body}`);
  }

  check(createRes, { 'POST status 200': (r) => r.status === 200 });

  if (success) {
    let id = JSON.parse(createRes.body).id;
    let deleteRes = http.del(`${TRANSPORTISTAS_URL}/${id}`, null, params);
    check(deleteRes, { 'DELETE status 200': (r) => r.status === 200 });
  }
  sleep(1);
}