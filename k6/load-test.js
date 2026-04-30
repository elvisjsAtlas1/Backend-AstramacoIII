import http from 'k6/http';
import { check } from 'k6';

export const options = {
  vus: 1,
  duration: '5s',
};

export default function () {
  let res = http.get('http://localhost:8080/v3/api-docs');

  check(res, {
    'status es 200': (r) => r.status === 200,
  });
}