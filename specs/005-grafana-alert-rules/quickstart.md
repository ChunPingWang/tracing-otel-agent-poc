# Quickstart: Grafana Alert Rules

## Overview

This feature adds 4 Grafana alert rules provisioned automatically via file-based provisioning. Alert rules are loaded when Grafana starts — no manual configuration needed.

## Alert Rules

| Rule | What it Detects | Threshold | Severity |
|------|-----------------|-----------|----------|
| High Error Rate | 5xx response percentage per service | > 5% | critical |
| High Response Latency (p95) | p95 latency per service | > 2000ms | warning |
| JVM Heap Memory Pressure | Heap usage percentage per service | > 80% | warning |
| Kafka Consumer Lag High | Consumer lag records | > 1000 | warning |

## Changes Required

### 1. Add Datasource UID

Add `uid: prometheus` to `grafana/provisioning/datasources/datasource.yaml` so alert rules can reference it deterministically.

### 2. Create Alert Rules YAML

Create `grafana/provisioning/alerting/alert-rules.yaml` with all 4 alert rules using the Grafana Unified Alerting provisioning format.

### 3. Update Docker Compose

Add volume mount for the alerting provisioning directory:
```yaml
- ./grafana/provisioning/alerting:/etc/grafana/provisioning/alerting
```

### 4. Update K8s Deployment

- Create new ConfigMap `grafana-alert-rules` with the alert rules YAML
- Add volume and volumeMount to the Grafana deployment

### 5. Verify

After deployment, check Grafana Alerting > Alert Rules page at:
- Docker Compose: `http://localhost:3000/alerting/list`
- K8s: `http://localhost:30300/alerting/list`

All 4 rules should appear in the "E-Commerce Alerts" folder.

## Testing Alert Rules

### Trigger Error Rate Alert
```bash
# Enable notification-service failure simulation
curl -X POST "http://localhost:9080/notification/admin/simulate-failure?enabled=true"

# Send orders to generate 5xx errors
for i in $(seq 1 20); do
  curl -s -X POST http://localhost:9080/api/orders \
    -H "Content-Type: application/json" \
    -d '{"customerId":"C001","items":[{"productId":"P001","quantity":1}]}'
done

# Check alerts in Grafana UI after ~60 seconds
# Disable simulation
curl -X POST "http://localhost:9080/notification/admin/simulate-failure?enabled=false"
```

### Trigger Latency Alert
```bash
# Enable payment-service delay simulation (5 seconds)
curl -X POST "http://localhost:9080/payment/admin/simulate-delay?ms=5000"

# Send orders
for i in $(seq 1 10); do
  curl -s -X POST http://localhost:9080/api/orders \
    -H "Content-Type: application/json" \
    -d '{"customerId":"C001","items":[{"productId":"P001","quantity":1}]}'
done

# Check alerts in Grafana UI after ~60 seconds
# Remove delay simulation
curl -X POST "http://localhost:9080/payment/admin/simulate-delay?ms=0"
```
