import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';

// A custom trend metric to track the latency of successful requests ONLY.
const successfulRequestDuration = new Trend('successful_req_duration');

export const options = {
    // We'll start with a moderate load and ramp up.
    // This simulates 200 concurrent users for 30 seconds.
    vus: 200,
    duration: '30s',
};

export default function () {
    const res = http.get('http://localhost:8080/api/limited');

    // Check if the request was successful (status 200) or rate limited (status 429)
    const successful = check(res, {
        'status is 200': (r) => r.status === 200,
    });

    // If the request was successful, add its duration to our custom metric.
    if (successful) {
        successfulRequestDuration.add(res.timings.duration);
    }

    check(res, {
        'status is 429': (r) => r.status === 429,
    });

    // Small sleep to avoid completely overwhelming the local machine's network stack instantly.
    sleep(0.01);
}
