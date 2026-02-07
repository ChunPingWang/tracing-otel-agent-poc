# Research: Grafana Alert Rules

## R1: Grafana File-Based Alert Provisioning Format

**Decision**: Use Grafana Unified Alerting file-based provisioning via YAML files in `/etc/grafana/provisioning/alerting/`.

**Rationale**: Grafana 11.x (our version: 11.6.0) only supports Unified Alerting — legacy alerting was completely removed in v11.0. File-based provisioning requires no intermediary config file (unlike dashboards); Grafana automatically scans the `provisioning/alerting/` directory at startup and loads all YAML/JSON files found.

**Alternatives considered**:
- Grafana HTTP API provisioning — rejected because it requires Grafana to be running first and adds deployment ordering complexity.
- Grafana Terraform provider — overkill for a PoC.

## R2: Alert Rule YAML Structure (Three-Step Pipeline)

**Decision**: Use the Reduce+Threshold pipeline pattern (A → B → C) for all alert rules.

**Rationale**: The recommended approach for Grafana Unified Alerting uses a three-step pipeline:
1. **Step A (refId: A)**: Prometheus query — fetches the metric data.
2. **Step B (refId: B)**: Reduce expression (`datasourceUid: __expr__`, `type: reduce`) — reduces time series to a single value (e.g., `last`, `mean`).
3. **Step C (refId: C)**: Threshold expression (`datasourceUid: __expr__`, `type: threshold`) — compares the reduced value against a threshold.

The `condition` field on the rule points to the final step (refId `C`). This approach generates **one alert instance per matching series** (multi-dimensional alerting), which is essential for per-service alerts.

**Alternatives considered**:
- Classic conditions (`type: classic_conditions`) — rejected because it collapses all series into a single alert instance, losing per-service granularity.

## R3: NoData and Error State Handling

**Decision**: Use `noDataState: OK` and `execErrState: Error`.

**Rationale**:
- `noDataState: OK` — When a metric has no data (e.g., service just started, no requests yet), the alert stays in Normal/OK state. This prevents false-positive alerts during startup or low-traffic periods. This directly satisfies FR-007.
- `execErrState: Error` — When the Prometheus query fails (e.g., Prometheus is unreachable), the alert enters an Error state. This is visible in the UI and distinguishable from a real alert firing.

**Alternatives considered**:
- `noDataState: NoData` — creates a distinct "No Data" state visible in the UI but could be confusing for operators who expect only Normal/Alerting.
- `noDataState: KeepLast` — maintains previous state, but on fresh start there's no previous state to keep.

## R4: Datasource UID for Alert Rules

**Decision**: Add an explicit `uid: prometheus` to the datasource provisioning YAML, and reference it in alert rules as `datasourceUid: prometheus`.

**Rationale**: Alert rule YAML requires `datasourceUid` to reference the Prometheus datasource. Without an explicit UID in the datasource provisioning, Grafana auto-generates one at startup — which is non-deterministic and would break the alert rule references. Dashboard JSON files currently use `"type": "prometheus"` (by type, not by UID), so adding a UID is backward-compatible.

**Alternatives considered**:
- Using datasource name instead of UID — Grafana alert provisioning requires UID, not name.

## R5: Docker Compose and K8s Volume Mounts

**Decision**: Add a new volume mount for `provisioning/alerting/` in both Docker Compose and K8s deployments.

**Rationale**: The current Docker Compose mounts only `datasources/` and `dashboards/` provisioning directories. The K8s deployment uses ConfigMaps with `subPath` mounts. Both need an additional mount for the `alerting/` directory.

- **Docker Compose**: Add `./grafana/provisioning/alerting:/etc/grafana/provisioning/alerting`
- **K8s**: Add a new ConfigMap `grafana-alert-rules` mounted at `/etc/grafana/provisioning/alerting/`

## R6: Alert Rule Evaluation Interval and Pending Duration

**Decision**: Use `interval: 30s` for the alert rule group, and `for: 1m` as the pending duration.

**Rationale**:
- `interval: 30s` — evaluates rules every 30 seconds, matching the spec's requirement for alert detection within ~60 seconds (2 evaluation cycles).
- `for: 1m` — a rule must be in "firing" state for 1 minute before transitioning to "Alerting". This prevents transient spikes from triggering alerts while still keeping detection time within the 60-second target specified in SC-002/SC-003.

## R7: PromQL Queries for Each Alert Rule

**Decision**: Use the following PromQL expressions, aligned with OTel Java Agent v1.32.1 old semantic convention metric names:

| Alert | PromQL Expression |
|-------|-------------------|
| High Error Rate | `sum(rate(http_server_duration_milliseconds_count{http_status_code=~"5.."}[5m])) by (service_name) / sum(rate(http_server_duration_milliseconds_count[5m])) by (service_name) * 100` |
| High Latency (p95) | `histogram_quantile(0.95, sum(rate(http_server_duration_milliseconds_bucket[5m])) by (le, service_name))` |
| JVM Heap Pressure | `sum(process_runtime_jvm_memory_usage_bytes{type="heap"}) by (service_name) / sum(process_runtime_jvm_memory_limit_bytes{type="heap"}) by (service_name) * 100` |
| Kafka Consumer Lag | `kafka_consumer_records_lag_max` |

**Rationale**: These queries match the exact metric names used by the existing Grafana dashboards (Feature 004), ensuring consistency. The error rate and latency queries are identical patterns to those in `service-health.json`.
