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
    transportistaId: 1,                   // ID del transportista existente (Elvis)
    clienteNombre: `Cliente ${uniqueId}`,
    clienteTelefono: `999${uniqueId}`.slice(0, 9),
    direccionEnvio: `Calle Falsa ${uniqueId}`,
    tipoTransporte: 'CAMIONERO',          // Coincide con el transportista
    material: 'PANDERETA',               // Válido para camionero
    cantidad: 5,
    montoTotal: 500.00,
    adelanto: 100.00,
    piso: 3,
    horaEnvio: '2026-06-04T10:00:00'
  });

  let createRes = http.post(PEDIDOS_URL, createPayload, params);

  check(createRes, {
    'POST /api/pedidos - status 200': (r) => r.status === 200,
    'POST - respuesta tiene id': (r) => {
      try {
        return JSON.parse(r.body).id !== undefined;
      } catch (e) {
        return false;
      }
    },
  });

  let pedidoId = null;
  try {
    pedidoId = JSON.parse(createRes.body).id;
    console.log(`Pedido creado con ID: ${pedidoId}`);
  } catch (e) {
    console.error('Error al parsear respuesta:', e.message);
  }

  if (pedidoId) {
    let deleteRes = http.del(`${PEDIDOS_URL}/${pedidoId}`, null, params);
    check(deleteRes, {
      'DELETE /api/pedidos/{id} - status 200': (r) => r.status === 200,
    });
  }

  sleep(1);
}