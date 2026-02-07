# Implementation Plan: Apache APISIX Blue-Green Deployment

**Branch**: `003-apisix-blue-green` | **Date**: 2026-02-07 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/003-apisix-blue-green/spec.md`

## Summary

Deploy Apache APISIX as an API Gateway on a Kind Kubernetes cluster to enable blue-green deployment of the order service. Two identical order-service instances (Blue v1 / Green v2) run behind APISIX, which provides weighted traffic splitting via the `traffic-split` plugin, header-based routing (`X-Canary: true`) for QA testing, and instant rollback. The APISIX `opentelemetry` plugin preserves W3C Trace Context through the gateway for end-to-end Jaeger traces. All infrastructure is automated via deploy/teardown scripts and traffic convenience scripts.

## Technical Context

**Language/Version**: Bash (scripts), YAML (K8s manifests, Helm values), JSON (APISIX Admin API payloads). No Java code changes.
**Primary Dependencies**: Apache APISIX 3.9.1-debian (Helm chart), etcd (bundled), Kind v0.20+, kubectl v1.28+, Helm 3.14+
**Storage**: etcd (APISIX config store, single replica, no persistence). Application services use H2 in-memory (unchanged).
**Testing**: Shell-based integration tests (curl + jq), traffic distribution verification scripts
**Target Platform**: Kind (Kubernetes in Docker) on Linux/macOS
**Project Type**: Infrastructure/deployment — Kubernetes manifests, Helm values, shell scripts
**Performance Goals**: SC-005: Environment setup ≤ 6 minutes. SC-003: Traffic cutover ≤ 5 seconds.
**Constraints**: Zero application code changes. Same Docker images for Blue and Green. Independent from Feature 002's Kind cluster.
**Scale/Scope**: 8 Kubernetes Deployments (order-blue, order-green, product, inventory, payment, notification, kafka, jaeger) + APISIX (Helm) + etcd (Helm). ~15 K8s manifest files, ~5 scripts, ~3 APISIX config JSONs.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Applicable? | Status | Notes |
|-----------|-------------|--------|-------|
| I. Hexagonal Architecture | N/A | PASS | No application code changes in this feature |
| II. Domain-Driven Design | N/A | PASS | No domain model changes |
| III. SOLID Principles | N/A | PASS | No application code changes |
| IV. Test-Driven Development | Partial | PASS | Infrastructure tests use shell scripts, not unit tests. TDD applies to application code only. |
| V. Behavior-Driven Development | PASS | PASS | Spec acceptance scenarios follow Given-When-Then format |
| VI. Code Quality Standards | Partial | PASS | Shell scripts follow standard conventions. No Java code to check. |
| Layered Architecture | N/A | PASS | No layer violations — no application code changes |
| Testing Standards | Partial | PASS | Integration tests validate all 5 scenarios + traffic distribution. No unit tests needed for infrastructure-only feature. |

**Gate Result**: PASS — This feature is infrastructure-only (K8s manifests, Helm config, shell scripts). Constitution principles for application code (Hexagonal, DDD, SOLID, TDD) are not applicable. No violations.

## Project Structure

### Documentation (this feature)

```text
specs/003-apisix-blue-green/
├── plan.md              # This file
├── research.md          # Phase 0: Technology research decisions
├── data-model.md        # Phase 1: APISIX entities and K8s deployments
├── quickstart.md        # Phase 1: User quickstart guide
├── contracts/
│   └── apisix-admin-api.md  # Phase 1: APISIX Admin API contracts
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
apisix-k8s/
├── kind-config.yaml                    # Kind cluster config (port mappings: 9080, 9180, 16686)
├── apisix-values.yaml                  # APISIX Helm chart values
├── namespace.yaml                      # ecommerce namespace definition
├── jaeger/
│   ├── deployment.yaml                 # Jaeger all-in-one with OTLP
│   └── service.yaml                    # Jaeger Service (16686, 4317, 4318) + NodePort 30686
├── kafka/
│   ├── deployment.yaml                 # Kafka 3.7.0 KRaft mode
│   └── service.yaml                    # Kafka Service (9092)
├── order-service-blue/
│   ├── deployment.yaml                 # Order v1 (OTEL_SERVICE_NAME=order-service-blue)
│   └── service.yaml                    # order-service-blue:8081
├── order-service-green/
│   ├── deployment.yaml                 # Order v2 (OTEL_SERVICE_NAME=order-service-green)
│   └── service.yaml                    # order-service-green:8081
├── product-service/
│   ├── deployment.yaml
│   └── service.yaml
├── inventory-service/
│   ├── deployment.yaml
│   └── service.yaml
├── payment-service/
│   ├── deployment.yaml
│   └── service.yaml
├── notification-service/
│   ├── deployment.yaml
│   └── service.yaml
└── apisix-config/
    ├── upstreams.json                  # Blue + Green upstream definitions
    ├── route.json                      # Main route with traffic-split plugin
    └── global-rules.json               # OpenTelemetry global rule

scripts/
├── apisix-deploy.sh                    # One-command deployment
├── apisix-teardown.sh                  # One-command teardown
├── apisix-traffic.sh                   # Traffic switching (blue/canary/split/green/rollback/status)
└── apisix-test.sh                      # Test all 5 scenarios + verify traffic distribution
```

**Structure Decision**: Infrastructure-only feature. All K8s manifests in `apisix-k8s/` directory (FR-014). All scripts in existing `scripts/` directory. No changes to application source code or existing `docker-compose.yml` / `k8s/` directories.

## Complexity Tracking

> No constitution violations to justify. This feature is infrastructure-only.

N/A

## Implementation Phases

### Phase 1: Kind Cluster & Base Infrastructure

**Goal**: Create the Kind cluster foundation with Kafka and Jaeger.

1. Create `apisix-k8s/kind-config.yaml` with extraPortMappings:
   - 30080 → 9080 (APISIX Gateway)
   - 30180 → 9180 (APISIX Admin API)
   - 30686 → 16686 (Jaeger UI)

2. Create `apisix-k8s/namespace.yaml` for `ecommerce` namespace.

3. Create Kafka K8s manifests (`apisix-k8s/kafka/`):
   - Deployment: `apache/kafka:3.7.0`, KRaft mode, same env vars as docker-compose
   - Service: ClusterIP on port 9092

4. Create Jaeger K8s manifests (`apisix-k8s/jaeger/`):
   - Deployment: `jaegertracing/all-in-one:1.53`, OTLP enabled
   - Service: ClusterIP (4317, 4318) + NodePort 30686 for UI (16686)

### Phase 2: Microservice Deployments

**Goal**: Deploy all 5 microservices (order-blue, order-green, product, inventory, payment, notification).

1. Create shared K8s manifests for product, inventory, payment, notification services:
   - Deployments with OTel Agent env vars (JAVA_TOOL_OPTIONS, OTEL_SERVICE_NAME, OTEL_EXPORTER_OTLP_ENDPOINT=http://jaeger:4317)
   - Services on respective ports (8082, 8083, 8084, 8085)
   - Readiness probes (FR-013)

2. Create order-service-blue manifests:
   - Deployment: same image as order-service, labels `version: blue`
   - Env: `OTEL_SERVICE_NAME=order-service-blue`, `APP_VERSION=v1-blue`
   - Service URLs pointing to K8s DNS (product-service:8082, etc.)
   - Service: `order-service-blue:8081`

3. Create order-service-green manifests:
   - Deployment: same image, labels `version: green`
   - Env: `OTEL_SERVICE_NAME=order-service-green`, `APP_VERSION=v2-green`
   - Service: `order-service-green:8081`

### Phase 3: APISIX Gateway Installation

**Goal**: Install APISIX via Helm and configure routes/upstreams.

1. Create `apisix-k8s/apisix-values.yaml`:
   - Image: `apache/apisix:3.9.1-debian`
   - Gateway: NodePort 30080
   - Admin API: NodePort 30180, key `poc-admin-key-2024`, allow `0.0.0.0/0`
   - Plugins: `traffic-split`, `proxy-rewrite`, `response-rewrite`, `opentelemetry`
   - pluginAttrs.opentelemetry: collector at `jaeger.ecommerce.svc.cluster.local:4318`
   - etcd: 1 replica, no persistence

2. Create APISIX config JSONs (`apisix-k8s/apisix-config/`):
   - `upstreams.json`: Blue (id=1) and Green (id=2) upstream definitions
   - `route.json`: Order API route with traffic-split plugin (initial: 100% Blue + X-Canary header rule)
   - `global-rules.json`: OpenTelemetry global rule (sampler: always_on)
   - Additional routes for payment/notification admin endpoints via proxy-rewrite

### Phase 4: Deployment Script

**Goal**: Create the one-command deployment script (FR-007).

1. `scripts/apisix-deploy.sh`:
   - Prerequisite checks (docker, kind, kubectl, helm)
   - Check for existing cluster (detect and prompt)
   - Create Kind cluster from `apisix-k8s/kind-config.yaml`
   - Create `ecommerce` namespace
   - Build and load Docker images into Kind (5 service images)
   - Deploy Kafka and Jaeger manifests, wait for ready
   - Deploy all microservice manifests, wait for ready
   - Install APISIX via Helm, wait for gateway ready
   - Configure APISIX routes/upstreams via Admin API (curl)
   - Configure OpenTelemetry global rule
   - Deployment verification: all pods ready, gateway responds, Jaeger UI accessible

### Phase 5: Traffic Control & Teardown Scripts

**Goal**: Create traffic switching and teardown scripts (FR-008, FR-010).

1. `scripts/apisix-traffic.sh`:
   - Subcommands: `blue`, `canary`, `split`, `green`, `rollback`, `status`, `header`
   - Each subcommand sends PATCH to Admin API with appropriate traffic-split config
   - `status` reads current route config and displays weights
   - Validates Admin API is reachable before making changes

2. `scripts/apisix-teardown.sh`:
   - Delete Kind cluster `apisix-ecommerce`
   - Verify cluster is removed

### Phase 6: Test & Verification Script

**Goal**: Create test automation for all 5 scenarios and traffic distribution verification.

1. `scripts/apisix-test.sh`:
   - Run all 5 test scenarios through APISIX gateway (port 9080)
   - `verify-distribution` subcommand: send 100 requests, count Blue vs Green responses by checking `X-App-Version` header or OTel service name in response
   - Verify Jaeger traces contain gateway span (APISIX service name)
   - Color-coded pass/fail output

### Phase 7: Documentation & README

**Goal**: Document the APISIX blue-green deployment feature.

1. Create `apisix-k8s/README.md`:
   - Architecture overview with diagram
   - Prerequisites
   - Quick start (deploy → traffic operations → test → teardown)
   - Traffic scenarios reference table
   - APISIX Admin API reference
   - Troubleshooting guide

## Key Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Gateway entry point | APISIX replaces Ingress Controller | APISIX serves as both gateway and external entry point; no need for NGINX Ingress |
| Green identification | APISIX response-rewrite plugin + different OTEL_SERVICE_NAME | Zero application code changes; verifiable via response header and Jaeger service name |
| Kind networking | NodePort + extraPortMappings | Persistent access without port-forward processes; matches existing APISIX PoC pattern |
| APISIX config method | Admin API (not standalone YAML) | Dynamic configuration is core requirement (FR-003); Admin API enables runtime traffic switching |
| Cluster isolation | Separate Kind cluster from Feature 002 | Independent deployment options per TECH.md #13; avoids port conflicts and coupling |
| Script architecture | Single `apisix-traffic.sh` with subcommands | Easier discoverability than 4+ separate scripts; aligns with FR-010 |
| OpenTelemetry config | Global rule + Helm pluginAttrs | Global rule applies to all routes; Helm config sets collector endpoint at install time |

## Risk Mitigation

| Risk | Mitigation |
|------|------------|
| APISIX Helm chart changes may break values | Pin Helm chart version in deploy script |
| Docker image load into Kind is slow for 5 services | Build images in parallel where possible |
| etcd startup may be slow | Deploy etcd/APISIX first, microservices in parallel after |
| APISIX Admin API not ready when scripts configure routes | Add retry loop with health check before Admin API calls |
| Kind extraPortMappings require cluster recreation | Deploy script checks for existing cluster and handles accordingly |
