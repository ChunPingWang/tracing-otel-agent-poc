# Phase 0 Research: Grafana Monitoring Dashboard

**Feature**: 004-grafana-dashboard | **Date**: 2026-02-07

## Research Area 1: OpenTelemetry Java Agent v1.32.1 Metrics Capabilities

### Metric Exporters Available

The OTel Java Agent supports these metric exporters via `OTEL_METRICS_EXPORTER`:

| Exporter | Value | Description |
|----------|-------|-------------|
| OTLP | `otlp` | Default. Push metrics via gRPC or HTTP/protobuf. |
| Prometheus | `prometheus` | Embedded HTTP server exposing `/metrics` on port 9464. |
| Logging | `logging` | Logs metrics to stdout (debugging). |
| None | `none` | Disabled (current setting). |

Multiple exporters can be combined: `OTEL_METRICS_EXPORTER=otlp,prometheus`.

### Signal-Specific Configuration

Each telemetry signal (traces, metrics, logs) has independent exporter configuration. This means traces can go to Jaeger via OTLP gRPC while metrics go to a different backend:

```bash
OTEL_TRACES_EXPORTER=otlp
OTEL_EXPORTER_OTLP_TRACES_ENDPOINT=http://jaeger:4317
OTEL_METRICS_EXPORTER=otlp
OTEL_EXPORTER_OTLP_METRICS_ENDPOINT=http://prometheus:9090/api/v1/otlp/v1/metrics
OTEL_EXPORTER_OTLP_METRICS_PROTOCOL=http/protobuf
```

### Auto-Collected Metrics

#### HTTP Server Metrics

| Metric Name | Type | Unit | Status |
|-------------|------|------|--------|
| `http.server.request.duration` | Histogram | `s` | **Stable** |
| `http.server.active_requests` | UpDownCounter | `{request}` | Development |

Key attributes: `http.request.method`, `url.scheme`, `http.response.status_code`, `http.route`, `error.type`.

#### HTTP Client Metrics

| Metric Name | Type | Unit | Status |
|-------------|------|------|--------|
| `http.client.request.duration` | Histogram | `s` | **Stable** |

Key attributes: `http.request.method`, `server.address`, `server.port`, `http.response.status_code`, `error.type`.

#### JVM Metrics (Auto-Collected)

| Metric Name | Type | Unit | Description |
|-------------|------|------|-------------|
| `jvm.memory.used` | UpDownCounter | `By` | Memory used |
| `jvm.memory.committed` | UpDownCounter | `By` | Memory committed |
| `jvm.memory.limit` | UpDownCounter | `By` | Max obtainable memory |
| `jvm.memory.used_after_last_gc` | UpDownCounter | `By` | Memory used after last GC |
| `jvm.gc.duration` | Histogram | `s` | GC pause duration |
| `jvm.thread.count` | UpDownCounter | `{thread}` | Live thread count |
| `jvm.class.loaded` | Counter | `{class}` | Classes loaded since start |
| `jvm.class.count` | UpDownCounter | `{class}` | Currently loaded classes |
| `jvm.cpu.time` | Counter | `s` | CPU time consumed |
| `jvm.cpu.recent_utilization` | Gauge | `1` | Recent CPU utilization |

Attributes: `jvm.memory.type` (heap/non_heap), `jvm.memory.pool.name`, `jvm.gc.name`, `jvm.gc.action`.

#### Database / JDBC Metrics

| Metric Name | Type | Unit | Description |
|-------------|------|------|-------------|
| `db.client.operation.duration` | Histogram | `s` | Database operation duration |
| `db.client.connection.count` | UpDownCounter | `{connection}` | Connection pool (idle/used) |
| `db.client.connection.max` | UpDownCounter | `{connection}` | Max connections |
| `db.client.connection.pending_requests` | UpDownCounter | `{request}` | Pending requests |
| `db.client.connection.create_time` | Histogram | `s` | Connection creation time |
| `db.client.connection.wait_time` | Histogram | `s` | Time to obtain connection |
| `db.client.connection.use_time` | Histogram | `s` | Connection use time |

#### Kafka / Messaging Metrics

| Metric Name | Type | Unit | Description |
|-------------|------|------|-------------|
| `messaging.client.operation.duration` | Histogram | `s` | Duration of send/receive/process |
| `messaging.client.sent.messages` | Counter | `{message}` | Messages sent by producer |
| `messaging.client.consumed.messages` | Counter | `{message}` | Messages consumed |
| `messaging.process.duration` | Histogram | `s` | Processing duration |

Attributes: `messaging.system` (kafka), `messaging.operation.name`, `messaging.destination.name`.

---

## Research Area 2: Prometheus + OTel Metrics Pipeline

### Three Approaches Compared

| Criterion | Approach 1: Prometheus Exporter (Pull) | Approach 2: OTel Collector | Approach 3: Prometheus OTLP Receiver (Push) |
|-----------|---------------------------------------|---------------------------|---------------------------------------------|
| New containers | 1 (Prometheus) | 2 (Collector + Prometheus) | 1 (Prometheus) |
| Config files | 1 (prometheus.yml with 5 scrape jobs) | 2 (collector + prometheus.yml) | 1 (prometheus.yml, no app scrape jobs) |
| Extra ports on apps | Yes (9464 per service) | No | No |
| Agent 1.32.1 compatible | Yes | Yes | Yes |
| Docker Compose ease | Easy | Moderate | **Easiest** |
| K8s ease | Moderate (annotations needed) | Moderate | **Easy** |

### Recommended: Approach 3 — Prometheus Native OTLP Receiver

**Rationale:**
1. Fewest components (only Prometheus, no collector)
2. Simplest config (no per-service scrape targets)
3. No extra ports on app containers
4. Push model matches existing OTLP trace pattern
5. Adding a new service requires zero Prometheus config changes
6. Prometheus 3.x has stable OTLP receiver support

### Prometheus Version

- **Latest stable**: v3.9.1 (January 2026)
- **Latest LTS**: v3.5.1 (January 2026)
- **Recommended for PoC**: `prom/prometheus:v3.5.1` (LTS, stable OTLP support)
- **Important**: `prom/prometheus:latest` on Docker Hub points to v2.x, NOT v3.x! Must use explicit v3.x tag.

### Per-Service Environment Variable Configuration

```yaml
# Traces: continue sending to Jaeger via OTLP gRPC
OTEL_TRACES_EXPORTER: otlp
OTEL_EXPORTER_OTLP_TRACES_ENDPOINT: http://jaeger:4317
OTEL_EXPORTER_OTLP_TRACES_PROTOCOL: grpc

# Metrics: push to Prometheus OTLP receiver via HTTP
OTEL_METRICS_EXPORTER: otlp
OTEL_EXPORTER_OTLP_METRICS_ENDPOINT: http://prometheus:9090/api/v1/otlp/v1/metrics
OTEL_EXPORTER_OTLP_METRICS_PROTOCOL: http/protobuf

# Logs: keep disabled
OTEL_LOGS_EXPORTER: none
```

### Prometheus Configuration (prometheus.yml)

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

otlp:
  promote_resource_attributes:
    - service.instance.id
    - service.name
    - service.namespace

storage:
  tsdb:
    out_of_order_time_window: 30m
```

Start command: `prometheus --web.enable-otlp-receiver --config.file=/etc/prometheus/prometheus.yml`

---

## Research Area 3: Grafana Auto-Provisioning

### Docker Image

- **Image**: `grafana/grafana:11.6.0` (current stable)
- **Port**: 3000 (default)

### Anonymous Access (Environment Variables)

```yaml
GF_AUTH_ANONYMOUS_ENABLED: "true"
GF_AUTH_ANONYMOUS_ORG_ROLE: Viewer
GF_AUTH_DISABLE_LOGIN_FORM: "true"
GF_AUTH_BASIC_ENABLED: "false"
```

### File-Based Provisioning Structure

```text
grafana/
  provisioning/
    datasources/
      datasource.yaml          # Prometheus data source config
    dashboards/
      dashboard.yaml           # Dashboard provider (points to JSON dir)
  dashboards/
    service-health.json        # Dashboard JSON files
    jvm-metrics.json
    kafka-metrics.json
```

Container mount paths:
- `/etc/grafana/provisioning/datasources/` — datasource YAML
- `/etc/grafana/provisioning/dashboards/` — dashboard provider YAML
- `/var/lib/grafana/dashboards/` — dashboard JSON files

### Datasource YAML

```yaml
apiVersion: 1
datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: false
```

### Dashboard Provider YAML

```yaml
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

### Kubernetes Provisioning

For K8s, use ConfigMaps:
- ConfigMap for datasource YAML → mount at `/etc/grafana/provisioning/datasources/`
- ConfigMap for dashboard provider YAML → mount at `/etc/grafana/provisioning/dashboards/`
- ConfigMap(s) for dashboard JSON files → mount at `/var/lib/grafana/dashboards/`

### Docker Compose Pattern

```yaml
grafana:
  image: grafana/grafana:11.6.0
  ports:
    - "3000:3000"
  environment:
    - GF_AUTH_ANONYMOUS_ENABLED=true
    - GF_AUTH_ANONYMOUS_ORG_ROLE=Viewer
    - GF_AUTH_DISABLE_LOGIN_FORM=true
    - GF_AUTH_BASIC_ENABLED=false
  volumes:
    - ./grafana/provisioning/datasources:/etc/grafana/provisioning/datasources
    - ./grafana/provisioning/dashboards:/etc/grafana/provisioning/dashboards
    - ./grafana/dashboards:/var/lib/grafana/dashboards
```

---

## Key Decision: Metrics Pipeline Architecture

**Decision**: Use Prometheus 3.x with native OTLP receiver (Approach 3, push model).

**Impact on existing setup**:
- Change `OTEL_METRICS_EXPORTER` from `none` to `otlp` on all 5 services
- Add signal-specific endpoint env vars to separate trace and metric OTLP destinations
- Add 2 new containers: Prometheus (metrics backend) + Grafana (visualization)
- No application code changes required
- No new ports on application containers

**Fallback**: If Prometheus 3.x OTLP receiver is unreliable, switch to Approach 1 (Prometheus exporter, pull model) by changing `OTEL_METRICS_EXPORTER` to `prometheus` and adding scrape configs.
