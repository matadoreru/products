import http from 'k6/http';
import {check, sleep} from 'k6';

const BASE_URL =
    __ENV.BASE_URL || 'http://product-api:8080';

const JSON_PARAMS = {
    headers: {'Content-Type': 'application/json'},
};

const DATES = [
    '2024-04-15',
    '2024-08-15',
    '2025-03-01',
];

const PRICES = [
    {
        value: 99.99,
        initDate: '2024-01-01',
        endDate: '2024-06-30',
    },
    {
        value: 129.99,
        initDate: '2024-07-01',
        endDate: '2024-12-31',
    },
    {
        value: 199.99,
        initDate: '2025-01-01',
        endDate: null,
    },
];

export const options = {
    scenarios: {
        warmup: {
            executor: 'constant-vus',
            exec: 'warmup',
            vus: 20,
            duration: '15s',
        },
        price_at_date: {
            executor: 'constant-vus',
            exec: 'priceAtDate',
            vus: 50,
            duration: '30s',
            startTime: '20s',
        },
        price_history: {
            executor: 'constant-vus',
            exec: 'priceHistory',
            vus: 50,
            duration: '30s',
            startTime: '55s',
        },
        create_product: {
            executor: 'constant-vus',
            exec: 'createProduct',
            vus: 20,
            duration: '30s',
            startTime: '90s',
        },
    },
    thresholds: {
        'http_req_failed': ['rate<0.01'],
        'http_req_duration{scenario:price_at_date}':
            ['p(95)<100'],
        'http_req_duration{scenario:price_history}':
            ['p(95)<100'],
        'http_req_duration{scenario:create_product}':
            ['p(95)<200'],
        'http_reqs{scenario:price_at_date}':
            ['count>0'],
        'http_reqs{scenario:price_history}':
            ['count>0'],
        'http_reqs{scenario:create_product}':
            ['count>0'],
    },
    summaryTrendStats: [
        'avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max',
    ],
};

function waitForApi() {
    const url = `${BASE_URL}/actuator/health`;
    for (let i = 0; i < 60; i++) {
        const res = http.get(url);
        if (res.status === 200 && res.body.includes('UP')) {
            return;
        }
        sleep(1);
    }
    throw new Error('API not ready after 60 seconds');
}

export function setup() {
    waitForApi();

    const body = JSON.stringify({
        name: 'Zapatillas deportivas',
        description: 'Producto para la prueba de carga',
    });
    const product = http.post(
        `${BASE_URL}/products`, body, JSON_PARAMS);
    const productId = product.json('id');

    for (const price of PRICES) {
        http.post(
            `${BASE_URL}/products/${productId}/prices`,
            JSON.stringify(price),
            JSON_PARAMS);
    }

    return {productId};
}

export function priceAtDate(data) {
    const index = Math.floor(Math.random() * DATES.length);
    const date = DATES[index];
    const url =
        `${BASE_URL}/products/${data.productId}/prices` +
        `?date=${date}`;
    const res = http.get(url);
    check(res, {
        'price: status 200': (r) => r.status === 200,
    });
}

export function priceHistory(data) {
    const url =
        `${BASE_URL}/products/${data.productId}/prices`;
    const res = http.get(url);
    check(res, {
        'history: status 200': (r) => r.status === 200,
    });
}

export function createProduct() {
    const body = JSON.stringify({
        name: `Producto ${__VU}-${__ITER}`,
        description: 'Creado por k6',
    });
    const res = http.post(
        `${BASE_URL}/products`, body, JSON_PARAMS);
    check(res, {
        'create: status 201': (r) => r.status === 201,
    });
}

export function warmup(data) {
    priceAtDate(data);
    priceHistory(data);
}
