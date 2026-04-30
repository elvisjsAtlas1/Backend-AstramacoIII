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

export default function () {
  let res = http.get('http://localhost:8080/v3/api-docs');

  check(res, {
    'status 200': (r) => r.status === 200,
  });

  sleep(1);
}