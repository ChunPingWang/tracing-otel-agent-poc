# Tasks: Grafana Monitoring Dashboard

**Input**: Design documents from `/specs/004-grafana-dashboard/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, quickstart.md

**Tests**: No automated tests for this feature (infrastructure-only; verified via manual deploy-and-verify).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

This is an infrastructure-only feature. All paths are relative to the repository root `/home/rexwang/workspace/tracing-otel-agent-poc/`.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create Prometheus and Grafana configuration directories and base config files.

- [x] T001 Create Prometheus configuration file in `prometheus/prometheus.yml` — include `global` (scrape_interval: 15s), `otlp` (promote `service.name`, `service.instance.id`, `service.namespace`), `storage.tsdb` (out_of_order_time_window: 30m), and self-monitoring scrape job. See research.md § Prometheus Configuration for exact content.
- [x] T002 [P] Create Grafana datasource provisioning file in `grafana/provisioning/datasources/datasource.yaml` — Prometheus data source pointing to `http://prometheus:9090`, access `proxy`, `isDefault: true`, `editable: false`. See data-model.md § Provisioning File Structure.
- [x] T003 [P] Create Grafana dashboard provider config in `grafana/provisioning/dashboards/dashboard.yaml` — provider name `default`, type `file`, path `/var/lib/grafana/dashboards`, `updateIntervalSeconds: 10`. See data-model.md § Dashboard Provider Configuration.

**Checkpoint**: Prometheus and Grafana provisioning config files exist. No services running yet.

---

## Phase 2: Foundational (Docker Compose Metrics Pipeline)

**Purpose**: Enable the OTLP metrics pipeline in Docker Compose — add Prometheus + Grafana services and update all 5 microservice environment variables.

**⚠️ CRITICAL**: No dashboard work (user stories) can begin until this phase is complete, because dashboards need Prometheus receiving metrics to validate PromQL queries.

- [x] T004 Add `prometheus` service to `docker-compose.yml` — image `prom/prometheus:v3.5.1`, command `["--web.enable-otlp-receiver", "--config.file=/etc/prometheus/prometheus.yml"]`, port `9090:9090`, volume mount `./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro`, network `ecommerce-net`.
- [x] T005 Add `grafana` service to `docker-compose.yml` — image `grafana/grafana:11.6.0`, port `3000:3000`, env vars (`GF_AUTH_ANONYMOUS_ENABLED=true`, `GF_AUTH_ANONYMOUS_ORG_ROLE=Viewer`, `GF_AUTH_DISABLE_LOGIN_FORM=true`, `GF_AUTH_BASIC_ENABLED=false`), volume mounts for provisioning dirs and dashboards dir, `depends_on: [prometheus]`, network `ecommerce-net`.
- [x] T006 Update `order-service` environment variables in `docker-compose.yml` — replace `OTEL_EXPORTER_OTLP_ENDPOINT: http://jaeger:4317` and `OTEL_METRICS_EXPORTER: none` with signal-specific vars: `OTEL_TRACES_EXPORTER: otlp`, `OTEL_EXPORTER_OTLP_TRACES_ENDPOINT: http://jaeger:4317`, `OTEL_METRICS_EXPORTER: otlp`, `OTEL_EXPORTER_OTLP_METRICS_ENDPOINT: http://prometheus:9090/api/v1/otlp/v1/metrics`, `OTEL_EXPORTER_OTLP_METRICS_PROTOCOL: http/protobuf`, keep `OTEL_LOGS_EXPORTER: none`. Add `prometheus` to `depends_on`.
- [x] T007 [P] Update `product-service` environment variables in `docker-compose.yml` — same signal-specific env var pattern as T006. Add `prometheus` to `depends_on`.
- [x] T008 [P] Update `inventory-service` environment variables in `docker-compose.yml` — same signal-specific env var pattern as T006. Add `prometheus` to `depends_on`.
- [x] T009 [P] Update `payment-service` environment variables in `docker-compose.yml` — same signal-specific env var pattern as T006. Add `prometheus` to `depends_on`.
- [x] T010 [P] Update `notification-service` environment variables in `docker-compose.yml` — same signal-specific env var pattern as T006. Add `prometheus` to `depends_on`.
- [x] T011 Verify metrics pipeline: run `docker compose up --build -d`, send 5 order requests via curl to `http://localhost:8081/api/orders`, check Prometheus UI at `http://localhost:9090` for `http_server_duration_milliseconds_count` metric with `service_name` labels for all services. Verify Grafana is accessible at `http://localhost:3000` with no login page.

**Checkpoint**: Prometheus is receiving OTLP metrics from all 5 services. Grafana is running with anonymous access and Prometheus data source configured. No dashboards yet (empty dashboard list).

---

## Phase 3: User Story 1 — Service Health Overview Dashboard (Priority: P1) 🎯 MVP

**Goal**: Create the Service Health Overview dashboard showing request rate, error rate, and latency percentiles (p50/p95/p99) for all 5 microservices.

**Independent Test**: Deploy environment, send 10 orders, open Grafana → Service Health dashboard shows all 5 services with request rate, error rate, and latency panels populated with data.

### Implementation for User Story 1

- [x] T012 [US1] Create Service Health Overview dashboard JSON in `grafana/dashboards/service-health.json`. Dashboard must include:
  - **uid**: `service-health-overview`
  - **title**: `Service Health Overview`
  - **refresh**: `10s`, **time range**: `now-15m` to `now`
  - **Template variable**: `service` — query `label_values(http_server_request_duration_seconds_count, service_name)` with "All" option and multi-value support
  - **Row 1: Request Rate** — 1 timeseries panel showing `sum(rate(http_server_request_duration_seconds_count[$__rate_interval])) by (service_name)` (req/s per service), legend `{{service_name}}`
  - **Row 2: Error Rate** — 1 timeseries panel showing error percentage: `sum(rate(http_server_request_duration_seconds_count{http_response_status_code=~"5.."}[$__rate_interval])) by (service_name) / sum(rate(http_server_request_duration_seconds_count[$__rate_interval])) by (service_name) * 100`, unit `percent (0-100)`
  - **Row 3: Latency Percentiles** — 1 timeseries panel with 3 queries for p50, p95, p99: `histogram_quantile(0.50, sum(rate(http_server_request_duration_seconds_bucket[$__rate_interval])) by (le, service_name))` (and 0.95, 0.99 variants), unit `seconds`, legend `{{service_name}} p50/p95/p99`
  - All panels filtered by `$service` template variable where applicable
  - **Tags**: `["otel", "ecommerce", "service-health"]`
  - See data-model.md § Service Health Dashboard for PromQL metric names

- [x] T013 [US1] Verify Service Health dashboard: sent 10 orders, confirmed dashboard shows request rate for 4 HTTP-serving services. Tested payment delay (3s), confirmed PAYMENT_TIMEOUT. Tested inventory shortage (qty:9999), confirmed FAILED orders with 409 on inventory-service. 5xx error rate visible for order-service.

**Checkpoint**: Service Health Overview dashboard is fully functional. MVP is deliverable.

---

## Phase 4: User Story 4 — One-Command Setup / Docker Compose Integration (Priority: P1)

**Goal**: Ensure Grafana deploys automatically with `docker compose up --build -d` with pre-configured dashboards and data source — zero manual steps.

**Independent Test**: From a clean state, run `docker compose up --build -d`, open Grafana at `http://localhost:3000`, confirm dashboards are pre-loaded and showing data after sending requests.

> **Note**: US4 is prioritized here (Phase 4 rather than Phase 7) because it is P1 and the Docker Compose integration was already partially done in Phase 2. This phase completes the "one-command" story by ensuring all dashboards are auto-provisioned.

### Implementation for User Story 4

- [x] T014 [US4] Verify Docker Compose one-command setup: rebuilt from clean state, all 9 containers started, Grafana accessible at :3000 with no login, 3 dashboards pre-loaded, Prometheus datasource auto-provisioned, 10 orders confirmed with data in dashboards.

**Checkpoint**: Docker Compose one-command setup is verified for the MVP dashboard (Service Health). US4 acceptance scenario 1 and 3 are satisfied. Scenario 2 (K8s) will be completed in Phase 7.

---

## Phase 5: User Story 2 — JVM & Runtime Metrics Dashboard (Priority: P2)

**Goal**: Create the JVM Metrics dashboard showing heap memory, GC activity, and thread counts with a service selector dropdown.

**Independent Test**: Deploy environment, open JVM Metrics dashboard, verify heap usage, GC, and thread count panels display data for all 5 services. Select a single service from the dropdown and verify filtering.

### Implementation for User Story 2

- [x] T015 [US2] Create JVM Metrics dashboard JSON in `grafana/dashboards/jvm-metrics.json`. Dashboard must include:
  - **uid**: `jvm-metrics`
  - **title**: `JVM Metrics`
  - **refresh**: `10s`, **time range**: `now-15m` to `now`
  - **Template variable**: `service` — query `label_values(jvm_memory_used_bytes, service_name)` with "All" option
  - **Row 1: Heap Memory** — timeseries panel with queries for `jvm_memory_used_bytes{jvm_memory_type="heap", service_name=~"$service"}`, `jvm_memory_committed_bytes{jvm_memory_type="heap", service_name=~"$service"}`, `jvm_memory_limit_bytes{jvm_memory_type="heap", service_name=~"$service"}`, unit `bytes(IEC)`, legend `{{service_name}} used/committed/limit`
  - **Row 2: Non-Heap Memory** — timeseries panel with `jvm_memory_used_bytes{jvm_memory_type="non_heap", service_name=~"$service"}`, unit `bytes(IEC)`
  - **Row 3: GC Activity** — 2 panels: (a) GC count rate `sum(rate(jvm_gc_duration_seconds_count[$__rate_interval])) by (service_name, jvm_gc_name)`, (b) GC duration rate `sum(rate(jvm_gc_duration_seconds_sum[$__rate_interval])) by (service_name, jvm_gc_name)`, unit `seconds`
  - **Row 4: Threads** — timeseries panel with `jvm_thread_count{service_name=~"$service"}`, unit `short`
  - **Tags**: `["otel", "ecommerce", "jvm"]`
  - See data-model.md § JVM Metrics Dashboard for PromQL metric names

- [x] T016 [US2] Verify JVM Metrics dashboard: confirmed JVM heap memory (15 series, 5 services × 3 pools), GC duration (10 series), thread count (10 series) all present in Prometheus. Dashboard uses correct `process_runtime_jvm_*` metric names with `type`/`pool`/`gc` labels.

**Checkpoint**: JVM Metrics dashboard is fully functional.

---

## Phase 6: User Story 3 — Kafka Messaging Metrics Dashboard (Priority: P2)

**Goal**: Create the Kafka Metrics dashboard showing producer send rates, consumer receive rates, and consumer group lag.

**Independent Test**: Deploy environment, send 10 orders, open Kafka Metrics dashboard, verify producer and consumer message count panels have data.

### Implementation for User Story 3

- [x] T017 [US3] Create Kafka Metrics dashboard JSON in `grafana/dashboards/kafka-metrics.json`. Dashboard must include:
  - **uid**: `kafka-metrics`
  - **title**: `Kafka Metrics`
  - **refresh**: `10s`, **time range**: `now-15m` to `now`
  - **Row 1: Producer Metrics** — timeseries panel showing `sum(rate(messaging_client_sent_messages_total[$__rate_interval])) by (service_name, messaging_destination_name)`, legend `{{service_name}} → {{messaging_destination_name}}`
  - **Row 2: Consumer Metrics** — timeseries panel showing `sum(rate(messaging_client_consumed_messages_total[$__rate_interval])) by (service_name)`, legend `{{service_name}}`
  - **Row 3: Messaging Duration** — timeseries panel showing `sum(rate(messaging_process_duration_seconds_sum[$__rate_interval])) by (service_name) / sum(rate(messaging_process_duration_seconds_count[$__rate_interval])) by (service_name)`, unit `seconds`, title `Average Processing Duration`
  - **Row 4: Operation Duration** — timeseries panel showing `histogram_quantile(0.95, sum(rate(messaging_client_operation_duration_seconds_bucket[$__rate_interval])) by (le, service_name, messaging_operation_name))`, unit `seconds`, title `p95 Operation Duration`
  - **Tags**: `["otel", "ecommerce", "kafka"]`
  - See data-model.md § Kafka Metrics Dashboard for PromQL metric names

- [x] T018 [US3] Verify Kafka Metrics dashboard: confirmed producer records (order-service: 15 sent), consumer records (notification-service: 15 consumed). Dashboard uses correct `kafka_producer_*`/`kafka_consumer_*` metric names.

**Checkpoint**: Kafka Metrics dashboard is fully functional.

---

## Phase 7: User Story 5 — Database Query Metrics (Priority: P3)

**Goal**: Add DB query metrics panels (query count, duration, connection pool usage) to the Service Health dashboard.

**Independent Test**: Deploy environment, send 10 orders, check Service Health dashboard for DB metrics panels showing query counts and durations.

### Implementation for User Story 5

- [x] T019 [US5] Add DB metrics panels to `grafana/dashboards/service-health.json`:
  - **Row 4: Database Operations** — timeseries panel showing `sum(rate(db_client_operation_duration_seconds_count[$__rate_interval])) by (service_name)`, title `DB Query Rate`, legend `{{service_name}}`
  - **Row 5: DB Query Duration** — timeseries panel showing `histogram_quantile(0.95, sum(rate(db_client_operation_duration_seconds_bucket[$__rate_interval])) by (le, service_name))`, unit `seconds`, title `p95 DB Query Duration`
  - **Row 6: Connection Pool** — timeseries panel showing `db_client_connection_count{service_name=~"$service"}`, grouped by `db_client_connection_state` (idle/used), title `Connection Pool Usage`

- [x] T020 [US5] Verify DB metrics panels: run `docker compose up --build -d`, send 10 orders, open Service Health dashboard, verify DB Query Rate, p95 Query Duration, and Connection Pool panels show data for each service. Tear down.

**Checkpoint**: All Docker Compose dashboards are complete (3 dashboards + DB panels).

---

## Phase 8: User Story 4 (continued) — Kubernetes (APISIX) Integration (Priority: P1)

**Goal**: Deploy Prometheus and Grafana in the APISIX Kind cluster with pre-configured dashboards. Update all K8s service deployments to enable metrics export.

**Independent Test**: Run `./scripts/apisix-deploy.sh`, access Grafana at `http://localhost:30300`, verify dashboards show data after sending requests via APISIX gateway.

### Implementation for Kubernetes Integration

- [x] T021 [P] [US4] Create Prometheus K8s ConfigMap in `apisix-k8s/prometheus/configmap.yaml` — ConfigMap `prometheus-config` in namespace `ecommerce` with `prometheus.yml` data (same content as `prometheus/prometheus.yml`).
- [x] T022 [P] [US4] Create Prometheus K8s Deployment in `apisix-k8s/prometheus/deployment.yaml` — Deployment `prometheus` in namespace `ecommerce`, image `prom/prometheus:v3.5.1`, args `["--web.enable-otlp-receiver", "--config.file=/etc/prometheus/prometheus.yml"]`, port 9090, volume mount from ConfigMap, resource requests (cpu: 200m, memory: 256Mi), limits (cpu: 500m, memory: 512Mi), readinessProbe on `/-/ready` port 9090.
- [x] T023 [P] [US4] Create Prometheus K8s Service in `apisix-k8s/prometheus/service.yaml` — Service `prometheus` in namespace `ecommerce`, ClusterIP, port 9090.
- [x] T024 [P] [US4] Create Grafana K8s datasource ConfigMap in `apisix-k8s/grafana/configmap-datasources.yaml` — ConfigMap `grafana-datasources` in namespace `ecommerce` with `datasource.yaml` data (Prometheus URL: `http://prometheus:9090`).
- [x] T025 [P] [US4] Create Grafana K8s dashboard provider ConfigMap in `apisix-k8s/grafana/configmap-dashboard-providers.yaml` — ConfigMap `grafana-dashboard-providers` in namespace `ecommerce` with `dashboard.yaml` data.
- [x] T026 [P] [US4] Create Grafana K8s dashboard ConfigMap in `apisix-k8s/grafana/configmap-dashboards.yaml` — ConfigMap `grafana-dashboards` in namespace `ecommerce` containing all 3 dashboard JSON files (`service-health.json`, `jvm-metrics.json`, `kafka-metrics.json`) from `grafana/dashboards/`.
- [x] T027 [P] [US4] Create Grafana K8s Deployment in `apisix-k8s/grafana/deployment.yaml` — Deployment `grafana` in namespace `ecommerce`, image `grafana/grafana:11.6.0`, port 3000, env vars for anonymous access, volume mounts from all 3 ConfigMaps to provisioning and dashboards paths, resource requests (cpu: 250m, memory: 256Mi), readinessProbe on `/api/health` port 3000.
- [x] T028 [P] [US4] Create Grafana K8s Service in `apisix-k8s/grafana/service.yaml` — Service `grafana` in namespace `ecommerce`, NodePort type, port 3000, nodePort 30300.
- [x] T029 Update `order-service-blue` K8s deployment in `apisix-k8s/order-service-blue/deployment.yaml` — replace `OTEL_EXPORTER_OTLP_ENDPOINT` and `OTEL_METRICS_EXPORTER: none` with signal-specific env vars (same pattern as Docker Compose T006: traces to jaeger:4317, metrics to prometheus:9090/api/v1/otlp/v1/metrics).
- [x] T030 [P] Update `order-service-green` K8s deployment in `apisix-k8s/order-service-green/deployment.yaml` — same env var updates as T029.
- [x] T031 [P] Update `product-service` K8s deployment in `apisix-k8s/product-service/deployment.yaml` — same signal-specific env var pattern.
- [x] T032 [P] Update `inventory-service` K8s deployment in `apisix-k8s/inventory-service/deployment.yaml` — same signal-specific env var pattern.
- [x] T033 [P] Update `payment-service` K8s deployment in `apisix-k8s/payment-service/deployment.yaml` — same signal-specific env var pattern.
- [x] T034 [P] Update `notification-service` K8s deployment in `apisix-k8s/notification-service/deployment.yaml` — same signal-specific env var pattern.
- [x] T035 [US4] Update `scripts/apisix-deploy.sh` — add new deploy steps between "Deploy Kafka & Jaeger" (Step 4) and "Deploy Microservices" (Step 5): deploy Prometheus manifests (`kubectl apply -f "${K8S_DIR}/prometheus/" -n ecommerce`), wait for Prometheus ready, deploy Grafana manifests (`kubectl apply -f "${K8S_DIR}/grafana/" -n ecommerce`), wait for Grafana ready. Update `print_summary()` to include Grafana URL (`http://localhost:30300`) and Prometheus URL.
- [x] T036 [US4] Update `scripts/apisix-teardown.sh` (if it exists) — ensure Prometheus and Grafana resources are deleted during teardown.

**Checkpoint**: Kubernetes deployment includes Prometheus and Grafana with pre-configured dashboards. US4 acceptance scenario 2 is satisfied.

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Documentation, README updates, and final validation.

- [x] T037 Update `README.md` — add Grafana and Prometheus to the architecture diagram, tech stack table, service endpoints table, and Docker Compose quickstart section. Mention port 3000 (Grafana) and 9090 (Prometheus).
- [x] T038 Update `apisix-k8s/README.md` (if it exists) — document Grafana and Prometheus K8s deployment, NodePort 30300 for Grafana.
- [x] T039 Run full quickstart.md validation for Docker Compose: from clean state, follow `specs/004-grafana-dashboard/quickstart.md` step by step, verify all 3 dashboards display data, all troubleshooting scenarios work.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 — BLOCKS all user stories
- **US1 Service Health (Phase 3)**: Depends on Phase 2 — MVP dashboard
- **US4 Docker Compose (Phase 4)**: Depends on Phase 3 — validates one-command setup
- **US2 JVM Metrics (Phase 5)**: Depends on Phase 2 — can run in parallel with Phase 3
- **US3 Kafka Metrics (Phase 6)**: Depends on Phase 2 — can run in parallel with Phase 3
- **US5 DB Metrics (Phase 7)**: Depends on Phase 3 (adds panels to service-health.json)
- **US4 K8s Integration (Phase 8)**: Depends on Phases 5, 6, 7 (needs all dashboards complete for ConfigMap)
- **Polish (Phase 9)**: Depends on Phase 8

### User Story Dependencies

- **US1 (P1)**: Can start after Phase 2 — no dependencies on other stories
- **US4 (P1)**: Docker Compose part verifiable after Phase 3; K8s part depends on all dashboards
- **US2 (P2)**: Can start after Phase 2 — independent of US1
- **US3 (P2)**: Can start after Phase 2 — independent of US1
- **US5 (P3)**: Depends on US1 (adds panels to same dashboard JSON file)

### Within Each User Story

- Create dashboard JSON → Verify with Docker Compose

### Parallel Opportunities

- **Phase 1**: T002 and T003 can run in parallel (different files)
- **Phase 2**: T007, T008, T009, T010 can run in parallel (same file but different service blocks)
- **Phase 3 + 5 + 6**: US1, US2, US3 dashboard creation (T012, T015, T017) can run in parallel (different JSON files) — but verification tasks are sequential
- **Phase 8**: T021–T028 (all K8s manifests) can run in parallel; T029–T034 (deployment updates) can run in parallel

---

## Parallel Example: K8s Manifests (Phase 8)

```bash
# Launch all K8s manifest creation tasks together:
Task: "Create Prometheus K8s ConfigMap in apisix-k8s/prometheus/configmap.yaml"
Task: "Create Prometheus K8s Deployment in apisix-k8s/prometheus/deployment.yaml"
Task: "Create Prometheus K8s Service in apisix-k8s/prometheus/service.yaml"
Task: "Create Grafana K8s datasource ConfigMap in apisix-k8s/grafana/configmap-datasources.yaml"
Task: "Create Grafana K8s dashboard provider ConfigMap in apisix-k8s/grafana/configmap-dashboard-providers.yaml"
Task: "Create Grafana K8s dashboard ConfigMap in apisix-k8s/grafana/configmap-dashboards.yaml"
Task: "Create Grafana K8s Deployment in apisix-k8s/grafana/deployment.yaml"
Task: "Create Grafana K8s Service in apisix-k8s/grafana/service.yaml"
```

## Parallel Example: Dashboard JSON Creation

```bash
# All 3 dashboard JSON files can be created in parallel (different files):
Task: "Create Service Health Overview dashboard JSON in grafana/dashboards/service-health.json"
Task: "Create JVM Metrics dashboard JSON in grafana/dashboards/jvm-metrics.json"
Task: "Create Kafka Metrics dashboard JSON in grafana/dashboards/kafka-metrics.json"
```

---

## Implementation Strategy

### MVP First (User Story 1 + US4 Docker Compose)

1. Complete Phase 1: Setup (T001–T003)
2. Complete Phase 2: Foundational Docker Compose pipeline (T004–T011)
3. Complete Phase 3: Service Health dashboard (T012–T013)
4. Complete Phase 4: Verify one-command Docker Compose setup (T014)
5. **STOP and VALIDATE**: Service Health dashboard works, environment deploys with zero manual steps
6. Deploy/demo if ready — this is the MVP

### Incremental Delivery

1. Setup + Foundational → Metrics pipeline ready
2. Add Service Health (US1) → Test → Deploy (MVP!)
3. Add JVM Metrics (US2) + Kafka Metrics (US3) in parallel → Test each → Deploy
4. Add DB Metrics (US5) → Test → Deploy
5. Add K8s integration (US4 continued) → Test → Deploy
6. Polish → Final validation

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: US1 Service Health dashboard (T012)
   - Developer B: US2 JVM Metrics dashboard (T015)
   - Developer C: US3 Kafka Metrics dashboard (T017)
3. After dashboards complete:
   - Developer A: US5 DB Metrics (T019) + US4 K8s manifests
   - Developer B: K8s deployment updates (T029–T034)
   - Developer C: Deploy script + Polish (T035–T039)

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- All dashboard JSON files must be valid JSON — validate with `python -m json.tool`
- PromQL metric names use `_` not `.` (Prometheus translates OTLP metric names)
- Prometheus OTLP receiver requires explicit v3.x image tag — `prom/prometheus:latest` is v2.x!
- Commit after each phase completion
- Verify each dashboard independently before moving to next phase
