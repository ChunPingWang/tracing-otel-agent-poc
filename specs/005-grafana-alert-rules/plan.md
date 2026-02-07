# Implementation Plan: Grafana Alert Rules

**Branch**: `005-grafana-alert-rules` | **Date**: 2026-02-07 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/005-grafana-alert-rules/spec.md`

## Summary

Add 4 Grafana alert rules (high error rate, high latency, JVM heap pressure, Kafka consumer lag) provisioned automatically via file-based YAML configuration. Alert rules use the Grafana Unified Alerting three-step pipeline (Prometheus query → Reduce → Threshold) and work in both Docker Compose and Kubernetes (Kind) deployments. No external notification channels — alerts are visible in the Grafana Alerting UI only.

## Technical Context

**Language/Version**: YAML configuration (Grafana provisioning format, apiVersion: 1)
**Primary Dependencies**: Grafana 11.6.0 (Unified Alerting), Prometheus (datasource)
**Storage**: N/A (Grafana internal state for alert evaluation)
**Testing**: Manual verification via Grafana UI + scripted curl tests with simulation endpoints
**Target Platform**: Docker Compose (local dev) + Kubernetes Kind cluster
**Project Type**: Configuration-only feature (no application code changes)
**Performance Goals**: Alert detection within 60 seconds (2 evaluation cycles × 30s)
**Constraints**: Must use file-based provisioning; no Grafana API calls; no external notification channels
**Scale/Scope**: 4 alert rules in 1 rule group, changes to ~6 files across Docker Compose and K8s

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

This feature is **configuration-only** — it adds Grafana alert provisioning YAML files and updates deployment manifests. No application code (Java, domain logic, services) is modified. Therefore:

| Principle | Applicable? | Status |
|-----------|-------------|--------|
| I. Hexagonal Architecture | No — no application code | PASS (N/A) |
| II. Domain-Driven Design | No — no domain entities | PASS (N/A) |
| III. SOLID Principles | No — no application code | PASS (N/A) |
| IV. Test-Driven Development | No — configuration, not code | PASS (N/A) |
| V. Behavior-Driven Development | Acceptance scenarios defined in spec | PASS |
| VI. Code Quality Standards | No — YAML configuration only | PASS (N/A) |
| Layered Architecture Constraints | No — no Java packages | PASS (N/A) |
| Testing Standards | Manual + scripted verification | PASS |

**Post-Phase 1 Re-check**: Same result — this feature does not touch application code.

## Project Structure

### Documentation (this feature)

```text
specs/005-grafana-alert-rules/
├── spec.md                    # Feature specification
├── plan.md                    # This file
├── research.md                # Phase 0 research findings
├── data-model.md              # Alert rule data model
├── quickstart.md              # Quick start guide
├── contracts/
│   └── alert-rules.yaml       # Target alert rules YAML contract
├── checklists/
│   └── requirements.md        # Spec quality checklist
└── tasks.md                   # Phase 2 output (created by /speckit.tasks)
```

### Source Code (repository root)

```text
grafana/
├── provisioning/
│   ├── datasources/
│   │   └── datasource.yaml          # MODIFY: add uid: prometheus
│   ├── dashboards/
│   │   └── dashboard.yaml           # No change
│   └── alerting/                    # NEW directory
│       └── alert-rules.yaml         # NEW: 4 alert rules
└── dashboards/                      # No change
    ├── service-health.json
    ├── jvm-metrics.json
    └── kafka-metrics.json

docker-compose.yml                   # MODIFY: add alerting volume mount

apisix-k8s/grafana/
├── deployment.yaml                  # MODIFY: add alert-rules volume + mount
├── configmap-datasources.yaml       # MODIFY: add uid: prometheus
├── configmap-alert-rules.yaml       # NEW: ConfigMap for alert rules
├── configmap-dashboard-providers.yaml  # No change
├── configmap-dashboards.yaml        # No change
└── service.yaml                     # No change

scripts/
└── apisix-test.sh                   # MODIFY: add alert rule verification test
```

**Structure Decision**: Configuration-only changes. New files are the alert rules YAML and K8s ConfigMap. Modifications are limited to adding UID to datasource config, volume mounts, and test script updates.

## Complexity Tracking

No constitution violations — no complexity justification needed.

## Implementation Phases

### Phase 1: Datasource UID + Alert Rules YAML

1. Add `uid: prometheus` to `grafana/provisioning/datasources/datasource.yaml`
2. Update `apisix-k8s/grafana/configmap-datasources.yaml` with same UID
3. Create `grafana/provisioning/alerting/alert-rules.yaml` with all 4 rules (per contracts/alert-rules.yaml)

### Phase 2: Docker Compose Integration

4. Add `./grafana/provisioning/alerting:/etc/grafana/provisioning/alerting` volume mount to `docker-compose.yml` Grafana service

### Phase 3: K8s Integration

5. Create `apisix-k8s/grafana/configmap-alert-rules.yaml` — ConfigMap embedding the alert rules YAML
6. Update `apisix-k8s/grafana/deployment.yaml` — add volume + volumeMount for alert-rules ConfigMap

### Phase 4: Verification & Testing

7. Docker Compose: `docker compose up`, verify 4 alert rules in Grafana UI
8. Trigger error rate alert (notification-service failure simulation)
9. Trigger latency alert (payment-service delay simulation)
10. Verify alert auto-resolve when simulations are disabled
11. Verify no false positives during 5-minute normal operation

### Phase 5: Documentation Updates

12. Update `README.md` — add alert rules section
13. Update `apisix-k8s/README.md` — add alert rules ConfigMap to directory structure
