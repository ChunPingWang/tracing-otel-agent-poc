# Implementation Plan: Grafana Alert Contact Point & Notification Policy

**Branch**: `006-grafana-alert-notifications` | **Date**: 2026-02-07 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/006-grafana-alert-notifications/spec.md`

## Summary

Add a webhook contact point, notification policy with severity-based routing, and a lightweight webhook receiver service to the PoC. All components are provisioned automatically via Grafana's file-based provisioning (same mechanism as Feature 005 alert rules). This completes the alerting pipeline: alert rules (Feature 005) → notification policy → contact point → webhook receiver → visible in container logs.

## Technical Context

**Language/Version**: YAML configuration (Grafana provisioning format, apiVersion: 1)
**Primary Dependencies**: Grafana 11.6.0 (Unified Alerting), `mendhak/http-https-echo:39` (webhook receiver)
**Storage**: N/A (Grafana internal state for alert evaluation and notification delivery)
**Testing**: No automated tests. Manual/scripted verification via Grafana API and `docker compose logs`.
**Target Platform**: Docker Compose + Kind Kubernetes
**Project Type**: Infrastructure/configuration only — no Java code changes
**Performance Goals**: Notification delivery within 2 minutes of alert firing
**Constraints**: PoC environment, aggressive notification timings for quick feedback
**Scale/Scope**: 1 contact point, 1 notification policy tree (3 routes), 1 webhook receiver service

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**Result: PASS — N/A (infrastructure-only feature)**

The constitution principles (Hexagonal Architecture, DDD, SOLID, TDD, BDD, Code Quality) govern Java microservice code. This feature adds only YAML configuration files and a pre-built Docker image — no Java code is created or modified. All constitution gates are not applicable.

**Post-Phase 1 re-check: PASS** — No Java code, no architecture violations. The webhook receiver is a pre-built third-party Docker image, not custom application code.

## Project Structure

### Documentation (this feature)

```text
specs/006-grafana-alert-notifications/
├── plan.md                              # This file
├── spec.md                              # Feature specification
├── research.md                          # Phase 0: research findings (7 decisions)
├── data-model.md                        # Phase 1: entity definitions
├── quickstart.md                        # Phase 1: verification commands
├── contracts/
│   ├── contact-points.yaml              # Phase 1: Grafana contact point YAML
│   └── notification-policies.yaml       # Phase 1: Grafana notification policy YAML
├── checklists/
│   └── requirements.md                  # Spec quality checklist
└── tasks.md                             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
grafana/provisioning/alerting/
├── alert-rules.yaml                     # Existing (Feature 005) — 4 alert rules
├── contact-points.yaml                  # NEW — webhook contact point definition
└── notification-policies.yaml           # NEW — severity-based routing policy

docker-compose.yml                       # MODIFY — add webhook-sink service

apisix-k8s/grafana/
├── configmap-alert-rules.yaml           # Existing (Feature 005)
├── configmap-contact-points.yaml        # NEW — K8s ConfigMap for contact points
├── configmap-notification-policies.yaml # NEW — K8s ConfigMap for notification policies
├── deployment.yaml                      # MODIFY — add volumeMounts for new ConfigMaps
└── ...                                  # Existing files unchanged
```

**Structure Decision**: Infrastructure/configuration only. New YAML files are added to the existing `grafana/provisioning/alerting/` directory (Grafana reads all files in this directory). Docker Compose gets a new service (`webhook-sink`). K8s gets new ConfigMaps and volumeMount entries.

## Implementation Steps

### Phase 1: Webhook Receiver Service

1. Add `webhook-sink` service to `docker-compose.yml` using `mendhak/http-https-echo:39` image, port 8090:8080, on `ecommerce-net` network.

### Phase 2: Contact Point Provisioning

2. Create `grafana/provisioning/alerting/contact-points.yaml` with a webhook contact point named `ecommerce-webhook` pointing to `http://webhook-sink:8080/grafana-alerts`.

### Phase 3: Notification Policy Provisioning

3. Create `grafana/provisioning/alerting/notification-policies.yaml` with severity-based routing:
   - Default route → `ecommerce-webhook` (group_wait: 10s, repeat: 5m)
   - Critical route → `ecommerce-webhook` (group_wait: 5s, repeat: 1m)
   - Warning route → `ecommerce-webhook` (group_wait: 10s, repeat: 5m)

### Phase 4: Kubernetes Integration

4. Create `apisix-k8s/grafana/configmap-contact-points.yaml` embedding `contact-points.yaml`.
5. Create `apisix-k8s/grafana/configmap-notification-policies.yaml` embedding `notification-policies.yaml`.
6. Update `apisix-k8s/grafana/deployment.yaml` — add new volumes and volumeMounts.
7. Add `webhook-sink` deployment and service to `apisix-k8s/` for K8s deployment.

### Phase 5: Verification

8. Docker Compose: `docker compose up -d`, verify contact point and notification policy via Grafana API.
9. Trigger high error rate alert, verify webhook receiver logs notification payload.
10. Verify resolved notification when alert clears.
11. Verify no false notifications during 5-minute normal operation.
12. Fresh deployment test: `docker compose down -v && docker compose up -d`.

### Phase 6: Documentation

13. Update `README.md` — add webhook receiver to tech stack, service list, and quickstart.
14. Update `apisix-k8s/README.md` — add new ConfigMaps to directory structure.

## Key Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Webhook receiver image | `mendhak/http-https-echo:39` | Pre-built, logs POST body to stdout, ~25MB, zero config |
| File organization | Separate YAML files per resource type | Follows SoC, matches Grafana UI navigation |
| Notification timing | Aggressive (5s-10s group_wait, 1m-5m repeat) | PoC needs fast feedback for demo |
| Single contact point | One webhook for both critical and warning | Sufficient for PoC; routing differentiation via repeat_interval |
| Docker Compose volume | No change needed | Existing directory mount covers new files |
