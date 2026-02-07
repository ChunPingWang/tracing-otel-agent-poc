# Feature Specification: Grafana Alert Rules

**Feature Branch**: `005-grafana-alert-rules`
**Created**: 2026-02-07
**Status**: Draft
**Input**: User description: "add alert rules for Grafana dashboards"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - High Error Rate Alert (Priority: P1)

As an operator monitoring the e-commerce PoC, I want to be automatically alerted when any microservice experiences a high error rate (5xx responses), so that I can investigate and resolve issues before they impact more users.

**Why this priority**: Error rate is the most critical indicator of service health. A spike in 5xx errors means orders are failing, payments are broken, or inventory lookups are erroring out — direct customer impact.

**Independent Test**: Can be fully tested by triggering the payment-service delay simulation (`/payment/admin/simulate-delay?ms=10000`) or notification-service failure simulation (`/notification/admin/simulate-failure?enabled=true`), sending requests, and verifying that the alert fires and appears in the Grafana alert list.

**Acceptance Scenarios**:

1. **Given** the system is running normally with less than 5% error rate, **When** notification-service failure simulation is enabled and orders are placed, **Then** an alert fires within the configured evaluation interval indicating high error rate for the affected service(s).
2. **Given** an alert is currently firing for high error rate, **When** the failure simulation is disabled and the error rate drops below the threshold, **Then** the alert resolves automatically.
3. **Given** all services are healthy, **When** I view the Grafana alert list, **Then** all error rate alerts show "Normal" status.

---

### User Story 2 - High Response Latency Alert (Priority: P1)

As an operator, I want to be alerted when service response latency (p95) exceeds acceptable thresholds, so that I can identify slow services and take action before user experience degrades significantly.

**Why this priority**: Latency directly affects user experience. Even if requests succeed, high latency means the system feels broken to users. This is equally critical as error rate for a production-like monitoring setup.

**Independent Test**: Can be tested by enabling payment-service delay simulation (`/payment/admin/simulate-delay?ms=5000`), placing orders, and verifying that the latency alert fires for the affected service.

**Acceptance Scenarios**:

1. **Given** normal latency levels (p95 under threshold), **When** payment-service delay is set to 5000ms and orders are placed, **Then** a latency alert fires for the affected service within the evaluation interval.
2. **Given** a latency alert is firing, **When** the delay simulation is removed and latency returns to normal, **Then** the alert resolves automatically.

---

### User Story 3 - JVM Memory Pressure Alert (Priority: P2)

As an operator, I want to be alerted when any microservice's JVM heap usage exceeds a high-water mark, so that I can investigate potential memory leaks or under-provisioned services before they cause OutOfMemoryError crashes.

**Why this priority**: Memory pressure is a leading indicator of upcoming service crashes. While less immediately visible to users than errors or latency, it can cause cascading failures if left unchecked.

**Independent Test**: Can be tested by verifying the alert rule exists and evaluates correctly against current JVM metrics. Under normal PoC load, heap usage should remain below the threshold, so the alert should stay in "Normal" state. The rule definition correctness can be verified by inspecting the provisioned alert rules in Grafana.

**Acceptance Scenarios**:

1. **Given** JVM heap usage is below the threshold for all services, **When** I view the Grafana alert list, **Then** the JVM memory alert shows "Normal" status for all services.
2. **Given** the alert rule is provisioned, **When** a service's heap usage exceeds the configured threshold, **Then** an alert fires identifying the specific service.

---

### User Story 4 - Kafka Consumer Lag Alert (Priority: P2)

As an operator, I want to be alerted when Kafka consumer lag grows beyond acceptable levels, so that I can identify consumers that are falling behind and may cause stale data or delayed order processing.

**Why this priority**: Consumer lag is an important operational metric for event-driven architectures. High lag means notifications or downstream processing are delayed, which degrades the system's responsiveness.

**Independent Test**: Can be tested by verifying the alert rule exists and evaluates correctly. Under normal PoC conditions, consumer lag should be low and the alert should remain in "Normal" state.

**Acceptance Scenarios**:

1. **Given** Kafka consumer lag is within acceptable bounds, **When** I view the Grafana alert list, **Then** the consumer lag alert shows "Normal" status.
2. **Given** the alert rule is provisioned, **When** consumer lag exceeds the configured threshold, **Then** an alert fires identifying the affected consumer group.

---

### User Story 5 - File-Based Alert Provisioning (Priority: P1)

As a developer deploying the PoC, I want all alert rules to be automatically provisioned when Grafana starts (via file-based provisioning), so that no manual configuration is needed — alerts are ready immediately after deployment.

**Why this priority**: This is foundational — without automated provisioning, operators would need to manually create alert rules after every deployment, which defeats the purpose of a one-command setup.

**Independent Test**: Can be tested by running `docker compose up` (or the K8s deployment script) and verifying that all alert rules appear in the Grafana Alerting UI without any manual configuration steps.

**Acceptance Scenarios**:

1. **Given** a fresh deployment (docker compose up or Kind cluster creation), **When** Grafana finishes starting, **Then** all alert rules are visible in the Grafana Alerting > Alert Rules page.
2. **Given** alert rules are provisioned, **When** I view any alert rule's details, **Then** it shows the correct evaluation interval, threshold, and associated metric query.
3. **Given** the PoC is deployed via the K8s deployment script, **When** Grafana starts in the Kind cluster, **Then** the same alert rules are provisioned automatically.

---

### Edge Cases

- What happens when a metric has no data yet (service just started, no requests sent)? Alert rules should handle `NoData` state gracefully — either staying in "Normal" or entering a distinct "No Data" state, not firing a false alert.
- What happens when Prometheus is temporarily unreachable? Alert rules should enter an "Error" state, not fire false positives.
- What happens when the metric name changes due to an OTel agent upgrade? The alert rules reference specific metric names; they would need to be updated. This is documented as an assumption.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST provision alert rules automatically when Grafana starts, using file-based provisioning (no manual UI configuration required).
- **FR-002**: System MUST include an alert rule for high error rate (5xx response percentage) per service, with a configurable threshold (default: 5%).
- **FR-003**: System MUST include an alert rule for high response latency (p95) per service, with a configurable threshold (default: 2000ms).
- **FR-004**: System MUST include an alert rule for JVM heap memory usage per service, with a configurable threshold (default: 80% of max heap).
- **FR-005**: System MUST include an alert rule for Kafka consumer lag, with a configurable threshold (default: 1000 records).
- **FR-006**: Alert rules MUST resolve automatically when the condition clears (i.e., metric returns below threshold).
- **FR-007**: Alert rules MUST handle "No Data" scenarios gracefully — they should NOT fire false alerts when metrics are not yet available.
- **FR-008**: Alert rules MUST work in both Docker Compose and Kubernetes (Kind) deployments.
- **FR-009**: System MUST organize alert rules into a logical alert rule group for manageability.
- **FR-010**: Each alert rule MUST include descriptive labels (service name, severity) and annotations (summary, description) to aid operator understanding.

### Key Entities

- **Alert Rule**: A condition definition that evaluates a metric query against a threshold at a regular interval. Contains: name, condition query, threshold, evaluation interval, labels, annotations, and NoData/Error handling behavior.
- **Alert Rule Group**: A logical grouping of related alert rules that share an evaluation interval. Groups are used for organizational purposes.
- **Contact Point**: The destination for alert notifications (out of scope for this PoC — Grafana's default internal alertmanager is sufficient).

## Assumptions

- Alert notifications (email, Slack, PagerDuty, etc.) are out of scope. The PoC only needs alert rules visible in the Grafana Alerting UI. No external notification channels will be configured.
- The existing Prometheus datasource provisioned in Feature 004 is used as the data source for all alert rules.
- Metric names follow the OTel Java Agent v1.32.1 old semantic conventions (e.g., `http_server_duration_milliseconds_*`, `process_runtime_jvm_memory_usage_bytes`, `kafka_consumer_records_lag_max`).
- Grafana version 11.6.0 (as deployed in the PoC) supports file-based alert rule provisioning.
- Alert evaluation interval of 30 seconds is appropriate for this PoC (not a production system requiring sub-second detection).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: All alert rules are visible in the Grafana Alerting UI within 60 seconds of Grafana starting, with zero manual configuration steps.
- **SC-002**: When the notification-service failure simulation is enabled and orders are placed, the error rate alert fires within 2 evaluation cycles (within ~60 seconds).
- **SC-003**: When the payment-service delay simulation is set to 5000ms, the latency alert fires within 2 evaluation cycles (within ~60 seconds).
- **SC-004**: When all simulations are disabled and metrics return to normal, all alerts resolve automatically within 3 evaluation cycles (within ~90 seconds).
- **SC-005**: Alert rules are provisioned identically in both Docker Compose and Kubernetes (Kind) deployments.
- **SC-006**: No false-positive alerts fire during normal operation (all services healthy, no simulations active) over a 5-minute observation period.
