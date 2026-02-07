# Tasks: Enhance Existing Grafana Dashboards

**Input**: Design documents from `/specs/007-enhance-grafana-dashboards/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/new-panels.md

**Tests**: No automated tests requested. Verification is done via manual/scripted checks against Grafana UI and Prometheus PromQL queries.

**Organization**: Tasks are grouped by user story (dashboard). Each dashboard enhancement is independently testable. US1 (Service Health) is MVP since per-endpoint breakdown provides the highest operational value.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: User Story 1 — Service Health: Per-Endpoint Breakdown (Priority: P1) MVP

**Goal**: Add per-endpoint request rate and p95 latency panels to the Service Health Overview dashboard, using `http_route` and `http_method` labels.

**Independent Test**: Start Docker Compose, send 10 orders, open Service Health dashboard and verify at least 4 distinct endpoints appear with per-endpoint request rate and latency values.

### Implementation

- [x] T001 [US1] Add "Request Rate by Endpoint" timeseries panel to `grafana/dashboards/service-health.json` — position y=36, w=24, h=8, query: `sum(rate(http_server_duration_milliseconds_count{service_name=~"$service"}[$__rate_interval])) by (service_name, http_method, http_route)`, unit: reqps, legend: `{{service_name}} {{http_method}} {{http_route}}`
- [x] T002 [US1] Add "Latency by Endpoint (p95)" timeseries panel to `grafana/dashboards/service-health.json` — position y=45, w=24, h=8, query: `histogram_quantile(0.95, ...) / 1000` with endpoint breakdown, unit: seconds
- [x] T003 [US1] Verify Service Health enhancement: run `docker compose up -d`, send 10 orders, confirm the 2 new panels show at least 4 distinct endpoints (`POST /api/orders`, `GET /api/products/{productId}`, `POST /api/inventory/reserve`, `POST /api/payments`) with non-zero data
- [x] T004 [US1] Verify latency anomaly detection: enable payment delay simulation (5s), send 5 orders, confirm "Latency by Endpoint (p95)" shows `POST /api/payments` > 4s while others < 1s

**Checkpoint**: Service Health dashboard shows per-endpoint breakdown. Operators can identify slow endpoints.

---

## Phase 2: User Story 2 — JVM: Memory Pool Detail & Class Loading (Priority: P2)

**Goal**: Add per-memory-pool usage panel and class loading panel to the JVM Metrics dashboard.

**Independent Test**: Open JVM Metrics dashboard and verify memory pools (PS Eden Space, PS Old Gen, Metaspace, etc.) appear as separate series, and class loaded count is displayed.

### Implementation

- [x] T005 [US2] Add "Memory Pool Usage" timeseries panel to `grafana/dashboards/jvm-metrics.json` — position y=36, w=24, h=8, query: `process_runtime_jvm_memory_usage_bytes{service_name=~"$service"}`, legend: `{{service_name}} — {{pool}} ({{type}})`, unit: bytes
- [x] T006 [US2] Add "Class Loading" timeseries panel to `grafana/dashboards/jvm-metrics.json` — position y=45, w=12, h=8, two queries: (A) `process_runtime_jvm_classes_current_loaded{service_name=~"$service"}` and (B) `rate(process_runtime_jvm_classes_unloaded{service_name=~"$service"}[$__rate_interval])`
- [x] T007 [US2] Verify JVM enhancement: confirm "Memory Pool Usage" panel shows at least 3 distinct pools (PS Eden Space, PS Old Gen, Metaspace) per service, and "Class Loading" panel shows loaded class count

**Checkpoint**: JVM dashboard provides per-pool memory visibility and class loading tracking.

---

## Phase 3: User Story 3 — Kafka: DLT Monitoring & Per-Topic Lag (Priority: P2)

**Goal**: Add DLT message monitoring, per-topic consumer lag, and a service selector dropdown to the Kafka Metrics dashboard.

**Independent Test**: Enable notification-service failure simulation, send 5 orders, wait for retries to exhaust, verify DLT Messages panel shows activity and service selector filters correctly.

### Implementation

- [x] T008 [US3] Add `service` template variable to `grafana/dashboards/kafka-metrics.json` — query: `label_values(kafka_producer_record_send_total, service_name)`, multi-select: true, include All
- [x] T009 [US3] Add "DLT Messages" timeseries panel to `grafana/dashboards/kafka-metrics.json` — position y=27, x=0, w=12, h=8, query: `sum(rate(kafka_producer_record_send_total{topic=~".*\\.DLT"}[$__rate_interval])) by (service_name, topic)`, unit: ops
- [x] T010 [US3] Add "Consumer Lag by Topic" timeseries panel to `grafana/dashboards/kafka-metrics.json` — position y=27, x=12, w=12, h=8, query: `kafka_consumer_records_lag_max{service_name=~"$service"}`, legend: `{{service_name}} — {{topic}}`
- [x] T011 [US3] Update existing Kafka panels to respect the new `service` template variable filter (add `service_name=~"$service"` to existing queries where missing)
- [x] T012 [US3] Verify Kafka enhancement: enable failure simulation on notification-service, send 5 orders, wait ~45s for retries to exhaust, confirm DLT Messages panel shows `order-confirmed.DLT` send rate; verify service selector filters all panels

**Checkpoint**: Kafka dashboard has DLT visibility, per-topic lag, and service filtering.

---

## Phase 4: K8s Integration

**Purpose**: Regenerate the K8s ConfigMap to include all dashboard changes.

- [x] T013 Regenerate `apisix-k8s/grafana/configmap-dashboards.yaml` with all 3 updated dashboard JSON files embedded

**Checkpoint**: K8s manifests reflect all dashboard enhancements.

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Documentation updates, final validation, and regression check.

- [x] T014 [P] Update `README.md` — update Grafana dashboard descriptions to mention new panels (per-endpoint breakdown, memory pool, class loading, DLT monitoring)
- [x] T015 [P] Update `apisix-k8s/README.md` — update dashboard descriptions to mention enhanced panels
- [x] T016 Verify existing panels unchanged: confirm all original panels (Request Rate, Error Rate, Response Latency, DB Connection Pool, Heap Memory, GC, Thread Count, Producer/Consumer Rate, Consumer Lag, Producer Throughput) still display data correctly after enhancements
- [x] T017 Fresh deployment test: run `docker compose down -v && docker compose up -d`, send 10 orders, verify all new and existing panels load with data

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (US1 — Service Health)**: No dependencies — can start immediately. Modifies `service-health.json` only.
- **Phase 2 (US2 — JVM Metrics)**: No dependencies on US1 — can run in parallel. Modifies `jvm-metrics.json` only.
- **Phase 3 (US3 — Kafka Metrics)**: No dependencies on US1/US2 — can run in parallel. Modifies `kafka-metrics.json` only.
- **Phase 4 (K8s Integration)**: Depends on Phases 1-3 (needs all 3 updated JSON files).
- **Phase 5 (Polish)**: Depends on all verification being complete.

### User Story Dependencies

- **US1 (Service Health)**: Independent — modifies only `service-health.json`
- **US2 (JVM Metrics)**: Independent — modifies only `jvm-metrics.json`
- **US3 (Kafka Metrics)**: Independent — modifies only `kafka-metrics.json`

### Parallel Opportunities

- T001 and T002 are sequential (same file, T002 depends on T001's positioning)
- T005 and T006 are sequential (same file)
- T008, T009, T010 are sequential (same file, template variable must be added first)
- **US1, US2, US3 are fully parallel** — each modifies a different JSON file
- T014 and T015 can run in parallel (different README files)

---

## Implementation Strategy

### MVP First (US1 Only)

1. Complete Phase 1: Service Health per-endpoint panels
2. **STOP and VALIDATE**: Test per-endpoint breakdown with orders + payment delay
3. If ready, proceed to US2 and US3

### Incremental Delivery

1. US1 → Per-endpoint request rate + latency breakdown (highest operational value)
2. US2 → Memory pool + class loading (JVM deep dive)
3. US3 → DLT monitoring + service selector (Kafka deep dive)
4. K8s → Regenerate ConfigMap
5. Polish → Documentation + regression check

---

## Notes

- This is a **configuration-only** feature — no Java/application code changes.
- Each user story modifies a **different dashboard JSON file**, so all 3 are fully parallelizable.
- All new panels are **appended below existing panels** (higher `y` values) — existing panels are untouched.
- The `http_route` and `http_method` labels are already available on HTTP metrics (no OTel config change needed).
- JVM pool names depend on JDK and GC algorithm — JDK 8 Parallel GC uses `PS Eden Space`, `PS Survivor Space`, `PS Old Gen`.
- DLT topic naming follows Spring Kafka convention: `{original-topic}.DLT`.
- Per-partition consumer lag is NOT available by default from OTel Agent — using per-topic lag instead (equivalent for single-partition PoC).
- [P] tasks = different files, no dependencies
- Commit after each phase or logical group
