# Quickstart: APISIX Blue-Green Deployment

**Feature**: 003-apisix-blue-green
**Date**: 2026-02-07

---

## Prerequisites

| Tool | Version | Check Command |
|------|---------|---------------|
| Docker | 20.10+ | `docker version` |
| Kind | v0.20+ | `kind version` |
| kubectl | v1.28+ | `kubectl version --client` |
| Helm | 3.14+ | `helm version` |

---

## 1. Deploy Environment

```bash
# One-command deployment: Kind cluster + APISIX + Blue/Green order-service + all supporting services
./scripts/apisix-deploy.sh
```

This creates:
- Kind cluster `apisix-ecommerce` with port mappings (9080, 9180, 16686)
- APISIX Gateway + etcd in `apisix` namespace
- 5 microservices (order-blue, order-green, product, inventory, payment, notification) + Kafka + Jaeger in `ecommerce` namespace
- APISIX routes and upstreams configured via Admin API
- OpenTelemetry global rule for trace propagation

**Wait for all pods to be ready** (~5-6 minutes).

---

## 2. Verify Deployment

```bash
# Check all pods
kubectl get pods -n ecommerce
kubectl get pods -n apisix

# Verify APISIX gateway
curl http://localhost:9080/api/orders -X POST \
  -H "Content-Type: application/json" \
  -d '{"customerId":"C001","items":[{"productId":"P001","quantity":1}]}'

# Verify Jaeger UI
open http://localhost:16686
```

---

## 3. Blue-Green Traffic Operations

```bash
# All traffic to Blue (initial state / rollback)
./scripts/apisix-traffic.sh blue

# Canary: 90% Blue / 10% Green
./scripts/apisix-traffic.sh canary

# Equal split: 50/50
./scripts/apisix-traffic.sh split

# Full cutover to Green
./scripts/apisix-traffic.sh green

# Rollback to Blue
./scripts/apisix-traffic.sh rollback

# Check current traffic configuration
./scripts/apisix-traffic.sh status
```

---

## 4. Header-Based Routing (QA Testing)

```bash
# Route directly to Green version via header
curl http://localhost:9080/api/orders -X POST \
  -H "Content-Type: application/json" \
  -H "X-Canary: true" \
  -d '{"customerId":"C001","items":[{"productId":"P001","quantity":1}]}'
```

---

## 5. Test Scenarios (via APISIX Gateway)

```bash
# Scenario 1: Happy Path
curl -X POST http://localhost:9080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"C001","items":[{"productId":"P001","quantity":2}]}'

# Scenario 2: Inventory Shortage
curl -X POST http://localhost:9080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"C001","items":[{"productId":"P999","quantity":999}]}'

# Scenario 3: Payment Timeout
curl -X POST http://localhost:9080/payment/admin/simulate-delay?ms=5000
curl -X POST http://localhost:9080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"C001","items":[{"productId":"P001","quantity":1}]}'

# Scenario 4: Kafka Async Notification (same as Scenario 1)

# Scenario 5: Kafka DLT
curl -X POST http://localhost:9080/notification/admin/simulate-failure?enabled=true
curl -X POST http://localhost:9080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"C001","items":[{"productId":"P001","quantity":1}]}'
# Wait 15s for retries...
curl -X POST http://localhost:9080/notification/admin/simulate-failure?enabled=false
```

---

## 6. Traffic Distribution Verification

```bash
# Send 100 requests and count Blue vs Green responses
./scripts/apisix-test.sh verify-distribution
```

---

## 7. Teardown

```bash
./scripts/apisix-teardown.sh
```

---

## Endpoint Reference

| Endpoint | URL | Description |
|----------|-----|-------------|
| APISIX Gateway | `http://localhost:9080` | All API requests go through here |
| APISIX Admin API | `http://localhost:9180` | Route/upstream configuration |
| Jaeger UI | `http://localhost:16686` | Trace visualization |
| Order API | `http://localhost:9080/api/orders` | Order creation (routed to Blue/Green) |
| Payment Admin | `http://localhost:9080/payment/admin/*` | Delay simulation |
| Notification Admin | `http://localhost:9080/notification/admin/*` | Failure simulation |
