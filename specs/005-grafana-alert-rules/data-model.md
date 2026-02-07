# Data Model: Grafana Alert Rules

This feature does not introduce new application-level data entities. The "data model" consists of Grafana provisioning configuration artifacts.

## Provisioning Artifacts

### Alert Rule

An alert rule definition within the Grafana Unified Alerting provisioning YAML.

| Attribute | Description |
|-----------|-------------|
| uid | Unique identifier (max 40 chars, alphanumeric + `-` and `_`) |
| title | Human-readable rule name displayed in Grafana UI |
| condition | Reference to the expression step (refId) that determines firing |
| data | Array of query and expression steps (A → B → C pipeline) |
| for | Pending duration before transitioning from Pending to Alerting |
| noDataState | Behavior when query returns no data (OK, NoData, Alerting, KeepLast) |
| execErrState | Behavior when query execution fails (Error, Alerting, OK, KeepLast) |
| labels | Key-value pairs for categorization and routing (e.g., severity, service) |
| annotations | Key-value metadata for notifications (e.g., summary, description) |

### Alert Rule Group

A logical grouping of alert rules sharing an evaluation interval.

| Attribute | Description |
|-----------|-------------|
| orgId | Grafana organization ID (always 1 for this PoC) |
| name | Group name (e.g., "ecommerce-service-alerts") |
| folder | Grafana folder where the group is displayed |
| interval | Evaluation interval (e.g., "30s") |
| rules | Array of Alert Rule definitions |

## Alert Rules Inventory

| Rule UID | Title | Metric Category | Threshold | Severity |
|----------|-------|-----------------|-----------|----------|
| `high-error-rate` | High Error Rate | HTTP 5xx % | > 5% | critical |
| `high-latency-p95` | High Response Latency (p95) | HTTP latency ms | > 2000ms | warning |
| `jvm-heap-pressure` | JVM Heap Memory Pressure | JVM heap % | > 80% | warning |
| `kafka-consumer-lag` | Kafka Consumer Lag High | Consumer lag records | > 1000 | warning |

## State Transitions

```
Normal → Pending → Alerting → Normal
  │         │                    ▲
  │         └── (condition clears before `for` duration) ──┘
  │
  └── NoData (when metric has no data, stays OK per config)
  └── Error (when Prometheus query fails)
```

- **Normal**: Condition not met. All healthy.
- **Pending**: Condition met but `for` duration not yet elapsed.
- **Alerting**: Condition met for longer than `for` duration.
- **NoData**: No metric data returned. Configured as OK (Normal).
- **Error**: Query execution failed. Distinct visual state in UI.
