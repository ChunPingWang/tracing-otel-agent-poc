# New Panel Contracts

**Date**: 2026-02-07
**Feature**: 007-enhance-grafana-dashboards

## Service Health Dashboard — New Panels

### Panel: Request Rate by Endpoint

- **Type**: timeseries
- **Position**: Below existing panels (y=36, w=24, h=8)
- **Query**:
  ```promql
  sum(rate(http_server_duration_milliseconds_count{service_name=~"$service"}[$__rate_interval])) by (service_name, http_method, http_route)
  ```
- **Legend**: `{{service_name}} {{http_method}} {{http_route}}`
- **Unit**: reqps (requests per second)
- **Legend table**: last, mean values

### Panel: Latency by Endpoint (p95)

- **Type**: timeseries
- **Position**: Below Request Rate by Endpoint (y=45, w=24, h=8)
- **Query**:
  ```promql
  histogram_quantile(0.95, sum(rate(http_server_duration_milliseconds_bucket{service_name=~"$service"}[$__rate_interval])) by (service_name, http_method, http_route, le)) / 1000
  ```
- **Legend**: `{{service_name}} {{http_method}} {{http_route}}`
- **Unit**: seconds
- **Legend table**: last, mean, max values

---

## JVM Metrics Dashboard — New Panels

### Panel: Memory Pool Usage

- **Type**: timeseries
- **Position**: Below existing panels (y=36, w=24, h=8)
- **Query**:
  ```promql
  process_runtime_jvm_memory_usage_bytes{service_name=~"$service"}
  ```
- **Legend**: `{{service_name}} — {{pool}} ({{type}})`
- **Unit**: bytes
- **Legend table**: last, mean values

### Panel: Class Loading

- **Type**: timeseries
- **Position**: Below Memory Pool (y=45, w=12, h=8) — left half
- **Queries**:
  - Query A (Loaded):
    ```promql
    process_runtime_jvm_classes_current_loaded{service_name=~"$service"}
    ```
  - Query B (Unloaded Rate):
    ```promql
    rate(process_runtime_jvm_classes_unloaded{service_name=~"$service"}[$__rate_interval])
    ```
- **Legend A**: `{{service_name}} — loaded`
- **Legend B**: `{{service_name}} — unloaded/s`
- **Unit A**: short (count)
- **Unit B**: ops (operations/second) — right Y axis

---

## Kafka Metrics Dashboard — New Panels & Variable

### Template Variable: service

- **Name**: `service`
- **Type**: query
- **Query**: `label_values(kafka_producer_record_send_total, service_name)`
- **Multi-select**: true
- **Include All**: true
- **Current value**: `$__all`

### Panel: DLT Messages (send rate)

- **Type**: timeseries
- **Position**: Below existing panels (y=27, w=12, h=8) — left half
- **Query**:
  ```promql
  sum(rate(kafka_producer_record_send_total{topic=~".*\\.DLT"}[$__rate_interval])) by (service_name, topic)
  ```
- **Legend**: `{{service_name}} → {{topic}}`
- **Unit**: ops (messages/s)
- **Legend table**: mean, lastNotNull, max values
- **Note**: When no DLT messages exist, shows "No data" (acceptable)

### Panel: Consumer Lag by Topic

- **Type**: timeseries
- **Position**: Below existing panels (y=27, x=12, w=12, h=8) — right half
- **Query**:
  ```promql
  kafka_consumer_records_lag_max{service_name=~"$service"}
  ```
- **Legend**: `{{service_name}} — {{topic}}`
- **Unit**: short (count)
- **Legend table**: mean, lastNotNull, max values
