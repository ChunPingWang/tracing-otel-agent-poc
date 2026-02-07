# Performance Report: OTel Java Agent Overhead Analysis

## Test Environment

| Item | Detail |
|------|--------|
| Machine | _[e.g., 8 vCPU, 16 GB RAM, SSD]_ |
| OS | _[e.g., Ubuntu 22.04]_ |
| Docker | _[e.g., Docker 24.x, Compose v2.x]_ |
| JDK | _[e.g., Eclipse Temurin 8u392]_ |
| OTel Agent | _[e.g., opentelemetry-javaagent 1.32.1]_ |
| Jaeger | _[e.g., jaegertracing/all-in-one:1.53]_ |

## Benchmark Methodology

- **Tool**: `scripts/benchmark.sh` (curl-based HTTP benchmarking)
- **Endpoint**: `POST /api/orders` (order-service, port 8081)
- **Payload**: `{"customerId":"C001","items":[{"productId":"P001","quantity":1}]}`
- **Total requests**: 100 (first 10 discarded as warmup)
- **Measured requests**: 90
- **Runs**: With OTel Agent (`docker-compose.yml`) vs Without Agent (`docker-compose.no-agent.yml`)

## Response Time Results

| Metric | With Agent | Without Agent | Overhead |
|--------|-----------|---------------|----------|
| Average | _TBD_ ms | _TBD_ ms | _TBD_ % |
| P95 | _TBD_ ms | _TBD_ ms | _TBD_ % |
| P99 | _TBD_ ms | _TBD_ ms | _TBD_ % |
| Min | _TBD_ ms | _TBD_ ms | - |
| Max | _TBD_ ms | _TBD_ ms | - |

## Startup Time Comparison

| Service | With Agent | Without Agent | Overhead |
|---------|-----------|---------------|----------|
| order-service | _TBD_ s | _TBD_ s | _TBD_ s |
| product-service | _TBD_ s | _TBD_ s | _TBD_ s |
| inventory-service | _TBD_ s | _TBD_ s | _TBD_ s |
| payment-service | _TBD_ s | _TBD_ s | _TBD_ s |
| notification-service | _TBD_ s | _TBD_ s | _TBD_ s |

## Graceful Degradation

| Scenario | Result |
|----------|--------|
| With Jaeger running | _TBD_ (expected: HTTP 200) |
| After Jaeger stopped | _TBD_ (expected: HTTP 200) |

## Overhead Analysis

- **Response time overhead**: _TBD_ % (target: < 5%)
- **Startup time overhead**: _TBD_ seconds per service
- **Memory overhead**: _TBD_ (optional, via `docker stats`)
- **Zero-intrusion verified**: Yes / No (see `ZeroIntrusionVerificationTest`)

## Conclusion

_[Summary of findings: whether the OTel Java Agent approach meets the < 5% overhead target, whether graceful degradation works, and whether zero-intrusion is confirmed.]_

## Recommendation

_[Recommendation on whether to adopt the OTel Java Agent approach for production use.]_
