# Implementation Plan: Enhance Existing Grafana Dashboards

**Branch**: `007-enhance-grafana-dashboards` | **Date**: 2026-02-07 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/007-enhance-grafana-dashboards/spec.md`

## Summary

Enhance the 3 existing Grafana dashboards with deeper observability panels:
1. **Service Health**: Add per-endpoint request rate and p95 latency panels using `http_route` + `http_method` labels.
2. **JVM Metrics**: Add per-memory-pool usage panel (Eden, Survivor, Old Gen, Metaspace) and class loading panel.
3. **Kafka Metrics**: Add DLT message monitoring panel, per-topic consumer lag panel, and a service selector dropdown.

All changes are purely additive (existing panels unchanged). Changes apply to both Docker Compose and K8s (Kind) deployments.

## Technical Context

**Language/Version**: N/A — infrastructure-only (Grafana dashboard JSON, K8s YAML)
**Primary Dependencies**: Grafana 11.6.0, Prometheus v3.5.1, OTel Java Agent 1.32.1
**Storage**: N/A
**Testing**: Manual/scripted verification via Grafana UI and PromQL queries
**Target Platform**: Docker Compose + Kind Kubernetes
**Project Type**: Infrastructure configuration
**Performance Goals**: Panels load within 10 seconds, auto-refresh at 10s interval
**Constraints**: Old OTel semconv metric names only; no Java code changes
**Scale/Scope**: 3 dashboard JSON files modified, 1 K8s ConfigMap regenerated

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**PASS — N/A**: This feature modifies only Grafana dashboard JSON files and K8s ConfigMaps. No Java code is written or changed. The constitution's principles (Hexagonal Architecture, DDD, SOLID, TDD, BDD, Code Quality) apply exclusively to Java microservice code and are not applicable to infrastructure configuration changes.

**Post-Phase 1 re-check**: Still PASS — no design decisions introduced any Java code.

## Project Structure

### Documentation (this feature)

```text
specs/007-enhance-grafana-dashboards/
├── plan.md              # This file
├── research.md          # Phase 0: metric names, labels, PromQL patterns
├── data-model.md        # Phase 1: metrics and panel specifications
├── quickstart.md        # Phase 1: verification commands
├── contracts/
│   └── new-panels.md    # Phase 1: panel position, query, and layout contracts
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
grafana/dashboards/
├── service-health.json      # MODIFY: add 2 panels (endpoint breakdown)
├── jvm-metrics.json         # MODIFY: add 2 panels (memory pool, class loading)
└── kafka-metrics.json       # MODIFY: add 2 panels (DLT, lag by topic) + template variable

apisix-k8s/grafana/
└── configmap-dashboards.yaml  # MODIFY: regenerate with updated JSON
```

**Structure Decision**: No new files created — only modifications to 4 existing files (3 dashboard JSONs + 1 K8s ConfigMap).

## Implementation Steps

### Phase 1: Service Health Dashboard Enhancement (US1)

1. Read `grafana/dashboards/service-health.json` and understand current panel structure
2. Add "Request Rate by Endpoint" panel at y=36 using `http_server_duration_milliseconds_count` with `http_method` + `http_route` labels
3. Add "Latency by Endpoint (p95)" panel at y=45 using `histogram_quantile(0.95, ...)` with endpoint breakdown
4. Verify: Start Docker Compose, send orders, confirm per-endpoint data appears

### Phase 2: JVM Metrics Dashboard Enhancement (US2)

5. Read `grafana/dashboards/jvm-metrics.json` and understand current panel structure
6. Add "Memory Pool Usage" panel at y=36 using `process_runtime_jvm_memory_usage_bytes` grouped by `pool`
7. Add "Class Loading" panel at y=45 using `process_runtime_jvm_classes_current_loaded` and `process_runtime_jvm_classes_unloaded`
8. Verify: Confirm per-pool memory data and class loading counts appear

### Phase 3: Kafka Metrics Dashboard Enhancement (US3)

9. Read `grafana/dashboards/kafka-metrics.json` and understand current panel structure
10. Add `service` template variable using `label_values(kafka_producer_record_send_total, service_name)`
11. Add "DLT Messages" panel at y=27 (left) using `kafka_producer_record_send_total{topic=~".*\\.DLT"}`
12. Add "Consumer Lag by Topic" panel at y=27 (right) using `kafka_consumer_records_lag_max` by topic
13. Verify: Enable failure simulation, send orders, confirm DLT panel shows data after retries exhaust

### Phase 4: K8s Integration

14. Regenerate `apisix-k8s/grafana/configmap-dashboards.yaml` with all 3 updated dashboard JSONs

### Phase 5: Polish

15. Update `README.md` — update dashboard descriptions to mention new panels
16. Update `apisix-k8s/README.md` — update dashboard descriptions
17. Final verification: `docker compose down -v && docker compose up -d`, confirm all new panels load
