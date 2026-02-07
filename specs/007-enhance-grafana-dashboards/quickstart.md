# Quickstart: Verify Enhanced Grafana Dashboards

**Feature**: 007-enhance-grafana-dashboards

## Prerequisites

- Docker Compose stack running: `docker compose up --build -d`
- Wait ~60 seconds for services to start and metrics to populate

## Verification Steps

### 1. Generate Traffic

```bash
# Send 10 normal orders to populate all endpoints
for i in $(seq 1 10); do
  curl -s -X POST http://localhost:8081/api/orders \
    -H "Content-Type: application/json" \
    -d '{"customerId":"C001","items":[{"productId":"P001","quantity":1}]}'
  sleep 1
done
```

### 2. Verify Service Health — Per-Endpoint Panels

Open http://localhost:3000 → Service Health Overview dashboard.

- Scroll below existing panels to find **"Request Rate by Endpoint"** panel
- Verify at least 4 series: `POST /api/orders`, `GET /api/products/{productId}`, `POST /api/inventory/reserve`, `POST /api/payments`
- Verify **"Latency by Endpoint (p95)"** panel shows per-endpoint latency

### 3. Verify JVM Metrics — Memory Pool & Class Loading

Open JVM Metrics dashboard.

- Scroll to **"Memory Pool Usage"** panel
- Verify pools: `PS Eden Space`, `PS Survivor Space`, `PS Old Gen`, `Metaspace`, `Code Cache`
- Verify **"Class Loading"** panel shows loaded class count

### 4. Verify Kafka Metrics — DLT & Service Selector

Open Kafka Metrics dashboard.

```bash
# Trigger DLT: enable failure simulation, then send orders
curl -X POST "http://localhost:8085/api/admin/simulate-failure?enabled=true"
for i in $(seq 1 5); do
  curl -s -X POST http://localhost:8081/api/orders \
    -H "Content-Type: application/json" \
    -d '{"customerId":"C001","items":[{"productId":"P001","quantity":1}]}'
  sleep 1
done

# Wait for retries to exhaust (~45s)
sleep 45

# Disable failure simulation
curl -X POST "http://localhost:8085/api/admin/simulate-failure?enabled=false"
```

- Verify **"DLT Messages"** panel shows send rate for `order-confirmed.DLT`
- Verify **"Consumer Lag by Topic"** panel shows lag per topic
- Verify **service selector dropdown** is available and filters Kafka metrics

### 5. Verify Payment Delay Visibility

```bash
# Enable 5-second delay on payment-service
curl -X POST "http://localhost:8084/api/admin/simulate-delay?ms=5000"

# Send orders
for i in $(seq 1 5); do
  curl -s -X POST http://localhost:8081/api/orders \
    -H "Content-Type: application/json" \
    -d '{"customerId":"C001","items":[{"productId":"P001","quantity":1}]}'
  sleep 1
done

# Wait for metrics
sleep 30

# Disable delay
curl -X POST "http://localhost:8084/api/admin/simulate-delay?ms=0"
```

- Verify "Latency by Endpoint (p95)" shows `POST /api/payments` > 4 seconds while others < 1 second

### 6. Verify Existing Panels Unchanged

- All original panels (Request Rate, Error Rate, Response Latency, DB Connection Pool, Heap Memory, GC, Thread Count, Producer/Consumer rates) still display data correctly
