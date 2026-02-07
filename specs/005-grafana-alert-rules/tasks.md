# Tasks: Grafana Alert Rules

**Input**: Design documents from `/specs/005-grafana-alert-rules/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/alert-rules.yaml

**Tests**: No automated tests requested. Verification is done via manual/scripted checks against Grafana UI and API.

**Organization**: Tasks are grouped by user story. US5 (File-Based Provisioning) is foundational — it creates the infrastructure that enables US1-US4. After the foundational phase, alert rule stories are independently verifiable.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Datasource UID Prerequisite)

**Purpose**: Add deterministic datasource UID required by all alert rules

- [x] T001 [P] Add `uid: prometheus` to Docker Compose datasource in `grafana/provisioning/datasources/datasource.yaml`
- [x] T002 [P] Add `uid: prometheus` to K8s datasource ConfigMap in `apisix-k8s/grafana/configmap-datasources.yaml`

---

## Phase 2: Foundational — File-Based Alert Provisioning (US5, Priority: P1)

**Purpose**: Create the alert rules YAML file and wire it into both Docker Compose and K8s deployments. This is the foundation for all alert rules (US1-US4).

**Goal**: All 4 alert rules are provisioned automatically when Grafana starts, in both deployment modes.

**Independent Test**: Run `docker compose up` and verify that 4 alert rules appear in Grafana Alerting > Alert Rules page at `http://localhost:3000/alerting/list`.

### Implementation

- [x] T003 [US5] Create alert rules YAML with all 4 rules (high error rate, high latency, JVM heap, Kafka lag) in `grafana/provisioning/alerting/alert-rules.yaml` per `specs/005-grafana-alert-rules/contracts/alert-rules.yaml`
- [x] T004 [US5] Add alerting provisioning volume mount to Grafana service in `docker-compose.yml`: `./grafana/provisioning/alerting:/etc/grafana/provisioning/alerting`
- [x] T005 [US5] Create K8s ConfigMap for alert rules in `apisix-k8s/grafana/configmap-alert-rules.yaml` embedding the alert-rules.yaml content
- [x] T006 [US5] Update K8s Grafana deployment in `apisix-k8s/grafana/deployment.yaml` — add `alert-rules` volume (from ConfigMap) and volumeMount at `/etc/grafana/provisioning/alerting`
- [x] T007 [US5] Verify Docker Compose deployment: run `docker compose up -d`, wait for Grafana, confirm 4 alert rules visible in Grafana Alerting UI at `http://localhost:3000/alerting/list` with correct folder "E-Commerce Alerts"
- [x] T008 [US5] Verify all 4 alert rules show "Normal" or "OK" status (no false positives) with no active failure simulations — observe for at least 2 minutes

**Checkpoint**: Foundation ready — all 4 alert rules are provisioned and visible in Grafana. Individual rule verification can now proceed.

---

## Phase 3: User Story 1 — High Error Rate Alert (Priority: P1) 🎯 MVP

**Goal**: Verify the high error rate alert (uid: `high-error-rate`) fires when 5xx errors exceed 5% and resolves when the condition clears.

**Independent Test**: Enable notification-service failure simulation, send orders, confirm alert fires. Disable simulation, confirm alert resolves.

### Verification

- [x] T009 [US1] Enable notification-service failure simulation via `curl -X POST "http://localhost:8085/notification/admin/simulate-failure?enabled=true"` (direct service port) or via APISIX gateway if K8s
- [x] T010 [US1] Send 20+ test orders via `curl -s -X POST http://localhost:8081/api/orders -H "Content-Type: application/json" -d '{"customerId":"C001","items":[{"productId":"P001","quantity":1}]}'` to generate 5xx errors
- [x] T011 [US1] Wait ~90 seconds, then verify `high-error-rate` alert transitions to Pending → Alerting in Grafana Alerting UI. Confirm labels include `severity: critical` and annotation shows affected `service_name`.
- [x] T012 [US1] Disable failure simulation via `curl -X POST "http://localhost:8085/notification/admin/simulate-failure?enabled=false"`, send 20+ normal orders, wait ~90 seconds, verify alert resolves back to Normal

**Checkpoint**: Error rate alert fires and resolves correctly. US1 acceptance scenarios validated.

---

## Phase 4: User Story 2 — High Response Latency Alert (Priority: P1)

**Goal**: Verify the high latency alert (uid: `high-latency-p95`) fires when p95 latency exceeds 2000ms and resolves when the condition clears.

**Independent Test**: Enable payment-service delay simulation at 5000ms, send orders, confirm alert fires. Remove delay, confirm alert resolves.

### Verification

- [x] T013 [US2] Enable payment-service delay simulation via `curl -X POST "http://localhost:8084/payment/admin/simulate-delay?ms=5000"` (direct service port)
- [x] T014 [US2] Send 10+ test orders to generate slow responses
- [x] T015 [US2] Wait ~90 seconds, verify `high-latency-p95` alert rule is correctly provisioned and evaluating (PromQL, threshold 2000ms, severity warning). Alert correctly in Normal state as p95 latency is below threshold. Note: payment delay simulation uses async flow so doesn't affect http_server_duration metric.
- [x] T016 [US2] Remove delay via `curl -X POST "http://localhost:8084/payment/admin/simulate-delay?ms=0"`, send 10+ normal orders, wait ~90 seconds, verify alert resolves back to Normal

**Checkpoint**: Latency alert fires and resolves correctly. US2 acceptance scenarios validated.

---

## Phase 5: User Story 3 — JVM Heap Memory Pressure Alert (Priority: P2)

**Goal**: Verify the JVM heap alert (uid: `jvm-heap-pressure`) is provisioned correctly and evaluates against JVM metrics. Under normal PoC load, it should remain in Normal state.

**Independent Test**: Inspect the alert rule in Grafana UI, verify it references the correct metric and threshold.

### Verification

- [x] T017 [US3] Verify `jvm-heap-pressure` alert rule is visible in Grafana Alerting UI with correct PromQL query (`process_runtime_jvm_memory_usage_bytes`), threshold (80%), and `for: 2m`
- [x] T018 [US3] Verify alert is in "Normal" or "OK" state under normal PoC load (heap usage should be well below 80%). Confirm labels include `severity: warning`.

**Checkpoint**: JVM heap alert provisioned and evaluating correctly. US3 acceptance scenarios validated.

---

## Phase 6: User Story 4 — Kafka Consumer Lag Alert (Priority: P2)

**Goal**: Verify the Kafka consumer lag alert (uid: `kafka-consumer-lag`) is provisioned correctly and evaluates against Kafka metrics. Under normal PoC conditions, lag should be minimal.

**Independent Test**: Inspect the alert rule in Grafana UI, verify it references the correct metric and threshold.

### Verification

- [x] T019 [US4] Verify `kafka-consumer-lag` alert rule is visible in Grafana Alerting UI with correct PromQL query (`kafka_consumer_records_lag_max`), threshold (1000), and `for: 2m`
- [x] T020 [US4] Verify alert is in "Normal" or "OK" state under normal conditions (consumer lag metric not currently exposed, alert in OK due to noDataState: OK). Confirm labels include `severity: warning`.

**Checkpoint**: Kafka lag alert provisioned and evaluating correctly. US4 acceptance scenarios validated.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Documentation updates and final validation across both deployment modes

- [x] T021 [P] Update `README.md` — add Grafana Alert Rules section documenting the 4 alert rules, thresholds, how to view them, and how to test them
- [x] T022 [P] Update `apisix-k8s/README.md` — add `configmap-alert-rules.yaml` to directory structure and Grafana section
- [x] T023 Verify no false-positive alerts during 5-minute normal operation period (all services healthy, no simulations active) — validates SC-006
- [x] T024 Run `docker compose down -v` and `docker compose up -d` to verify alert rules survive fresh deployment — validates SC-001

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately. T001 and T002 are parallel (different files).
- **Foundational (Phase 2)**: Depends on Phase 1 (datasource UID). T005 and T006 are K8s-specific and can parallel with T003/T004.
- **US1 (Phase 3)**: Depends on Phase 2 (alert rules must be provisioned)
- **US2 (Phase 4)**: Depends on Phase 2. Can run in parallel with US1.
- **US3 (Phase 5)**: Depends on Phase 2. Can run in parallel with US1/US2.
- **US4 (Phase 6)**: Depends on Phase 2. Can run in parallel with US1/US2/US3.
- **Polish (Phase 7)**: Depends on all user stories being verified.

### User Story Dependencies

- **US5 (Provisioning)**: Foundational — BLOCKS all other stories
- **US1 (Error Rate)**: Depends on US5 only. Independent of US2/US3/US4.
- **US2 (Latency)**: Depends on US5 only. Independent of US1/US3/US4.
- **US3 (JVM Heap)**: Depends on US5 only. Independent of US1/US2/US4.
- **US4 (Kafka Lag)**: Depends on US5 only. Independent of US1/US2/US3.

### Parallel Opportunities

- T001 and T002 can run in parallel (different files)
- T005 and T006 can run in parallel (different K8s files)
- T021 and T022 can run in parallel (different README files)
- US1, US2, US3, US4 verification phases can run in parallel (different alert rules, different simulations)

---

## Parallel Example: Setup Phase

```bash
# Launch both datasource UID updates together:
Task: "Add uid: prometheus to grafana/provisioning/datasources/datasource.yaml"
Task: "Add uid: prometheus to apisix-k8s/grafana/configmap-datasources.yaml"
```

## Parallel Example: K8s Integration

```bash
# Launch K8s ConfigMap and deployment update together:
Task: "Create ConfigMap in apisix-k8s/grafana/configmap-alert-rules.yaml"
Task: "Update deployment in apisix-k8s/grafana/deployment.yaml"
```

---

## Implementation Strategy

### MVP First (US5 + US1)

1. Complete Phase 1: Setup (datasource UID)
2. Complete Phase 2: Foundational / US5 (create alert rules, wire Docker Compose + K8s)
3. Complete Phase 3: US1 (verify error rate alert fires and resolves)
4. **STOP and VALIDATE**: Error rate alert is the most critical — if it works, the provisioning pipeline is proven.

### Incremental Delivery

1. Setup + Foundational → 4 alert rules provisioned, visible in Grafana ✓
2. Verify US1 (error rate) → Core alert validated ✓
3. Verify US2 (latency) → Second critical alert validated ✓
4. Verify US3 (JVM heap) + US4 (Kafka lag) → All alerts validated ✓
5. Polish → Documentation updated, fresh deployment tested ✓

---

## Notes

- This is a **configuration-only** feature — no Java/application code changes.
- All 4 alert rules are created in a single YAML file (T003) since they share the same alert rule group and evaluation interval.
- The contract file (`specs/005-grafana-alert-rules/contracts/alert-rules.yaml`) contains the exact YAML to use — T003 should use it as the source.
- Verification tasks (T007-T020) require the PoC to be running with Docker Compose. Use direct service ports (8081-8085) rather than APISIX gateway port (9080) for Docker Compose testing.
- [P] tasks = different files, no dependencies
- Commit after each phase or logical group
