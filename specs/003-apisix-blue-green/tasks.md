# Tasks: Apache APISIX Blue-Green Deployment

**Input**: Design documents from `/specs/003-apisix-blue-green/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/apisix-admin-api.md, quickstart.md

**Tests**: This feature is infrastructure-only (K8s manifests, Helm values, shell scripts). Validation is performed via shell-based integration tests in the test script (Phase 7). No unit tests.

**Organization**: Tasks are grouped by user story. Note that US5 (One-Command Setup) and US1 (Traffic Switching) are both P1 and tightly coupled — infrastructure manifests (Setup/Foundational) feed into both. US2 (Header Routing) and US3 (Rollback) extend the traffic-split configuration from US1. US4 (Tracing) is independent.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create the `apisix-k8s/` directory structure, Kind cluster config, and namespace definition.

- [x] T001 Create directory structure for `apisix-k8s/` per plan.md: `apisix-k8s/{jaeger,kafka,order-service-blue,order-service-green,product-service,inventory-service,payment-service,notification-service,apisix-config}/`
- [x] T002 [P] Create Kind cluster configuration in `apisix-k8s/kind-config.yaml` with extraPortMappings: 30080→9080 (Gateway), 30180→9180 (Admin API), 30686→16686 (Jaeger UI)
- [x] T003 [P] Create ecommerce namespace definition in `apisix-k8s/namespace.yaml`

---

## Phase 2: Foundational (Base Services — Kafka & Jaeger)

**Purpose**: K8s manifests for Kafka and Jaeger that ALL user stories depend on. These must be deployed before any microservice.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [x] T004 [P] Create Kafka Deployment in `apisix-k8s/kafka/deployment.yaml` — image `apache/kafka:3.7.0`, KRaft mode env vars matching docker-compose.yml (KAFKA_NODE_ID, KAFKA_PROCESS_ROLES, KAFKA_LISTENERS, etc.), single replica
- [x] T005 [P] Create Kafka Service in `apisix-k8s/kafka/service.yaml` — ClusterIP, port 9092, selector matching Kafka Deployment labels
- [x] T006 [P] Create Jaeger Deployment in `apisix-k8s/jaeger/deployment.yaml` — image `jaegertracing/all-in-one:1.53`, env `COLLECTOR_OTLP_ENABLED=true`, ports 16686/4317/4318
- [x] T007 [P] Create Jaeger Service in `apisix-k8s/jaeger/service.yaml` — ClusterIP for OTLP ports (4317, 4318) + NodePort 30686 for UI port 16686

**Checkpoint**: Kafka and Jaeger manifests ready — microservice deployment can proceed.

---

## Phase 3: User Story 1 — Blue-Green Traffic Switching (Priority: P1) 🎯 MVP

**Goal**: Deploy Blue (v1) and Green (v2) order-service instances behind APISIX, with weighted traffic splitting (100/0, 90/10, 50/50, 0/100).

**Independent Test**: Deploy both versions, configure 90/10 split via Admin API, send 100 requests to `http://localhost:9080/api/orders`, verify ~90% Blue / ~10% Green by checking Jaeger service names (`order-service-blue` vs `order-service-green`).

### Microservice K8s Manifests (shared by all stories)

- [x] T008 [P] [US1] Create product-service Deployment in `apisix-k8s/product-service/deployment.yaml` — image `product-service:latest`, port 8082, OTel env vars (JAVA_TOOL_OPTIONS, OTEL_SERVICE_NAME=product-service, OTEL_EXPORTER_OTLP_ENDPOINT=http://jaeger:4317), readiness probe on `/h2-console`
- [x] T009 [P] [US1] Create product-service Service in `apisix-k8s/product-service/service.yaml` — ClusterIP, port 8082
- [x] T010 [P] [US1] Create inventory-service Deployment in `apisix-k8s/inventory-service/deployment.yaml` — image `inventory-service:latest`, port 8083, OTel env vars, readiness probe
- [x] T011 [P] [US1] Create inventory-service Service in `apisix-k8s/inventory-service/service.yaml` — ClusterIP, port 8083
- [x] T012 [P] [US1] Create payment-service Deployment in `apisix-k8s/payment-service/deployment.yaml` — image `payment-service:latest`, port 8084, OTel env vars, readiness probe
- [x] T013 [P] [US1] Create payment-service Service in `apisix-k8s/payment-service/service.yaml` — ClusterIP, port 8084
- [x] T014 [P] [US1] Create notification-service Deployment in `apisix-k8s/notification-service/deployment.yaml` — image `notification-service:latest`, port 8085, OTel env vars, SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092, readiness probe
- [x] T015 [P] [US1] Create notification-service Service in `apisix-k8s/notification-service/service.yaml` — ClusterIP, port 8085

### Blue/Green Order Service Deployments

- [x] T016 [P] [US1] Create order-service-blue Deployment in `apisix-k8s/order-service-blue/deployment.yaml` — image `order-service:latest`, port 8081, labels `version: blue`, env: OTEL_SERVICE_NAME=order-service-blue, APP_VERSION=v1-blue, PRODUCT_SERVICE_URL=http://product-service:8082, INVENTORY_SERVICE_URL=http://inventory-service:8083, PAYMENT_SERVICE_URL=http://payment-service:8084, SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092, readiness probe
- [x] T017 [P] [US1] Create order-service-blue Service in `apisix-k8s/order-service-blue/service.yaml` — ClusterIP, port 8081, selector `version: blue`
- [x] T018 [P] [US1] Create order-service-green Deployment in `apisix-k8s/order-service-green/deployment.yaml` — identical to Blue except: labels `version: green`, OTEL_SERVICE_NAME=order-service-green, APP_VERSION=v2-green
- [x] T019 [P] [US1] Create order-service-green Service in `apisix-k8s/order-service-green/service.yaml` — ClusterIP, port 8081, selector `version: green`

### APISIX Helm Values & Config

- [x] T020 [US1] Create APISIX Helm values in `apisix-k8s/apisix-values.yaml` — image `apache/apisix:3.9.1-debian`, gateway NodePort 30080, admin NodePort 30180 with key `poc-admin-key-2024`, allow `0.0.0.0/0`, plugins list (traffic-split, proxy-rewrite, response-rewrite, opentelemetry), pluginAttrs.opentelemetry with collector `jaeger.ecommerce.svc.cluster.local:4318`, etcd 1 replica no persistence, dashboard disabled, ingressController disabled
- [x] T021 [P] [US1] Create Blue upstream JSON in `apisix-k8s/apisix-config/upstreams.json` — upstream id=1 (order-blue-v1) and upstream id=2 (order-green-v2) per contracts/apisix-admin-api.md, K8s DNS nodes, health checks, timeouts
- [x] T022 [P] [US1] Create order API route JSON in `apisix-k8s/apisix-config/route.json` — route id=1 with uri `/api/*`, upstream_id=1 (Blue default), traffic-split plugin with Rule 1 (X-Canary header match → Green 100%) and Rule 2 (weighted split, initial: Green 0 / Blue 100) per contracts/apisix-admin-api.md
- [x] T023 [P] [US1] Create admin endpoint routes in `apisix-k8s/apisix-config/route.json` — add upstream id=3 (payment-service:8084), upstream id=4 (notification-service:8085), route id=2 (payment-admin `/payment/*` with proxy-rewrite), route id=3 (notification-admin `/notification/*` with proxy-rewrite) per contracts/apisix-admin-api.md

### Traffic Control Script

- [x] T024 [US1] Create traffic control script `scripts/apisix-traffic.sh` — subcommands: `blue` (100/0), `canary` (90/10), `split` (50/50), `green` (0/100), `status` (GET route config and parse weights). Each subcommand sends PATCH to `http://localhost:9180/apisix/admin/routes/1` with traffic-split plugin JSON per contracts. Include Admin API reachability check, error handling, and color-coded output.

**Checkpoint**: Blue/Green order-service manifests + APISIX config + traffic script ready. With deploy script (US5), US1 acceptance scenarios can be validated.

---

## Phase 4: User Story 5 — One-Command Environment Setup (Priority: P1) 🎯 MVP

**Goal**: Single script deploys the entire environment: Kind cluster + APISIX + Blue/Green + all services + Jaeger. Single script tears it all down.

**Independent Test**: Run `scripts/apisix-deploy.sh` on a clean machine with prerequisites, verify all pods reach Ready within 6 minutes and `curl http://localhost:9080/api/orders` returns a valid response. Then run `scripts/apisix-teardown.sh` and verify cluster is removed.

- [x] T025 [US5] Create deployment script `scripts/apisix-deploy.sh` — prerequisite checks (docker, kind, kubectl, helm, jq), detect existing cluster `apisix-ecommerce`, create Kind cluster from `apisix-k8s/kind-config.yaml`, create `ecommerce` namespace, build 5 Docker images (order-service, product-service, inventory-service, payment-service, notification-service) and load into Kind, deploy Kafka + Jaeger manifests and wait for ready, deploy all microservice manifests and wait for ready, `helm repo add apisix` + `helm install` with `apisix-k8s/apisix-values.yaml`, wait for APISIX gateway + etcd ready, configure upstreams/routes/global-rules via Admin API curl using JSONs from `apisix-k8s/apisix-config/`, final verification (all pods ready, gateway responds, Jaeger UI accessible), print summary with endpoint URLs
- [x] T026 [US5] Create teardown script `scripts/apisix-teardown.sh` — delete Kind cluster `apisix-ecommerce`, verify removal, print confirmation message

**Checkpoint**: `apisix-deploy.sh` + `apisix-teardown.sh` ready. Combined with Phase 3, the full MVP (US1 + US5) is functional — deploy environment, switch traffic, verify distribution.

---

## Phase 5: User Story 2 — Header-Based Routing for Testing (Priority: P2)

**Goal**: QA engineers can send requests with `X-Canary: true` header to route directly to Green, while regular traffic follows weighted routing to Blue.

**Independent Test**: With 100% Blue configured, send a request with `-H "X-Canary: true"` and verify it reaches Green (check Jaeger for `order-service-green`). Send a request without the header and verify it reaches Blue.

- [x] T027 [US2] Add `header` subcommand to `scripts/apisix-traffic.sh` — enables combined configuration: Rule 1 (X-Canary header match → Green 100%) is always present in all traffic-split configurations. The `header` subcommand explicitly sets 100% Blue for non-header traffic + X-Canary → Green, clearly documenting header-based routing is active. Verify by sending a test request with header and confirming Green response.

**Checkpoint**: Header-based routing functional. QA can test Green version independently via `X-Canary: true` header while all other traffic goes to Blue.

---

## Phase 6: User Story 3 — Instant Rollback (Priority: P2)

**Goal**: Operator can immediately revert all traffic to Blue with a single command, regardless of current traffic configuration.

**Independent Test**: Configure 100% Green (`apisix-traffic.sh green`), then run `apisix-traffic.sh rollback`, send 10 requests and verify all go to Blue.

- [x] T028 [US3] Add `rollback` subcommand to `scripts/apisix-traffic.sh` — sends PATCH to set traffic-split weights to Green 0 / Blue 100 (same as `blue` subcommand but with explicit "ROLLBACK" messaging and confirmation output). Include validation that rollback was successful by querying route status after PATCH.

**Checkpoint**: Rollback functional. Operator can instantly revert from any traffic configuration (canary, 50/50, full Green) to 100% Blue.

---

## Phase 7: User Story 4 — Distributed Tracing Through Gateway (Priority: P3)

**Goal**: W3C Trace Context is preserved through APISIX gateway. Jaeger shows complete span chains including APISIX gateway span for all 5 test scenarios.

**Independent Test**: Send an order request through APISIX, open Jaeger UI, find the trace, and verify it includes an APISIX span plus all 5 downstream service spans.

- [x] T029 [US4] Create OpenTelemetry global rule JSON in `apisix-k8s/apisix-config/global-rules.json` — OpenTelemetry plugin with sampler `always_on` per contracts/apisix-admin-api.md contract #8
- [x] T030 [US4] Create test & verification script `scripts/apisix-test.sh` — subcommands: `all` (run all 5 test scenarios through gateway port 9080), `verify-distribution` (send 100 requests, count Blue vs Green by checking Jaeger traces for service name), `verify-traces` (send 1 order request, query Jaeger API for trace, verify APISIX span exists in the trace alongside order/product/inventory/payment/notification spans). Include: Scenario 1 Happy Path, Scenario 2 Inventory Shortage (P999/qty 999), Scenario 3 Payment Timeout (simulate-delay + order), Scenario 4 Kafka Async (same as S1, verify notification-service span), Scenario 5 Kafka DLT (simulate-failure + order + wait + disable). Color-coded pass/fail output per scenario.

**Checkpoint**: Full distributed tracing validated through APISIX gateway. All 5 test scenarios produce complete Jaeger traces.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Documentation, final validation, and cleanup.

- [x] T031 [P] Create feature README in `apisix-k8s/README.md` — architecture overview with ASCII/mermaid diagram showing APISIX → Blue/Green → downstream services → Jaeger, prerequisites table, quickstart section (deploy → traffic operations → test → teardown), traffic scenarios reference table (100/0, 90/10, 50/50, 0/100, header, rollback), APISIX Admin API reference with curl examples, troubleshooting guide (common issues: pods not ready, Admin API unreachable, port conflicts)
- [ ] T032 Validate quickstart.md against actual deployment — run through all steps in `specs/003-apisix-blue-green/quickstart.md` on the deployed environment, fix any discrepancies *(requires Docker/Kind runtime)*
- [ ] T033 End-to-end validation — run `scripts/apisix-deploy.sh`, execute `scripts/apisix-test.sh all`, run each traffic scenario (`blue`, `canary`, `split`, `green`, `header`, `rollback`), verify `scripts/apisix-test.sh verify-distribution` for canary (90/10), run `scripts/apisix-teardown.sh`, confirm all success criteria (SC-001 through SC-006) *(requires Docker/Kind runtime)*

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 (directory structure) — BLOCKS all user stories
- **US1 - Traffic Switching (Phase 3)**: Depends on Phase 2 (Kafka + Jaeger manifests)
- **US5 - One-Command Setup (Phase 4)**: Depends on Phase 3 (all manifests + APISIX config must exist)
- **US2 - Header Routing (Phase 5)**: Depends on Phase 4 (deploy script needed to test; traffic script from T024)
- **US3 - Rollback (Phase 6)**: Depends on Phase 4 (deploy script needed to test; traffic script from T024)
- **US4 - Tracing (Phase 7)**: Depends on Phase 4 (deploy script needed; OpenTelemetry config in T020/T029)
- **Polish (Phase 8)**: Depends on all user stories being complete

### User Story Dependencies

```
Phase 1 (Setup) → Phase 2 (Foundational)
                     ↓
                Phase 3 (US1: Manifests + APISIX Config + Traffic Script)
                     ↓
                Phase 4 (US5: Deploy/Teardown Scripts)
                     ↓
              ┌──────┼──────┐
              ↓      ↓      ↓
         Phase 5  Phase 6  Phase 7
         (US2)    (US3)    (US4)
              └──────┼──────┘
                     ↓
                Phase 8 (Polish)
```

### Within Each User Story

- K8s manifests (Deployment + Service pairs) can be created in parallel [P]
- APISIX config JSONs can be created in parallel [P]
- Scripts depend on manifests/config being created first
- Deploy script (T025) depends on ALL manifests and APISIX config

### Parallel Opportunities

**Phase 1** (3 tasks): T002 + T003 can run in parallel after T001.

**Phase 2** (4 tasks): T004 + T005 + T006 + T007 can ALL run in parallel.

**Phase 3** (17 tasks): T008-T019 (12 manifest tasks) can ALL run in parallel. T021 + T022 + T023 can run in parallel. T020 and T024 are sequential.

**Phases 5, 6, 7** can all proceed in parallel after Phase 4 completes.

---

## Parallel Example: Phase 3 (US1 Manifests)

```bash
# Launch ALL microservice manifests in parallel (12 tasks):
Task: "Create product-service Deployment in apisix-k8s/product-service/deployment.yaml"
Task: "Create product-service Service in apisix-k8s/product-service/service.yaml"
Task: "Create inventory-service Deployment in apisix-k8s/inventory-service/deployment.yaml"
Task: "Create inventory-service Service in apisix-k8s/inventory-service/service.yaml"
Task: "Create payment-service Deployment in apisix-k8s/payment-service/deployment.yaml"
Task: "Create payment-service Service in apisix-k8s/payment-service/service.yaml"
Task: "Create notification-service Deployment in apisix-k8s/notification-service/deployment.yaml"
Task: "Create notification-service Service in apisix-k8s/notification-service/service.yaml"
Task: "Create order-service-blue Deployment in apisix-k8s/order-service-blue/deployment.yaml"
Task: "Create order-service-blue Service in apisix-k8s/order-service-blue/service.yaml"
Task: "Create order-service-green Deployment in apisix-k8s/order-service-green/deployment.yaml"
Task: "Create order-service-green Service in apisix-k8s/order-service-green/service.yaml"

# Then APISIX config in parallel (3 tasks):
Task: "Create upstream JSON in apisix-k8s/apisix-config/upstreams.json"
Task: "Create route JSON in apisix-k8s/apisix-config/route.json"
Task: "Create admin endpoint routes in apisix-k8s/apisix-config/route.json"
```

---

## Implementation Strategy

### MVP First (US1 + US5)

1. Complete Phase 1: Setup (T001-T003)
2. Complete Phase 2: Foundational — Kafka + Jaeger manifests (T004-T007)
3. Complete Phase 3: US1 — All microservice manifests + APISIX config + traffic script (T008-T024)
4. Complete Phase 4: US5 — Deploy + teardown scripts (T025-T026)
5. **STOP and VALIDATE**: Run `apisix-deploy.sh`, test traffic switching with `apisix-traffic.sh canary`, verify distribution
6. Deploy/demo if ready — this is the MVP

### Incremental Delivery

1. Setup + Foundational → Infrastructure ready
2. Add US1 + US5 → Deploy, switch traffic, verify → **MVP!**
3. Add US2 → Header-based routing for QA testing
4. Add US3 → Instant rollback safety net
5. Add US4 → Full tracing through gateway + test automation
6. Polish → Documentation, end-to-end validation

---

## Summary

| Phase | Tasks | Parallel | Story |
|-------|-------|----------|-------|
| Phase 1: Setup | T001-T003 (3) | 2 | — |
| Phase 2: Foundational | T004-T007 (4) | 4 | — |
| Phase 3: US1 Traffic Switching | T008-T024 (17) | 15 | US1 |
| Phase 4: US5 One-Command Setup | T025-T026 (2) | 0 | US5 |
| Phase 5: US2 Header Routing | T027 (1) | 0 | US2 |
| Phase 6: US3 Rollback | T028 (1) | 0 | US3 |
| Phase 7: US4 Tracing | T029-T030 (2) | 0 | US4 |
| Phase 8: Polish | T031-T033 (3) | 1 | — |
| **Total** | **33 tasks** | **22 parallelizable** | **5 stories** |

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- No application Java code changes — all tasks create K8s manifests, Helm values, shell scripts, or JSON config
- Existing `docker-compose.yml`, `k8s/`, and application source code remain unchanged
- Docker images are built from existing Dockerfiles — no Dockerfile modifications needed
- All scripts should be `chmod +x` and have `#!/usr/bin/env bash` shebang
- T025 (deploy script) is the largest single task — consider breaking into helper functions within the script
- T030 (test script) requires `jq` and `curl` — deploy script should validate these as prerequisites
