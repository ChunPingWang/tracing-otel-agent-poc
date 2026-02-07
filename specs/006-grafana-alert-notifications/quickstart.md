# Quickstart: Grafana Alert Notifications

## Verify Contact Point & Notification Policy

After `docker compose up -d`, verify provisioning:

```bash
# 1. Check contact points
curl -s http://localhost:3000/api/v1/provisioning/contact-points | python3 -m json.tool

# 2. Check notification policy tree
curl -s http://localhost:3000/api/v1/provisioning/policies | python3 -m json.tool

# 3. View in Grafana UI
open http://localhost:3000/alerting/notifications  # Contact Points
open http://localhost:3000/alerting/routes          # Notification Policies
```

## Trigger an Alert and Verify Notification

```bash
# 1. Send orders with non-existent product to trigger 5xx errors
for i in $(seq 1 30); do
  curl -s -X POST http://localhost:8081/api/orders \
    -H "Content-Type: application/json" \
    -d '{"customerId":"C001","items":[{"productId":"P999","quantity":1}]}' > /dev/null
  sleep 1
done

# 2. Wait ~90 seconds for alert to fire, then check webhook receiver logs
docker compose logs webhook-sink --tail 20

# 3. Verify notification payload contains:
#    - "status": "firing"
#    - "alerts[].labels.alertname": "High Error Rate"
#    - "alerts[].labels.severity": "critical"
```

## Verify Resolved Notification

```bash
# 1. Send normal orders to clear the error rate
for i in $(seq 1 30); do
  curl -s -X POST http://localhost:8081/api/orders \
    -H "Content-Type: application/json" \
    -d '{"customerId":"C001","items":[{"productId":"P001","quantity":1}]}' > /dev/null
  sleep 1
done

# 2. Wait ~90 seconds, then check logs for resolved notification
docker compose logs webhook-sink --tail 20

# 3. Verify notification payload contains:
#    - "status": "resolved"
```

## Verify No False Notifications

```bash
# With all services healthy (no simulations), watch for 5 minutes
docker compose logs -f webhook-sink
# Expected: no notification payloads logged
```
