# Data Model: Grafana Monitoring Dashboard

**Feature**: 004-grafana-dashboard | **Date**: 2026-02-07

## Overview

This feature is infrastructure-only. There are no application-level data model changes (no new entities, DTOs, or database tables). The "data model" for this feature consists of:

1. **Metrics data** flowing from OTel Java Agent → Prometheus TSDB
2. **Grafana provisioning configuration** (YAML + JSON files)

## Metrics Data Flow

```
┌─────────────────────┐    OTLP gRPC     ┌────────┐
│   OTel Java Agent   │ ──── traces ────→ │ Jaeger │
│  (per microservice) │                   └────────┘
│                     │  OTLP HTTP/proto  ┌────────────┐    PromQL    ┌─────────┐
│                     │ ── metrics ─────→ │ Prometheus │ ◄─────────── │ Grafana │
└─────────────────────┘                   │   (TSDB)   │              │ (UI)    │
                                          └────────────┘              └─────────┘
```

## Metric Names Used in Dashboards

### Service Health Dashboard

| PromQL Metric | OTel Source | Description |
|---------------|-------------|-------------|
| `http_server_request_duration_seconds_count` | `http.server.request.duration` | Request count (rate) |
| `http_server_request_duration_seconds_bucket` | `http.server.request.duration` | Latency histogram (p50/p95/p99) |
| `http_server_request_duration_seconds_sum` | `http.server.request.duration` | Total duration (for avg calculation) |

Note: Prometheus translates OTLP metric names by replacing `.` with `_` and adding `_seconds` suffix for histogram types with unit `s`.

Error rate is derived by filtering on `http_response_status_code >= 500`.

### JVM Metrics Dashboard

| PromQL Metric | OTel Source | Description |
|---------------|-------------|-------------|
| `jvm_memory_used_bytes` | `jvm.memory.used` | Heap/non-heap memory used |
| `jvm_memory_committed_bytes` | `jvm.memory.committed` | Memory committed |
| `jvm_memory_limit_bytes` | `jvm.memory.limit` | Max memory |
| `jvm_gc_duration_seconds_count` | `jvm.gc.duration` | GC event count |
| `jvm_gc_duration_seconds_sum` | `jvm.gc.duration` | Total GC pause time |
| `jvm_thread_count` | `jvm.thread.count` | Live threads |

### Kafka Metrics Dashboard

| PromQL Metric | OTel Source | Description |
|---------------|-------------|-------------|
| `messaging_client_sent_messages_total` | `messaging.client.sent.messages` | Producer messages sent |
| `messaging_client_consumed_messages_total` | `messaging.client.consumed.messages` | Consumer messages received |
| `messaging_client_operation_duration_seconds` | `messaging.client.operation.duration` | Operation duration |
| `messaging_process_duration_seconds` | `messaging.process.duration` | Processing duration |

### DB Metrics (P3, in Service Health dashboard)

| PromQL Metric | OTel Source | Description |
|---------------|-------------|-------------|
| `db_client_operation_duration_seconds` | `db.client.operation.duration` | Query duration |
| `db_client_connection_count` | `db.client.connection.count` | Connection pool usage |

## Provisioning File Structure

### Datasource Configuration (YAML)

```yaml
# grafana/provisioning/datasources/datasource.yaml
apiVersion: 1
datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: false
```

### Dashboard Provider Configuration (YAML)

```yaml
# grafana/provisioning/dashboards/dashboard.yaml
apiVersion: 1
providers:
  - name: 'default'
    orgId: 1
    folder: ''
    type: file
    disableDeletion: false
    updateIntervalSeconds: 10
    options:
      path: /var/lib/grafana/dashboards
```

### Dashboard JSON Structure

Each dashboard JSON file follows the Grafana dashboard schema:

```json
{
  "uid": "<unique-id>",
  "title": "<dashboard-title>",
  "tags": ["otel", "ecommerce"],
  "timezone": "browser",
  "refresh": "10s",
  "time": { "from": "now-15m", "to": "now" },
  "templating": {
    "list": [
      {
        "name": "service",
        "type": "query",
        "query": "label_values(http_server_request_duration_seconds_count, service_name)"
      }
    ]
  },
  "panels": [ ... ]
}
```

## No Application-Level Changes

- No new Java entities, DTOs, or repositories
- No database schema changes
- No new API endpoints
- Only infrastructure configuration (env vars, Docker Compose services, K8s manifests, config files)
