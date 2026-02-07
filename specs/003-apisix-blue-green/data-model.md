# Data Model: Apache APISIX Blue-Green Deployment

**Feature**: 003-apisix-blue-green
**Date**: 2026-02-07

> This feature does not introduce new application-level data models.
> The entities below describe APISIX gateway configuration objects managed via Admin API.

---

## APISIX Upstream (Blue)

| Field | Value | Description |
|-------|-------|-------------|
| id | `1` | Admin API resource ID |
| name | `order-blue-v1` | Human-readable name |
| type | `roundrobin` | Load balancing algorithm |
| scheme | `http` | Backend protocol |
| nodes | `order-service-blue.ecommerce.svc.cluster.local:8081` | K8s Service DNS |
| checks.active.http_path | `/h2-console` | Health check endpoint |
| timeout.connect | `5` | Connection timeout (seconds) |
| timeout.send | `10` | Send timeout (seconds) |
| timeout.read | `10` | Read timeout (seconds) |

## APISIX Upstream (Green)

| Field | Value | Description |
|-------|-------|-------------|
| id | `2` | Admin API resource ID |
| name | `order-green-v2` | Human-readable name |
| type | `roundrobin` | Load balancing algorithm |
| scheme | `http` | Backend protocol |
| nodes | `order-service-green.ecommerce.svc.cluster.local:8081` | K8s Service DNS |
| checks.active.http_path | `/h2-console` | Health check endpoint |
| timeout.connect | `5` | Connection timeout (seconds) |
| timeout.send | `10` | Send timeout (seconds) |
| timeout.read | `10` | Read timeout (seconds) |

## APISIX Route (Order API)

| Field | Value | Description |
|-------|-------|-------------|
| id | `1` | Admin API resource ID |
| name | `order-api-route` | Human-readable name |
| uri | `/api/*` | URI pattern matching all order API paths |
| methods | `GET, POST, PUT, DELETE` | Allowed HTTP methods |
| upstream_id | `1` | Default upstream (Blue) |
| plugins.traffic-split | See below | Weighted + header-based routing |

### Traffic-Split Plugin States

| State | Rule 1 (X-Canary Match) | Rule 2 (Default Weighted) | Description |
|-------|--------------------------|---------------------------|-------------|
| All Blue (100/0) | Green 100% | Green 0 / Blue 100 | Initial state / rollback |
| Canary (90/10) | Green 100% | Green 10 / Blue 90 | Small traffic validation |
| Equal (50/50) | Green 100% | Green 50 / Blue 50 | A/B comparison |
| All Green (0/100) | Green 100% | Green 100 / Blue 0 | Full cutover |

> Rule 1 always routes `X-Canary: true` requests to Green regardless of weighted state.
> Rule 2 applies weighted split to all other requests.

## APISIX Global Rule (OpenTelemetry)

| Field | Value | Description |
|-------|-------|-------------|
| id | `1` | Global rule resource ID |
| plugins.opentelemetry.sampler.name | `always_on` | 100% trace sampling |
| plugins.opentelemetry (Helm) | collector address: `jaeger:4318` | OTLP HTTP endpoint |

## Kubernetes Deployments

| Deployment | Image | Replicas | Namespace | Service Name | Port |
|------------|-------|----------|-----------|-------------|------|
| order-service-blue | order-service:latest | 1 | ecommerce | order-service-blue | 8081 |
| order-service-green | order-service:latest | 1 | ecommerce | order-service-green | 8081 |
| product-service | product-service:latest | 1 | ecommerce | product-service | 8082 |
| inventory-service | inventory-service:latest | 1 | ecommerce | inventory-service | 8083 |
| payment-service | payment-service:latest | 1 | ecommerce | payment-service | 8084 |
| notification-service | notification-service:latest | 1 | ecommerce | notification-service | 8085 |
| kafka | apache/kafka:3.7.0 | 1 | ecommerce | kafka | 9092 |
| jaeger | jaegertracing/all-in-one:1.53 | 1 | ecommerce | jaeger | 16686, 4317, 4318 |

## Environment Variable Differences (Blue vs Green)

| Variable | Blue | Green |
|----------|------|-------|
| `OTEL_SERVICE_NAME` | `order-service-blue` | `order-service-green` |
| `APP_VERSION` | `v1-blue` | `v2-green` |
| All other env vars | Identical | Identical |
