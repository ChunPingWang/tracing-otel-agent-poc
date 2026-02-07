# Tasks: Grafana Alert Contact Point & Notification Policy

**Input**: Design documents from `/specs/006-grafana-alert-notifications/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/contact-points.yaml, contracts/notification-policies.yaml

**Tests**: No automated tests requested. Verification is done via manual/scripted checks against Grafana API and `docker compose logs`.

**Organization**: Tasks are grouped by user story. US4 (File-Based Provisioning) is foundational — it creates the infrastructure that enables US1-US3. US3 (Webhook Receiver) must be deployed before US1/US2 can be verified end-to-end.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Webhook Receiver Service (US3, Priority: P1)

**Purpose**: Deploy the webhook receiver so the contact point has a reachable destination.

**Goal**: A lightweight HTTP service running as part of the Docker Compose stack that logs all incoming POST requests to stdout.

**Independent Test**: Run `docker compose up -d`, then `curl -X POST http://localhost:8090/test -d '{"hello":"world"}'` and verify the payload appears in `docker compose logs webhook-sink`.

### Implementation

- [x] T001 [US3] Add `webhook-sink` service to `docker-compose.yml` using `mendhak/http-https-echo:39` image, port 8090:8080, on `ecommerce-net` network
- [x] T002 [US3] Verify webhook-sink starts: run `docker compose up -d webhook-sink`, send a test POST request via curl, confirm payload appears in `docker compose logs webhook-sink`

**Checkpoint**: Webhook receiver running and logging POST payloads. Contact point now has a reachable destination.

---

## Phase 2: Contact Point Provisioning (US1 + US4, Priority: P1) 🎯 MVP

**Purpose**: Create the webhook contact point provisioning file and wire it into the Grafana provisioning directory.

**Goal**: Grafana automatically provisions the `ecommerce-webhook` contact point on startup, pointing to the webhook-sink service.

**Independent Test**: Run `docker compose up -d`, wait for Grafana, then verify the contact point via `curl -s http://localhost:3000/api/v1/provisioning/contact-points`.

### Implementation

- [x] T003 [US1] Create `grafana/provisioning/alerting/contact-points.yaml` per `specs/006-grafana-alert-notifications/contracts/contact-points.yaml` — webhook contact point named `ecommerce-webhook` with URL `http://webhook-sink:8080/grafana-alerts`
- [x] T004 [US1] Verify contact point provisioning: run `docker compose up -d`, wait for Grafana to be healthy, query `curl -s http://localhost:3000/api/v1/provisioning/contact-points` and confirm `ecommerce-webhook` appears with correct webhook URL

**Checkpoint**: Contact point provisioned automatically. No Docker Compose volume change needed (existing directory mount covers new file).

---

## Phase 3: Notification Policy Provisioning (US2 + US4, Priority: P1)

**Purpose**: Create the notification policy provisioning file with severity-based routing.

**Goal**: Grafana automatically provisions the notification policy tree with separate routes for critical and warning severity alerts.

**Independent Test**: Run `docker compose up -d`, then verify via `curl -s http://localhost:3000/api/v1/provisioning/policies` that the policy tree has the correct routing rules.

### Implementation

- [x] T005 [US2] Create `grafana/provisioning/alerting/notification-policies.yaml` per `specs/006-grafana-alert-notifications/contracts/notification-policies.yaml` — root policy with `ecommerce-webhook` receiver, group_by [grafana_folder, alertname], child routes for critical (group_wait: 5s, repeat: 1m) and warning (group_wait: 10s, repeat: 5m)
- [x] T006 [US2] Verify notification policy provisioning: query `curl -s http://localhost:3000/api/v1/provisioning/policies` and confirm the policy tree has root receiver `ecommerce-webhook`, 2 child routes matching `severity = critical` and `severity = warning`

**Checkpoint**: Full alerting pipeline provisioned — alert rules (Feature 005) → notification policy → contact point → webhook receiver.

---

## Phase 4: End-to-End Verification (US1 + US2 + US3, Priority: P1)

**Purpose**: Verify the complete notification pipeline works: alert fires → notification policy routes → contact point sends → webhook receiver logs.

**Goal**: Demonstrate that notifications are delivered for firing and resolved alerts, and no false notifications occur during normal operation.

### Verification

- [x] T007 [US1] Trigger high error rate alert: send 30+ orders with `productId: P999` (1 per second) to generate 5xx errors on order-service
- [x] T008 [US1] Wait ~90 seconds for alert to fire, then check `docker compose logs webhook-sink` for a notification payload containing `"status":"firing"`, `alertname: High Error Rate`, `severity: critical`
- [x] T009 [US1] Verify resolved notification: send 30+ normal orders with `productId: P001` (1 per second) to clear the error rate, wait ~90 seconds, check webhook-sink logs for `"status":"resolved"`
- [x] T010 [US3] Verify no false notifications: with all services healthy and no simulations active, observe `docker compose logs -f webhook-sink` for 2+ minutes and confirm zero notification payloads are logged (validates SC-004)
- [x] T011 [US4] Fresh deployment test: run `docker compose down -v && docker compose up -d`, wait for Grafana, verify contact point and notification policy both exist via Grafana API (validates SC-001)

**Checkpoint**: End-to-end alerting pipeline verified — notifications fire, resolve, and don't false-positive.

---

## Phase 5: Kubernetes Integration (US4, Priority: P1)

**Purpose**: Extend the alerting notification pipeline to the Kind K8s deployment.

**Goal**: Contact point, notification policy, and webhook receiver are provisioned in K8s identically to Docker Compose.

### Implementation

- [x] T012 [P] [US4] Create K8s ConfigMap for contact points in `apisix-k8s/grafana/configmap-contact-points.yaml` embedding the `contact-points.yaml` content
- [x] T013 [P] [US4] Create K8s ConfigMap for notification policies in `apisix-k8s/grafana/configmap-notification-policies.yaml` embedding the `notification-policies.yaml` content
- [x] T014 [US4] Update K8s Grafana deployment in `apisix-k8s/grafana/deployment.yaml` — add `contact-points` and `notification-policies` volumes (from ConfigMaps) and volumeMounts at `/etc/grafana/provisioning/alerting/contact-points.yaml` and `/etc/grafana/provisioning/alerting/notification-policies.yaml` with subPath
- [x] T015 [P] [US4] Create K8s webhook-sink deployment and service in `apisix-k8s/webhook-sink/deployment.yaml` and `apisix-k8s/webhook-sink/service.yaml` using `mendhak/http-https-echo:39` image

**Checkpoint**: K8s manifests ready for webhook-sink, contact points, and notification policies.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Documentation updates and final validation

- [x] T016 [P] Update `README.md` — add webhook-sink to tech stack table, service port table (8090), project structure, and quickstart section
- [x] T017 [P] Update `apisix-k8s/README.md` — add `configmap-contact-points.yaml`, `configmap-notification-policies.yaml`, and `webhook-sink/` to directory structure and architecture overview

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Webhook Receiver)**: No dependencies — can start immediately. Needed before Phases 2-4.
- **Phase 2 (Contact Point)**: Depends on Phase 1 (webhook-sink must exist for the contact point URL to be valid).
- **Phase 3 (Notification Policy)**: Depends on Phase 2 (contact point must exist for policy `receiver` reference).
- **Phase 4 (E2E Verification)**: Depends on Phases 1-3 (all components must be provisioned).
- **Phase 5 (K8s Integration)**: Depends on Phase 3 (need final YAML content). T012, T013, T015 are parallel.
- **Phase 6 (Polish)**: Depends on all verification being complete.

### User Story Dependencies

- **US3 (Webhook Receiver)**: Foundational — BLOCKS US1 and US2 (contact point needs a URL)
- **US1 (Contact Point)**: Depends on US3. Must exist before US2 (policy references contact point by name).
- **US2 (Notification Policy)**: Depends on US1 (policy `receiver` field references contact point `name`).
- **US4 (File-Based Provisioning)**: Cross-cutting — implemented across Phases 2, 3, 5 alongside US1/US2.

### Parallel Opportunities

- T012 and T013 can run in parallel (different K8s ConfigMap files)
- T012/T013 and T015 can run in parallel (different K8s directories)
- T016 and T017 can run in parallel (different README files)

---

## Implementation Strategy

### MVP First (US3 + US1 + US2)

1. Complete Phase 1: Webhook Receiver (US3)
2. Complete Phase 2: Contact Point (US1)
3. Complete Phase 3: Notification Policy (US2)
4. Complete Phase 4: E2E Verification
5. **STOP and VALIDATE**: Entire notification pipeline works in Docker Compose.

### Incremental Delivery

1. Webhook Receiver → contact point has a destination ✓
2. Contact Point provisioned → Grafana knows where to send notifications ✓
3. Notification Policy provisioned → severity-based routing active ✓
4. E2E Verification → firing, resolved, no-false-positive validated ✓
5. K8s Integration → same pipeline in Kind cluster ✓
6. Polish → documentation updated ✓

---

## Notes

- This is a **configuration-only** feature — no Java/application code changes.
- The Docker Compose volume for `grafana/provisioning/alerting/` already mounts the entire directory (established in Feature 005), so new YAML files are automatically available — no `docker-compose.yml` volume change needed for Grafana.
- The notification policy tree is a **single resource per org** — the entire tree must be in one `policies:` entry.
- Contact point `name` must exactly match the `receiver` field in notification policies.
- The contract files (`specs/006-grafana-alert-notifications/contracts/`) contain the exact YAML to use — T003 and T005 should use them as the source.
- Verification tasks (T007-T011) require the PoC to be running with Docker Compose. Use direct service ports (8081-8085) for testing.
- [P] tasks = different files, no dependencies
- Commit after each phase or logical group
