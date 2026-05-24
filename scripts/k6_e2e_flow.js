import http from 'k6/http';
import { check, fail, sleep } from 'k6';
import { Counter } from 'k6/metrics';

const TEST_MODE = __ENV.TEST_MODE || 'iterations'; // iterations | duration
const TOTAL_VUS = Number(__ENV.VUS || 10);
const ITERATIONS_PER_VU = Number(__ENV.ITERATIONS || 1);
const WAVE_GAP_SEC = Number(__ENV.WAVE_GAP_SEC || 5);
const TEST_DURATION = __ENV.DURATION || '5m';
const wave1Vus = Number(__ENV.WAVE1_VUS || Math.max(1, Math.floor(TOTAL_VUS * 0.3)));
const wave2Vus = Number(__ENV.WAVE2_VUS || Math.max(1, Math.floor(TOTAL_VUS * 0.3)));
const wave3Vus = Math.max(0, TOTAL_VUS - wave1Vus - wave2Vus);

let scenarios;
if (TEST_MODE === 'duration') {
  scenarios = {
    sustained: {
      executor: 'constant-vus',
      exec: 'e2eFlow',
      vus: TOTAL_VUS,
      duration: TEST_DURATION,
      gracefulStop: '30s',
    },
  };
} else {
  scenarios = {
    wave1: {
      executor: 'per-vu-iterations',
      exec: 'e2eFlow',
      startTime: '0s',
      vus: wave1Vus,
      iterations: ITERATIONS_PER_VU,
      maxDuration: __ENV.MAX_DURATION || '10m',
    },
    wave2: {
      executor: 'per-vu-iterations',
      exec: 'e2eFlow',
      startTime: `${WAVE_GAP_SEC}s`,
      vus: wave2Vus,
      iterations: ITERATIONS_PER_VU,
      maxDuration: __ENV.MAX_DURATION || '10m',
    },
  };

  if (wave3Vus > 0) {
    scenarios.wave3 = {
      executor: 'per-vu-iterations',
      exec: 'e2eFlow',
      startTime: `${WAVE_GAP_SEC * 2}s`,
      vus: wave3Vus,
      iterations: ITERATIONS_PER_VU,
      maxDuration: __ENV.MAX_DURATION || '10m',
    };
  }
}

export const options = { scenarios };

const BASE_URL = __ENV.BASE_URL || 'http://k8s-istiosys-devgwist-75f9e87595-cb185c46783cf9f8.elb.ap-northeast-2.amazonaws.com';
const TOKEN = __ENV.TOKEN;
const TOKEN_LIST_RAW = __ENV.TOKEN_LIST || '';
const TOKENS = TOKEN_LIST_RAW
  .split(',')
  .map((t) => t.trim())
  .filter((t) => t.length > 0);

const HOST_PRODUCT = __ENV.HOST_PRODUCT || 'product-dev.example.com';
const HOST_CART = __ENV.HOST_CART || 'cart-dev.example.com';
const HOST_ORDER = __ENV.HOST_ORDER || 'order-dev.example.com';
const HOST_PAYMENT = __ENV.HOST_PAYMENT || 'payment-dev.example.com';
const HOST_USER = __ENV.HOST_USER || 'user-dev.example.com';
const CART_CLEAR_MODE = (__ENV.CART_CLEAR_MODE || 'event').trim().toLowerCase(); // event | manual
const EVENT_WAIT_TIMEOUT_SEC = Number(__ENV.EVENT_WAIT_TIMEOUT_SEC || 30);
const EVENT_WAIT_POLL_MS = Number(__ENV.EVENT_WAIT_POLL_MS || 500);
const PRODUCT_ID = (__ENV.PRODUCT_ID || __ENV.FIXED_PRODUCT_ID || 'a0000000-0000-0000-0000-000000001001').trim();
const paymentReadyRetryFailures = new Counter('payment_ready_retry_failures');
const txCompleted = new Counter('tx_completed');
const orderCreateRetryFailures = new Counter('order_create_retry_failures');
const orderConfirmedWaitTimeouts = new Counter('order_confirmed_wait_timeouts');
const cartEventClearWaitTimeouts = new Counter('cart_event_clear_wait_timeouts');

if (!TOKEN && TOKENS.length === 0) {
  fail('Either TOKEN or TOKEN_LIST env is required');
}

function req(method, host, path, body, expectedStatuses, token) {
  const headers = {
    Authorization: `Bearer ${token}`,
    Host: host,
  };

  if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }

  const payload = body !== undefined ? JSON.stringify(body) : null;
  const params = { headers };
  if (Array.isArray(expectedStatuses) && expectedStatuses.length > 0) {
    params.responseCallback = http.expectedStatuses(...expectedStatuses);
  }
  const res = http.request(method, `${BASE_URL}${path}`, payload, params);
  return res;
}

function waitForOrderConfirmed(orderId, token) {
  const deadline = Date.now() + (EVENT_WAIT_TIMEOUT_SEC * 1000);
  while (Date.now() < deadline) {
    const res = req('GET', HOST_ORDER, `/api/v2/orders/${orderId}`, undefined, [200], token);
    if (res.status === 200) {
      const status = res.json()?.status ?? '';
      if (status === 'CONFIRMED') {
        return true;
      }
    }
    sleep(EVENT_WAIT_POLL_MS / 1000);
  }
  return false;
}

function waitForCartEmpty(token) {
  const deadline = Date.now() + (EVENT_WAIT_TIMEOUT_SEC * 1000);
  while (Date.now() < deadline) {
    const res = req('GET', HOST_CART, '/api/v2/cart', undefined, undefined, token);
    if (res.status === 200) {
      const cart = res.json();
      if (Array.isArray(cart) && cart.length === 0) {
        return true;
      }
    }
    sleep(EVENT_WAIT_POLL_MS / 1000);
  }
  return false;
}

export function e2eFlow() {
  let res;
  const token = TOKENS.length > 0 ? TOKENS[(__VU - 1) % TOKENS.length] : TOKEN;

  // Ensure address exists for this user; create default once if missing.
  res = req('GET', HOST_USER, '/api/v2/users/me/addresses', undefined, [200], token);
  let addresses = [];
  try {
    addresses = res.json();
  } catch (e) {
    addresses = [];
  }
  if (!Array.isArray(addresses)) addresses = [];
  if (addresses.length === 0) {
    const suffix = (__VU * 10000) + __ITER;
    res = req('POST', HOST_USER, '/api/v2/users/me/addresses', {
      zipCode: `${10000 + (suffix % 89999)}`,
      address: `Load Test Addr ${__VU}`,
      detailAddress: `Iter ${__ITER}`,
      recipient: `User ${__VU}`,
      recipientPhone: `010-9${String(__VU).padStart(3, '0')}-${String(__ITER % 10000).padStart(4, '0')}`,
      isDefault: true,
    }, [200, 201], token);
  }

  const productId = PRODUCT_ID;
  if (!productId) fail('PRODUCT_ID env is required');

  res = req('GET', HOST_PRODUCT, `/api/v2/products/${productId}`, undefined, undefined, token);
  check(res, { 'STEP1 product detail code=200': (r) => r.status === 200 }) || fail(`STEP1 failed: ${res.status} ${res.body}`);
  const detail = res.json();
  const price = detail?.price ?? detail?.variants?.[0]?.price ?? 0;
  const title = detail?.title ?? '';
  const thumb = detail?.thumbnailUrl ?? '';
  const optionName = detail?.variants?.[0]?.optionName ?? '';
  const variantId = detail?.variants?.[0]?.variantId ?? null;
  if (!price) fail(`STEP1 failed: invalid price=${price}`);

  res = req('POST', HOST_CART, '/api/v2/cart', {
    productId,
    variantId,
    quantity: 1,
  }, undefined, token);
  check(res, { 'STEP3 cart add code=201': (r) => r.status === 201 }) || fail(`STEP3 failed: ${res.status} ${res.body}`);

  res = req('GET', HOST_CART, '/api/v2/cart', undefined, undefined, token);
  check(res, { 'STEP4 cart get code=200': (r) => r.status === 200 }) || fail(`STEP4 failed: ${res.status} ${res.body}`);
  const cart = res.json();
  if (!Array.isArray(cart) || cart.length < 1) fail(`STEP4 failed: invalid cart=${res.body}`);

  const orderBody = {
    addressId: null,
    totalAmount: price,
    items: [
      {
        productId,
        variantId,
        quantity: 1,
        productTitle: title,
        productThumbnail: thumb,
        optionName,
        unitPrice: price,
      },
    ],
  };

  let orderOk = false;
  for (let i = 0; i < 3; i += 1) {
    res = req('POST', HOST_ORDER, '/api/v2/orders', orderBody, [200, 404, 500, 502, 503, 504], token);
    if (res.status === 200) {
      orderOk = true;
      break;
    }
    orderCreateRetryFailures.add(1);
    sleep(0.5);
  }
  const step5Ok = check(res, { 'STEP5 order create code=200': (r) => r.status === 200 });
  if (!step5Ok) {
    const debug = JSON.stringify({
      step: 'STEP5',
      vu: __VU,
      iter: __ITER,
      productId,
      variantId,
      price,
      status: res.status,
      body: res.body,
    });
    fail(`STEP5 failed: ${debug}`);
  }
  if (!orderOk) {
    const debug = JSON.stringify({
      step: 'STEP5_RETRY_EXHAUSTED',
      vu: __VU,
      iter: __ITER,
      productId,
      variantId,
      price,
      status: res.status,
      body: res.body,
    });
    fail(`STEP5 failed after retries: ${debug}`);
  }
  const orderId = res.json()?.orderId;
  if (!orderId) fail(`STEP5 failed: orderId missing body=${res.body}`);

  let readyOk = false;
  for (let i = 0; i < 5; i += 1) {
    res = req('POST', HOST_PAYMENT, '/api/v2/payments/ready', {
      orderId,
      amount: price,
    }, [200, 502, 503, 504], token);
    if (res.status === 200) {
      readyOk = true;
      break;
    }
    paymentReadyRetryFailures.add(1);
    sleep(1);
  }
  if (!readyOk) fail(`STEP6 failed: payments/ready not 200, last=${res.status} ${res.body}`);

  if (CART_CLEAR_MODE === 'manual') {
    const deleteItems = cart.map((x) => ({
      productId: x.productId,
      variantId: x.variantId ?? null,
    }));
    res = req('DELETE', HOST_CART, '/api/v2/cart/items/bulk', deleteItems, undefined, token);
    check(res, { 'STEP7 cart clear code=204': (r) => r.status === 204 }) || fail(`STEP7 failed: ${res.status} ${res.body}`);

    res = req('GET', HOST_CART, '/api/v2/cart', undefined, undefined, token);
    check(res, { 'STEP8 cart empty code=200': (r) => r.status === 200 }) || fail(`STEP8 failed: ${res.status} ${res.body}`);
    const finalCart = res.json();
    if (!Array.isArray(finalCart) || finalCart.length !== 0) {
      fail(`STEP8 failed: cart not empty body=${res.body}`);
    }
  } else {
    const orderConfirmed = waitForOrderConfirmed(orderId, token);
    if (!orderConfirmed) {
      orderConfirmedWaitTimeouts.add(1);
    }
    check({ ok: orderConfirmed }, { 'STEP7 order confirmed by event': (x) => x.ok === true })
      || fail(`STEP7 failed: order not CONFIRMED within ${EVENT_WAIT_TIMEOUT_SEC}s (orderId=${orderId})`);

    const cartEmptyByEvent = waitForCartEmpty(token);
    if (!cartEmptyByEvent) {
      cartEventClearWaitTimeouts.add(1);
    }
    check({ ok: cartEmptyByEvent }, { 'STEP8 cart emptied by event': (x) => x.ok === true })
      || fail(`STEP8 failed: cart not empty within ${EVENT_WAIT_TIMEOUT_SEC}s after event chain`);
  }

  txCompleted.add(1);
}

export default function () {
  e2eFlow();
}

export function handleSummary(data) {
  const checks = data.metrics.checks || {};
  const iterations = data.metrics.iterations || {};
  const httpReqs = data.metrics.http_reqs || {};
  const httpFailed = data.metrics.http_req_failed || {};
  const tx = data.metrics.tx_completed || {};
  const checkPasses = checks.values?.passes ?? 0;
  const checkFails = checks.values?.fails ?? 0;
  const checkTotal = checkPasses + checkFails;
  const rps = httpReqs.values?.rate ?? 0;
  const tps = tx.values?.rate ?? 0;

  const txt = [
    'k6 e2e summary',
    `checks: ${checkPasses}/${checkTotal} passed`,
    `iterations: ${iterations.values?.count ?? 0}`,
    `http_reqs: ${httpReqs.values?.count ?? 0}`,
    `rps: ${rps}`,
    `tps: ${tps}`,
    `tx_completed: ${tx.values?.count ?? 0}`,
    `http_req_failed_rate: ${httpFailed.values?.rate ?? 0}`,
  ].join('\n');

  return {
    'k6_e2e_summary.json': JSON.stringify(data, null, 2),
    'k6_e2e_summary.txt': `${txt}\n`,
  };
}
