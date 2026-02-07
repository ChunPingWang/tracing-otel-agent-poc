# Research: Apache APISIX Blue-Green Deployment

**Feature**: 003-apisix-blue-green
**Date**: 2026-02-07

---

## R-001: APISIX Helm Chart & Kind Deployment Strategy

**Decision**: Use official `apisix/apisix` Helm chart (v2.12.x, appVersion 3.9.1-debian) with NodePort + Kind extraPortMappings.

**Rationale**:
- The existing APISIX PoC at `../apisix/` already validates this exact approach with `apisix-values.yaml` and `kind-config.yaml`.
- NodePort + extraPortMappings provides persistent host access without running background port-forward processes.
- APISIX 3.9.1-debian is proven in the existing PoC and matches TECH.md's version spec.

**Alternatives Considered**:
- `kubectl port-forward`: Simpler but requires keeping processes alive; unsuitable for scripted test automation.
- APISIX Ingress Controller mode: Overkill for this PoC; Admin API is more direct for dynamic route configuration.
- APISIX standalone mode (no etcd): Would lose Admin API dynamic configuration capability, which is core to FR-003.

**Configuration**:
- Gateway: container port 9080 → NodePort 30080 → Kind hostPort 9080
- Admin API: port 9180 → NodePort 30180 → Kind hostPort 9180
- Jaeger UI: NodePort 30686 → Kind hostPort 16686
- etcd: single replica, no persistence (PoC)
- Admin API key: `poc-admin-key-2024` (reuse from existing PoC)

---

## R-002: Traffic-Split Plugin Configuration Pattern

**Decision**: Use APISIX `traffic-split` plugin with `upstream_id` references and multi-rule configuration for combined weighted + header-based routing.

**Rationale**:
- The `traffic-split` plugin natively supports both weighted upstreams and conditional match rules in a single configuration.
- Using `upstream_id` references (vs. inline upstream definitions) allows independent upstream health checking and cleaner Admin API operations.
- Multi-rule ordering: first rule matches `X-Canary: true` header → 100% Green; second rule (no match condition) applies weighted split. Rules are evaluated in order; first match wins.

**Alternatives Considered**:
- Separate routes for header-based and weighted routing: More complex to manage, duplicated route configuration.
- APISIX `proxy-rewrite` + multiple routes: Possible but traffic-split is purpose-built for this use case.

**Configuration Pattern** (combined header + weighted):
```json
{
  "traffic-split": {
    "rules": [
      {
        "match": [{"vars": [["http_X-Canary", "==", "true"]]}],
        "weighted_upstreams": [{"upstream_id": "<green-id>", "weight": 1}]
      },
      {
        "weighted_upstreams": [
          {"upstream_id": "<green-id>", "weight": <green-weight>},
          {"weight": <blue-weight>}
        ]
      }
    ]
  }
}
```

**Key Implementation Detail**: In the second rule (weighted split), the entry with only `weight` (no `upstream_id`) refers to the route's default upstream (Blue). This is the APISIX convention for referencing the default upstream.

---

## R-003: Green Version Identification Strategy

**Decision**: Use an environment variable `APP_VERSION=v2-green` injected into the Green order-service Deployment, exposed via a custom response header `X-App-Version` and included in the JSON response body.

**Rationale**:
- Zero application code changes: The order-service already returns `traceId` in responses. Adding a version field can be done via Spring Boot configuration or a simple response header filter.
- Actually, to maintain true zero-code-change principle, we will use APISIX's `response-rewrite` plugin to inject `X-App-Version: v2-green` header on responses from the Green upstream. This way the application code remains identical for Blue and Green.
- For response body identification, we use an environment variable `APP_VERSION` that the order-service reads and includes in its response (this requires a minor config change, not a code change — or we rely solely on the header).

**Final Approach**: Use APISIX `response-rewrite` plugin on the Green route to add `X-Served-By: green-v2` response header. No application code changes required. Traffic verification scripts count this header.

**Alternatives Considered**:
- Environment variable injected into application: Requires application code awareness of the version variable.
- Different container image with modified response: Violates assumption that both versions use the same Docker image.

---

## R-004: APISIX OpenTelemetry Integration for Trace Propagation

**Decision**: Enable APISIX `opentelemetry` plugin globally via `global_rules` Admin API, configured to send traces to Jaeger's OTLP HTTP endpoint (port 4318).

**Rationale**:
- Global rule ensures all requests passing through APISIX generate gateway spans without per-route plugin configuration.
- APISIX's opentelemetry plugin uses OTLP over HTTP (port 4318), and Jaeger all-in-one natively accepts OTLP HTTP.
- The plugin automatically participates in W3C Trace Context propagation — it reads incoming `traceparent` header, creates a child span, and forwards the updated `traceparent` to upstream services.
- `sampler: always_on` for PoC (100% sampling).

**Alternatives Considered**:
- Zipkin plugin: APISIX has a zipkin plugin, but OpenTelemetry is the modern standard and aligns with the PoC's OTel focus.
- Per-route plugin configuration: Works but global rule is cleaner for cross-cutting concerns.
- No gateway tracing (pass-through only): Would lose gateway span visibility in Jaeger; doesn't fulfill FR-006 / US-4.

**Helm Values Addition**:
```yaml
pluginAttrs:
  opentelemetry:
    resource:
      service.name: "APISIX"
    collector:
      address: "jaeger.ecommerce.svc.cluster.local:4318"
      request_timeout: 3
    batch_span_processor:
      max_queue_size: 1024
      batch_timeout: 2
```

---

## R-005: Kind Cluster Architecture for APISIX Deployment

**Decision**: Create a dedicated Kind cluster (`apisix-ecommerce`) separate from Feature 002's Kind cluster, with APISIX replacing NGINX Ingress Controller as the traffic entry point.

**Rationale**:
- TECH.md note #13 explicitly states: "Feature 002 and Feature 003 are independent deployment options, each with its own Kind cluster and deployment scripts."
- APISIX serves as both the API gateway and the external traffic entry point — no need for a separate Ingress Controller.
- All services (order-blue, order-green, product, inventory, payment, notification, kafka, jaeger) run in `ecommerce` namespace; APISIX and etcd run in `apisix` namespace (Helm default).

**Kind Port Mappings**:
| Host Port | Node Port | Service |
|-----------|-----------|---------|
| 9080 | 30080 | APISIX Gateway (HTTP) |
| 9180 | 30180 | APISIX Admin API |
| 16686 | 30686 | Jaeger UI |

**Alternatives Considered**:
- Reuse Feature 002's Kind cluster with APISIX added: More complex, potential port conflicts, couples two independent features.
- Deploy APISIX as a DaemonSet with hostPort: Overkill for single-node Kind.

---

## R-006: Blue/Green Order Service Deployment Strategy

**Decision**: Deploy two independent Kubernetes Deployments (`order-service-blue` and `order-service-green`) using the same Docker image, differentiated only by Kubernetes labels and environment variable `APP_VERSION`.

**Rationale**:
- Spec assumption: "The Green version is created by deploying the same order service Docker image with a different version identifier, not by modifying application code."
- Both Deployments point to the same image but have different:
  - Deployment name: `order-service-blue` / `order-service-green`
  - Service name: `order-service-blue` / `order-service-green`
  - Labels: `version: blue` / `version: green`
  - Environment variable: `APP_VERSION=v1-blue` / `APP_VERSION=v2-green`
  - OTel service name: `order-service-blue` / `order-service-green` (to distinguish in Jaeger)
- Both share the same downstream services (product, inventory, payment, notification, kafka).

**APISIX Upstream Configuration**:
- Upstream 1 (Blue): `order-service-blue.ecommerce.svc.cluster.local:8081`
- Upstream 2 (Green): `order-service-green.ecommerce.svc.cluster.local:8081`
- Route default upstream: Blue (Upstream 1)
- Traffic-split plugin references Green (Upstream 2)

**Alternatives Considered**:
- Single Deployment with multiple replicas labeled differently: Kubernetes doesn't support per-pod routing easily.
- Different Docker images: Violates spec assumption of same image.

---

## R-007: Convenience Scripts Architecture

**Decision**: Provide a single `apisix-traffic.sh` script with subcommands for all traffic operations, plus dedicated `apisix-deploy.sh` and `apisix-teardown.sh` lifecycle scripts.

**Rationale**:
- FR-010 requires convenience scripts for: canary (90/10), 50/50, cutover (0/100), rollback (100/0).
- A single script with subcommands is easier to discover and maintain than 4+ separate scripts.
- Deploy/teardown are distinct lifecycle operations warranting separate scripts.

**Script Inventory**:
| Script | Purpose |
|--------|---------|
| `scripts/apisix-deploy.sh` | One-command: create Kind cluster + install APISIX + deploy all services + configure routes |
| `scripts/apisix-teardown.sh` | Destroy Kind cluster and all resources |
| `scripts/apisix-traffic.sh blue` | 100/0 — all traffic to Blue |
| `scripts/apisix-traffic.sh canary` | 90/10 — canary split |
| `scripts/apisix-traffic.sh split` | 50/50 — equal split |
| `scripts/apisix-traffic.sh green` | 0/100 — full cutover to Green |
| `scripts/apisix-traffic.sh rollback` | Alias for `blue` (100/0) |
| `scripts/apisix-traffic.sh status` | Show current traffic configuration |
| `scripts/apisix-traffic.sh header` | Enable header-based routing (X-Canary: true → Green) |
| `scripts/apisix-test.sh` | Run all 5 test scenarios + verify traces in Jaeger |

---

## R-008: Directory Structure for APISIX Deployment Artifacts

**Decision**: All APISIX-related artifacts in `apisix-k8s/` directory at project root, separate from Feature 002's `k8s/` directory.

**Rationale**:
- FR-014 requires a dedicated directory at the project root.
- Spec assumption: "this feature adds a separate APISIX-focused deployment" — existing `k8s/` directory remains unchanged.
- TECH.md note #13 confirms independent deployment options.

**Directory Structure**:
```
apisix-k8s/
├── kind-config.yaml              # Kind cluster config (port mappings)
├── apisix-values.yaml            # APISIX Helm chart values
├── namespace.yaml                # ecommerce namespace
├── jaeger/
│   ├── deployment.yaml
│   └── service.yaml
├── kafka/
│   ├── deployment.yaml
│   └── service.yaml
├── order-service-blue/
│   ├── deployment.yaml
│   └── service.yaml
├── order-service-green/
│   ├── deployment.yaml
│   └── service.yaml
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
    ├── upstreams.json            # Blue + Green upstream definitions
    ├── route.json                # Main route with traffic-split
    └── global-rules.json         # OpenTelemetry global rule
```
