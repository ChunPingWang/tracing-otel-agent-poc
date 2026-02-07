# Data Model: Enhance Existing Grafana Dashboards

**Date**: 2026-02-07
**Feature**: 007-enhance-grafana-dashboards

## Overview

This feature modifies existing Grafana dashboard JSON files — no new data entities or databases are involved. The "data model" describes the Prometheus metric names, labels, and PromQL queries used in new panels.

## New Metrics Used (already collected by OTel Agent, not currently displayed)

### Service Health Dashboard — New Panels

| Panel | Metric | Key Labels | PromQL Pattern |
|-------|--------|------------|---------------|
| Request Rate by Endpoint | `http_server_duration_milliseconds_count` | `service_name`, `http_method`, `http_route` | `sum(rate(...[$__rate_interval])) by (service_name, http_method, http_route)` |
| Latency by Endpoint (p95) | `http_server_duration_milliseconds_bucket` | `service_name`, `http_method`, `http_route`, `le` | `histogram_quantile(0.95, sum(rate(...[$__rate_interval])) by (service_name, http_method, http_route, le))` |

### JVM Metrics Dashboard — New Panels

| Panel | Metric | Key Labels | PromQL Pattern |
|-------|--------|------------|---------------|
| Memory Pool Usage | `process_runtime_jvm_memory_usage_bytes` | `service_name`, `type`, `pool` | `...{service_name=~"$service"}` grouped by `pool` |
| Class Loading | `process_runtime_jvm_classes_current_loaded` | `service_name` | `...{service_name=~"$service"}` |
| Class Unloading | `process_runtime_jvm_classes_unloaded` | `service_name` | `rate(...[$__rate_interval])` |

### Kafka Metrics Dashboard — New Panels

| Panel | Metric | Key Labels | PromQL Pattern |
|-------|--------|------------|---------------|
| DLT Messages | `kafka_producer_record_send_total` | `service_name`, `topic` | `sum(rate(...{topic=~".*\\.DLT"}[$__rate_interval])) by (service_name, topic)` |
| Consumer Lag by Topic | `kafka_consumer_records_lag_max` | `service_name`, `topic` | `...{service_name=~"$service"}` grouped by `topic` |

### Kafka Dashboard — New Template Variable

| Variable | Query | Multi-select | Include All |
|----------|-------|-------------|-------------|
| `service` | `label_values(kafka_producer_record_send_total, service_name)` | Yes | Yes |

## Affected Files

| File | Change Type | Description |
|------|-------------|-------------|
| `grafana/dashboards/service-health.json` | Modify | Add 2 new panels (Request Rate by Endpoint, Latency by Endpoint) |
| `grafana/dashboards/jvm-metrics.json` | Modify | Add 2 new panels (Memory Pool Usage, Class Loading) |
| `grafana/dashboards/kafka-metrics.json` | Modify | Add 2 new panels (DLT Messages, Consumer Lag by Topic) + 1 template variable |
| `apisix-k8s/grafana/configmap-dashboards.yaml` | Modify | Regenerate K8s ConfigMap with updated dashboard JSON |
