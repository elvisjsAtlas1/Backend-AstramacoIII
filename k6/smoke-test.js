import http from 'k6/http';
import { sleep, check } from 'k6';

export const options = {
  vus: 20,          // 20 usuarios simultáneos
  duration: '20s',  // durante 20 segundos
};

export default function () {
  let res = http.get('http://localhost:8080/v3/api-docs');

  check(res, {
    'status 200': (r) => r.status === 200,
  });

  sleep(1);
}