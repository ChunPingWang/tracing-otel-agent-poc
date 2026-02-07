# Research: Enhance Existing Grafana Dashboards

**Date**: 2026-02-07
**Feature**: 007-enhance-grafana-dashboards

## R1: Per-Endpoint HTTP Metrics Labels

**Decision**: Use existing `http_route` and `http_method` labels on `http_server_duration_milliseconds_*` metrics for per-endpoint breakdown.

**Rationale**: OTel Java Agent v1.32.1 with Spring MVC automatically captures `http_route` (e.g., `/api/orders`, `/api/products/{id}`) and `http_method` (e.g., `GET`, `POST`) labels. These are available by default — no configuration change needed. The route uses template placeholders (`{id}`) for low cardinality.

**Alternatives considered**:
- Custom metric with span-level data: Unnecessary — labels already exist on default metrics.
- Enable new semconv via `OTEL_SEMCONV_STABILITY_OPT_IN`: Unnecessary — old semconv labels work correctly.

**Expected `http_route` values for this project**:

| Service | Endpoint | `http_route` | `http_method` |
|---------|----------|-------------|---------------|
| order-service | Create order | `/api/orders` | `POST` |
| product-service | Get product | `/api/products/{productId}` | `GET` |
| inventory-service | Reserve | `/api/inventory/reserve` | `POST` |
| inventory-service | Release | `/api/inventory/release` | `POST` |
| payment-service | Process payment | `/api/payments` | `POST` |
| payment-service | Admin delay | `/api/admin/simulate-delay` | `POST` |
| notification-service | Admin failure | `/api/admin/simulate-failure` | `POST` |

## R2: JVM Memory Pool Metrics

**Decision**: Use `process_runtime_jvm_memory_usage_bytes` with `pool` label for per-pool breakdown.

**Rationale**: OTel Java Agent v1.32.1 uses **old semantic conventions** (`process_runtime_jvm_*`). The existing JVM dashboard already uses this metric with `type` label (heap/non_heap). The same metric also carries a `pool` label that identifies specific memory pools.

**JDK 8 Parallel GC pool names** (used in this project's `eclipse-temurin:8-jre`):

| Pool Name | Type | Description |
|-----------|------|-------------|
| `PS Eden Space` | heap | Young generation — new objects allocated here |
| `PS Survivor Space` | heap | Young generation — objects surviving minor GC |
| `PS Old Gen` | heap | Old generation — long-lived objects |
| `Metaspace` | non_heap | Class metadata storage |
| `Code Cache` | non_heap | JIT compiled code |
| `Compressed Class Space` | non_heap | Compressed class pointers |

**PromQL for per-pool breakdown**:
```promql
process_runtime_jvm_memory_usage_bytes{service_name=~"$service"}
# Returns one series per (service_name, type, pool) combination
```

**Alternatives considered**:
- New semconv `jvm_memory_used_bytes`: Not available — v1.32.1 uses old naming.

## R3: JVM Class Loading Metrics

**Decision**: Use `process_runtime_jvm_classes_current_loaded` (gauge) and `process_runtime_jvm_classes_unloaded` (counter) for class loading panels.

**Rationale**: These are the old semconv metric names emitted by OTel Java Agent v1.32.1. The `current_loaded` metric is a gauge showing the current number of loaded classes. The `unloaded` metric is a monotonic counter of total unloaded classes.

**Note**: These metrics have no additional labels beyond `service_name` (resource attribute).

**Alternatives considered**:
- New semconv `jvm_class_count` / `jvm_class_loaded_total`: Not available on v1.32.1 old semconv.

## R4: Kafka DLT Monitoring

**Decision**: Use `kafka_producer_record_send_total` with `topic=~".*\\.DLT"` filter for DLT message tracking.

**Rationale**: The OTel Java Agent bridges native Kafka JMX metrics, including per-topic producer metrics. The `topic` label is available on `kafka_producer_record_send_total`. When Spring Kafka's DLT mechanism sends a message to `order-confirmed.DLT`, the OTel agent instruments this producer operation and the `topic` label will show the DLT topic name.

**PromQL for DLT monitoring**:
```promql
sum(rate(kafka_producer_record_send_total{topic=~".*\\.DLT"}[$__rate_interval])) by (service_name, topic)
```

**Alternatives considered**:
- Custom application metric: Requires code changes — violates zero-code-change principle.
- OTel Collector with Kafka Metrics Receiver: Adds unnecessary infrastructure for PoC.

## R5: Kafka Per-Partition Consumer Lag

**Decision**: Use `kafka_consumer_records_lag_max` broken down by `topic` label instead of per-partition. Add topic-level detail to supplement aggregate view.

**Rationale**: Per-partition lag (`records-lag` per partition) is NOT available by default from the OTel Java Agent v1.32.1. It requires custom JMX configuration or an OTel Collector with Kafka Metrics Receiver — both add complexity beyond PoC scope. However, `kafka_consumer_records_lag_max` does have a `topic` label, allowing per-topic lag visibility.

**What we CAN show**:
- `kafka_consumer_records_lag_max` by `topic` — max lag per topic assigned to the consumer
- This is sufficient for the PoC since the `order-confirmed` topic has 1 partition

**Alternatives considered**:
- Custom JMX YAML configuration for per-partition metrics: Too complex for PoC scope.
- OTel Collector Kafka Metrics Receiver: Requires additional infrastructure component.

**Spec impact**: FR-006 requests "lag per topic-partition". We'll implement per-topic lag (which for a single-partition topic is equivalent to per-partition lag). The panel title will be "Consumer Lag by Topic" instead of "by Partition".

## R6: Kafka Dashboard Service Selector

**Decision**: Add a `service` template variable to the Kafka dashboard matching the pattern in Service Health and JVM dashboards.

**Rationale**: The existing Service Health and JVM dashboards both have a `service` template variable using `label_values(..., service_name)`. The Kafka dashboard currently has no template variables. Adding one improves consistency and enables filtering Kafka metrics by producer/consumer service.

**PromQL for variable**:
```promql
label_values(kafka_producer_record_send_total, service_name)
```

## R7: Dashboard Panel Positioning Strategy

**Decision**: Append new panels below existing panels, maintaining the current layout.

**Rationale**: Each dashboard uses `gridPos` with `y` coordinates for vertical positioning. New panels will be added with `y` values beyond the current maximum, preserving all existing panel positions. This ensures FR-010 (no changes to existing panels).

**Current panel layout**:

| Dashboard | Current max y | Rows used | New panels start at |
|-----------|--------------|-----------|-------------------|
| Service Health | y=27, h=8 → bottom=35 | 35 rows | y=36 |
| JVM Metrics | y=27, h=8 → bottom=35 | 35 rows | y=36 |
| Kafka Metrics | y=18, h=8 → bottom=26 | 26 rows | y=27 |
