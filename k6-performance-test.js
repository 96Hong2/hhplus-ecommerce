/**
 * k6 성능 테스트 스크립트
 *
 * 설치: brew install k6 (Mac) or https://k6.io/docs/get-started/installation/
 * 실행: k6 run k6-performance-test.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// 커스텀 메트릭
const errorRate = new Rate('errors');

// 테스트 설정
export const options = {
    stages: [
        { duration: '30s', target: 10 },   // Ramp-up: 10명까지 증가
        { duration: '1m', target: 50 },    // 50명 유지
        { duration: '30s', target: 100 },  // Peak: 100명까지 증가
        { duration: '1m', target: 100 },   // 100명 유지
        { duration: '30s', target: 0 },    // Ramp-down: 0명으로 감소
    ],
    thresholds: {
        http_req_duration: ['p(95)<500'], // 95%의 요청이 500ms 이내
        http_req_failed: ['rate<0.1'],    // 에러율 10% 미만
        errors: ['rate<0.1'],              // 비즈니스 에러율 10% 미만
    },
};

const BASE_URL = 'http://localhost:8080';

// 테스트 데이터
const productOptionId = 1;  // 실제 존재하는 상품 옵션 ID로 변경 필요
const couponId = 1;          // 실제 존재하는 쿠폰 ID로 변경 필요

/**
 * 시나리오 1-1: 선착순 쿠폰 발급 (DB 비관적 락)
 */
export function couponIssuanceTestWithLock() {
    const userId = Math.floor(Math.random() * 1000) + 1;

    const payload = JSON.stringify({
        userId: userId
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
    };

    const res = http.patch(`${BASE_URL}/api/coupon/${couponId}/issue`, payload, params);

    const success = check(res, {
        'status is 200 or 400': (r) => r.status === 200 || r.status === 400,
        'response time < 500ms': (r) => r.timings.duration < 500,
    });

    errorRate.add(!success);

    sleep(0.1); // 100ms 대기
}

/**
 * 시나리오 1-2: 선착순 쿠폰 발급 (Redis SET)
 */
export function couponIssuanceTestWithRedis() {
    const userId = Math.floor(Math.random() * 1000) + 1;

    const payload = JSON.stringify({
        userId: userId
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
    };

    const res = http.patch(`${BASE_URL}/api/coupon/${couponId}/issue-redis`, payload, params);

    const success = check(res, {
        'status is 200 or 400': (r) => r.status === 200 || r.status === 400,
        'response time < 500ms': (r) => r.timings.duration < 500,
    });

    errorRate.add(!success);

    sleep(0.1); // 100ms 대기
}

/**
 * 시나리오 2: 주문 생성
 */
export function orderCreationTest() {
    const userId = Math.floor(Math.random() * 100) + 1;

    const payload = JSON.stringify({
        items: [
            {
                productOptionId: productOptionId,
                quantity: Math.floor(Math.random() * 3) + 1
            }
        ]
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
    };

    const res = http.post(`${BASE_URL}/api/orders`, payload, params);

    const success = check(res, {
        'status is 200 or 400': (r) => r.status === 200 || r.status === 400,
        'response time < 1000ms': (r) => r.timings.duration < 1000,
    });

    errorRate.add(!success);

    sleep(0.2); // 200ms 대기
}

/**
 * 시나리오 3: 쿠폰 발급 성능 테스트
 *
 * 환경 변수로 테스트 모드 선택:
 * - USE_REDIS=true: Redis SET 방식 테스트
 * - USE_REDIS=false 또는 미설정: DB 비관적 락 방식 테스트
 */
export default function() {
    const useRedis = __ENV.USE_REDIS === 'true';

    if (useRedis) {
        // Redis SET 방식 테스트
        couponIssuanceTestWithRedis();
    } else {
        // DB 비관적 락 방식 테스트
        couponIssuanceTestWithLock();
    }
}

/**
 * 테스트 요약 출력
 */
export function handleSummary(data) {
    return {
        'summary.json': JSON.stringify(data, null, 2),
        stdout: textSummary(data, { indent: ' ', enableColors: true }),
    };
}

function textSummary(data, options = {}) {
    const indent = options.indent || '';
    const colors = options.enableColors;

    let summary = `
${indent}========== Performance Test Summary ==========
${indent}
${indent}Total Requests: ${data.metrics.http_reqs.values.count}
${indent}Failed Requests: ${data.metrics.http_req_failed.values.passes}
${indent}Success Rate: ${((1 - data.metrics.http_req_failed.values.rate) * 100).toFixed(2)}%
${indent}
${indent}Response Times:
${indent}  - avg: ${data.metrics.http_req_duration.values.avg.toFixed(2)}ms
${indent}  - min: ${data.metrics.http_req_duration.values.min.toFixed(2)}ms
${indent}  - max: ${data.metrics.http_req_duration.values.max.toFixed(2)}ms
${indent}  - p(50): ${data.metrics.http_req_duration.values['p(50)'].toFixed(2)}ms
${indent}  - p(95): ${data.metrics.http_req_duration.values['p(95)'].toFixed(2)}ms
${indent}  - p(99): ${data.metrics.http_req_duration.values['p(99)'].toFixed(2)}ms
${indent}
${indent}Throughput: ${data.metrics.http_reqs.values.rate.toFixed(2)} req/s
${indent}
${indent}=============================================
`;

    return summary;
}
