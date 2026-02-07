# Quickstart: Grafana Monitoring Dashboard

**Feature**: 004-grafana-dashboard | **Date**: 2026-02-07

## Prerequisites

- Docker and Docker Compose (v2+) installed
- Project built: all 5 service Docker images exist
- Port 3000 (Grafana) and 9090 (Prometheus) available on host

## Docker Compose Quickstart

### 1. Start the environment

```bash
docker compose up --build -d
```

This starts all 7 existing containers plus 2 new ones:
- `prometheus` — Metrics backend (port 9090)
- `grafana` — Dashboard UI (port 3000)

### 2. Send test traffic

```bash
# Send 10 orders to generate metrics
for i in $(seq 1 10); do
  curl -s -X POST http://localhost:8081/api/orders \
    -H "Content-Type: application/json" \
    -d '{"customerId":"C001","items":[{"productId":"P001","quantity":1}]}'
  echo ""
done
```

### 3. Open Grafana

Open http://localhost:3000 in your browser.

No login required — anonymous access is enabled.

### 4. Verify dashboards

Three dashboards are pre-loaded:
1. **Service Health Overview** — Request rates, error rates, latency percentiles
2. **JVM Metrics** — Heap memory, GC activity, thread counts
3. **Kafka Metrics** — Producer/consumer rates, consumer lag

Dashboards auto-refresh every 10 seconds. Data should appear within 60 seconds of first request.

### 5. Test specific scenarios

**Latency anomaly (Payment Service delay):**
```bash
# Enable 5-second payment delay
curl -X POST "http://localhost:8084/payment/admin/simulate-delay?ms=5000"

# Send orders to trigger payments
for i in $(seq 1 5); do
  curl -s -X POST http://localhost:8081/api/orders \
    -H "Content-Type: application/json" \
    -d '{"customerId":"C001","items":[{"productId":"P001","quantity":1}]}'
done

# Check Service Health dashboard — payment-service p95/p99 should show >4s
```

**Error rate (Inventory shortage):**
```bash
# Send order with quantity exceeding stock (stock is 100)
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"C001","items":[{"productId":"P001","quantity":999}]}'

# Check Service Health dashboard — inventory-service error rate should increase
```

**Kafka metrics (Notification pipeline):**
```bash
# Send several orders (each triggers Kafka message to notification-service)
for i in $(seq 1 10); do
  curl -s -X POST http://localhost:8081/api/orders \
    -H "Content-Type: application/json" \
    -d '{"customerId":"C001","items":[{"productId":"P001","quantity":1}]}'
done

# Check Kafka Metrics dashboard — producer and consumer message counts should increase
```

### 6. Teardown

```bash
docker compose down -v
```

## Kubernetes (APISIX) Quickstart

### 1. Deploy the full environment

```bash
./scripts/apisix-deploy.sh
```

This deploys all existing services plus Prometheus and Grafana in the Kind cluster.

### 2. Access Grafana

Grafana is exposed via NodePort. Access at: http://localhost:30300

### 3. Verify

Same verification steps as Docker Compose — send requests via APISIX gateway at http://localhost:9080/api/orders.

### 4. Teardown

```bash
./scripts/apisix-teardown.sh
```

## Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| Grafana shows "No data" | Prometheus not receiving metrics | Check Prometheus targets at http://localhost:9090; verify `OTEL_METRICS_EXPORTER` is not `none` |
| Dashboard panels are empty | Metrics not yet received | Wait 60 seconds; send more requests to generate data |
| Prometheus UI shows no metrics | OTLP receiver disabled | Ensure Prometheus started with `--web.enable-otlp-receiver` |
| Grafana login page appears | Anonymous access not configured | Check `GF_AUTH_ANONYMOUS_ENABLED=true` env var |

## Service Endpoints Summary

| Service | URL | Description |
|---------|-----|-------------|
| Grafana | http://localhost:3000 | Monitoring dashboards |
| Prometheus | http://localhost:9090 | Metrics backend + PromQL |
| Jaeger | http://localhost:16686 | Distributed tracing (existing) |
| Order API | http://localhost:8081/api/orders | E-commerce API entry point |
