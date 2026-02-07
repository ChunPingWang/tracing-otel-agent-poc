# Implementation Plan: Grafana Monitoring Dashboard

**Branch**: `004-grafana-dashboard` | **Date**: 2026-02-07 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/004-grafana-dashboard/spec.md`

## Summary

Add Grafana dashboards with Prometheus metrics backend to provide real-time monitoring of the 5 e-commerce microservices. The approach uses Prometheus 3.x native OTLP receiver (push model) so services push metrics via OTLP — the same protocol already used for traces to Jaeger. Grafana is auto-provisioned with 3 pre-configured dashboards (Service Health, JVM Metrics, Kafka Metrics) and anonymous access. Zero application code changes required — only infrastructure configuration changes.

## Technical Context

**Language/Version**: Infrastructure-only feature (YAML, JSON, shell scripts). No Java code changes.
**Primary Dependencies**: Prometheus v3.5.1 (LTS), Grafana 11.6.0, OpenTelemetry Java Agent 1.32.1 (existing)
**Storage**: Prometheus TSDB (ephemeral, in-memory for PoC — no persistent volume)
**Testing**: Manual verification via Docker Compose and K8s; curl-based smoke tests in deploy scripts
**Target Platform**: Docker Compose (local dev) + Kind Kubernetes cluster (APISIX deployment)
**Project Type**: Infrastructure configuration (Docker Compose services, K8s manifests, Grafana JSON dashboards)
**Performance Goals**: Dashboards display live data within 60 seconds; metrics export adds <5% latency overhead (SC-005)
**Constraints**: No application code changes (FR-009); existing OTel agent 1.32.1; Java 8 / Spring Boot 2.7.18
**Scale/Scope**: 5 microservices, 3 dashboards, 2 deployment modes (Docker Compose + K8s)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

This feature is **infrastructure-only** — it adds Docker Compose services, Kubernetes manifests, Grafana dashboard JSON files, and Prometheus configuration. No Java application code is created or modified.

| Constitution Principle | Applicable? | Assessment |
|----------------------|-------------|------------|
| I. Hexagonal Architecture | **N/A** | No Java code changes. Infrastructure config only. |
| II. Domain-Driven Design | **N/A** | No domain logic changes. |
| III. SOLID Principles | **N/A** | No Java code changes. |
| IV. Test-Driven Development | **N/A** | No production Java code. Deploy scripts are verified via smoke tests. |
| V. Behavior-Driven Development | **Partial** | Spec acceptance scenarios follow Given-When-Then. Manual verification replaces automated BDD tests for infrastructure. |
| VI. Code Quality Standards | **Partial** | Shell scripts follow existing project style. JSON dashboards are well-structured. |
| Layered Architecture Constraints | **N/A** | No Java package changes. |
| Testing Standards | **N/A** | No Java test code. Infrastructure verified via deploy-and-verify pattern. |

**Result**: PASS. No violations. Constitution principles primarily govern Java application code; this feature only modifies infrastructure configuration.

## Project Structure

### Documentation (this feature)

```text
specs/004-grafana-dashboard/
├── plan.md              # This file
├── research.md          # Phase 0 output (completed)
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── README.md        # N/A explanation (infrastructure feature)
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
# New files for this feature
grafana/
├── provisioning/
│   ├── datasources/
│   │   └── datasource.yaml          # Prometheus data source config
│   └── dashboards/
│       └── dashboard.yaml           # Dashboard provider config
└── dashboards/
    ├── service-health.json          # Service Health Overview dashboard (P1)
    ├── jvm-metrics.json             # JVM Metrics dashboard (P2)
    └── kafka-metrics.json           # Kafka Metrics dashboard (P2)

prometheus/
└── prometheus.yml                   # Prometheus config with OTLP receiver

# Modified files
docker-compose.yml                   # Add prometheus + grafana services; update env vars
apisix-k8s/
├── prometheus/                      # New: Prometheus K8s manifests
│   ├── deployment.yaml
│   ├── service.yaml
│   └── configmap.yaml
├── grafana/                         # New: Grafana K8s manifests
│   ├── deployment.yaml
│   ├── service.yaml
│   ├── configmap-datasources.yaml
│   ├── configmap-dashboard-providers.yaml
│   └── configmap-dashboards.yaml
├── order-service-blue/deployment.yaml   # Modified: metrics env vars
├── order-service-green/deployment.yaml  # Modified: metrics env vars
├── product-service/deployment.yaml      # Modified: metrics env vars
├── inventory-service/deployment.yaml    # Modified: metrics env vars
├── payment-service/deployment.yaml      # Modified: metrics env vars
└── notification-service/deployment.yaml # Modified: metrics env vars

scripts/
└── apisix-deploy.sh                # Modified: add Prometheus + Grafana deploy steps
```

**Structure Decision**: Infrastructure-only feature. New `grafana/` and `prometheus/` directories at project root for Docker Compose config files. New `apisix-k8s/prometheus/` and `apisix-k8s/grafana/` directories for K8s manifests. No Java source code directories involved.

## Design Decisions

### D1: Metrics Pipeline — Prometheus OTLP Receiver (Push Model)

**Decision**: Use Prometheus 3.5.1 (LTS) with `--web.enable-otlp-receiver` to receive OTLP metrics directly from the OTel Java Agent.

**Rationale**: (see research.md for full comparison)
- Fewest new components (Prometheus only, no OTel Collector)
- Simplest configuration (no per-service scrape targets)
- No extra ports on application containers
- Push model matches existing OTLP trace export pattern
- Adding new services requires zero Prometheus config changes

**Tradeoff**: Push model is newer than traditional Prometheus pull. Mitigated by using LTS release.

### D2: Environment Variable Strategy

**Decision**: Use signal-specific OTLP endpoint environment variables to split traces (→ Jaeger gRPC) and metrics (→ Prometheus HTTP).

```yaml
# Replace current OTEL_EXPORTER_OTLP_ENDPOINT with signal-specific vars
OTEL_TRACES_EXPORTER: otlp
OTEL_EXPORTER_OTLP_TRACES_ENDPOINT: http://jaeger:4317
OTEL_METRICS_EXPORTER: otlp
OTEL_EXPORTER_OTLP_METRICS_ENDPOINT: http://prometheus:9090/api/v1/otlp/v1/metrics
OTEL_EXPORTER_OTLP_METRICS_PROTOCOL: http/protobuf
OTEL_LOGS_EXPORTER: none
```

**Rationale**: The generic `OTEL_EXPORTER_OTLP_ENDPOINT` cannot serve both gRPC (Jaeger) and HTTP (Prometheus OTLP) simultaneously. Signal-specific variables are the standard OTel approach.

### D3: Grafana Version and Access

**Decision**: `grafana/grafana:11.6.0` with anonymous access (no login required).

**Rationale**: PoC does not need authentication. Anonymous viewer access lets users open dashboards immediately.

### D4: Dashboard Organization

**Decision**: 3 separate dashboards matching spec requirements:

| Dashboard | Priority | Key Panels |
|-----------|----------|------------|
| Service Health Overview | P1 | Request rate, error rate, p50/p95/p99 latency per service |
| JVM Metrics | P2 | Heap memory, GC duration/count, thread count with service selector |
| Kafka Metrics | P2 | Producer send rate, consumer receive rate, consumer lag |

DB Query Metrics (P3) will be included as additional panels in the Service Health dashboard rather than a separate dashboard, to reduce initial complexity.

## Complexity Tracking

> No constitution violations to track. Feature is infrastructure-only.
