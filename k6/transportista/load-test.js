import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = 'http://localhost:8080';
const LOGIN_URL = `${BASE_URL}/api/auth/login`;
const TRANSPORTISTAS_URL = `${BASE_URL}/api/transportistas`;

// Usuario transportista (NO admin)
const LOGIN_PAYLOAD = JSON.stringify({
  username: 'elvis.apaza',
  password: '76371922'
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
  // Usando CAMIONERO (con capacidad 0 para evitar errores si el campo es obligatorio)
  const createPayload = JSON.stringify({
    nombre: `Test${uniqueId}`,
    apellidos: `Apellido${uniqueId}`,
    dni: `12345678${uniqueId}`.slice(-8),
    edad: 30,
    tipoTransporte: 'CAMIONERO',
    placa: `PLACA${uniqueId}`.slice(-7),
    vehiculoInfo: 'Vehículo de prueba',
    capacidad: 0,          // CAMIONERO puede tener 0, pero si no existe el campo, elimínalo
    estado: 'ACTIVO'
  });

  let createRes = http.post(TRANSPORTISTAS_URL, createPayload, params);

  check(createRes, {
    'POST /api/transportistas - status 200': (r) => r.status === 200,
    'POST - respuesta tiene id': (r) => {
      try {
        return JSON.parse(r.body).id !== undefined;
      } catch (e) {
        return false;
      }
    },
  });

  let transportistaId = null;
  try {
    transportistaId = JSON.parse(createRes.body).id;
    console.log(`Transportista creado con ID: ${transportistaId}`);
  } catch (e) {
    console.error('Error al parsear respuesta:', e.message);
  }

  if (transportistaId) {
    let deleteRes = http.del(`${TRANSPORTISTAS_URL}/${transportistaId}`, null, params);
    check(deleteRes, {
      'DELETE /api/transportistas/{id} - status 200': (r) => r.status === 200,
    });
  }

  sleep(1);
}