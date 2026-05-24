import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL  = __ENV.BASE_URL  || 'http://k8s-istiosys-devgwist-75f9e87595-cb185c46783cf9f8.elb.ap-northeast-2.amazonaws.com';
const TOKEN     = __ENV.TOKEN;
const HOST      = __ENV.HOST_PRODUCT || 'product-dev.example.com';
const PRODUCT_ID = __ENV.PRODUCT_ID || 'a0000000-0000-0000-0000-000000001001';

export const options = {
  scenarios: {
    load: {
      executor: 'constant-vus',
      vus: Number(__ENV.VUS || 20),
      duration: __ENV.DURATION || '30s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
  },
};

export default function () {
  const res = http.get(`${BASE_URL}/api/v2/products/${PRODUCT_ID}`, {
    headers: {
      Authorization: `Bearer ${TOKEN}`,
      Host: HOST,
    },
  });

  check(res, { 'status 200': (r) => r.status === 200 });
  sleep(0.1);
}
