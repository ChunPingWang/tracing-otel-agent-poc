# Feature Specification: Grafana Alert Contact Point & Notification Policy

**Feature Branch**: `006-grafana-alert-notifications`
**Created**: 2026-02-07
**Status**: Draft
**Input**: User description: "add contact point and notification policy for Grafana alerts"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Webhook Contact Point (Priority: P1)

As an operator, I want alert notifications to be sent to a webhook endpoint when alerts fire, so that I can integrate Grafana alerting with external systems (chat tools, incident management, custom dashboards) without manual monitoring of the Grafana UI.

**Why this priority**: A contact point is the foundational building block — without it, alerts only appear in the Grafana UI and no external notification occurs. Webhook is the most versatile contact point type, supporting integration with virtually any external system.

**Independent Test**: Can be tested by triggering a known alert (e.g., high error rate via product ID P999), then verifying that the webhook endpoint receives a notification payload containing alert details (alert name, severity, affected service, description).

**Acceptance Scenarios**:

1. **Given** a webhook contact point is configured, **When** the high error rate alert fires, **Then** the webhook endpoint receives a notification containing the alert name, severity label, affected service name, and description.
2. **Given** a webhook contact point is configured, **When** a firing alert resolves back to normal, **Then** the webhook endpoint receives a "resolved" notification.
3. **Given** a fresh deployment (no prior state), **When** Grafana starts, **Then** the webhook contact point is automatically provisioned without manual configuration.

---

### User Story 2 - Notification Policy Routing (Priority: P1)

As an operator, I want notification policies to route alerts to the appropriate contact point based on severity, so that critical alerts (e.g., high error rate) are handled differently from warning alerts (e.g., JVM heap pressure).

**Why this priority**: Without a notification policy, Grafana uses a default policy that either sends everything to one contact point or sends nothing. A routing policy ensures critical alerts get immediate attention while lower-severity alerts follow a different cadence.

**Independent Test**: Can be tested by triggering a critical alert and a warning alert separately, then verifying that each is routed according to the defined notification policy (e.g., different repeat intervals or grouping behavior).

**Acceptance Scenarios**:

1. **Given** a notification policy is configured with severity-based routing, **When** a critical alert fires, **Then** it is routed to the designated contact point with the repeat interval configured for critical alerts.
2. **Given** a notification policy is configured, **When** a warning alert fires, **Then** it is routed to the designated contact point with the repeat interval configured for warning alerts.
3. **Given** a fresh deployment, **When** Grafana starts, **Then** the notification policy is automatically provisioned.

---

### User Story 3 - Webhook Receiver for Local Verification (Priority: P1)

As a developer testing the PoC locally, I want a lightweight webhook receiver running as part of the deployment, so that I can see alert notifications in container logs without needing any external service or account setup.

**Why this priority**: The PoC needs to be fully self-contained. Without a local webhook receiver, the contact point would have no reachable destination, making it impossible to verify notifications work end-to-end.

**Independent Test**: Can be tested by triggering any alert, waiting for it to fire, then checking the webhook receiver's logs for the notification payload.

**Acceptance Scenarios**:

1. **Given** the PoC stack is running, **When** an alert fires and a notification is sent, **Then** the webhook receiver logs the full notification payload (alert name, status, labels, annotations).
2. **Given** the webhook receiver is part of the stack, **When** I run `docker compose up`, **Then** the webhook receiver starts automatically alongside other services.
3. **Given** the webhook receiver is running, **When** no alerts are firing, **Then** the receiver logs no notification messages (no noise from healthy state).

---

### User Story 4 - File-Based Provisioning for Contact Points and Policies (Priority: P1)

As a developer, I want the contact points and notification policies to be provisioned automatically via configuration files (same pattern as alert rules from Feature 005), so that the entire alerting pipeline is reproducible and requires zero manual setup.

**Why this priority**: Foundational — this is the delivery mechanism for US1 and US2. Without file-based provisioning, operators would need to manually configure contact points and policies after every deployment.

**Independent Test**: Can be tested by running `docker compose down -v && docker compose up -d`, then verifying contact points and notification policies exist in Grafana without any manual steps.

**Acceptance Scenarios**:

1. **Given** a fresh deployment via Docker Compose, **When** Grafana finishes starting, **Then** the webhook contact point and notification policy are visible in Grafana Alerting > Contact Points and Notification Policies pages.
2. **Given** the PoC is deployed via the K8s deployment script, **When** Grafana starts in the Kind cluster, **Then** the same contact point and notification policy are provisioned automatically.
3. **Given** the provisioned contact point and policy, **When** I inspect them in the Grafana UI, **Then** they match the expected configuration (webhook URL, routing rules, repeat intervals).

---

### Edge Cases

- What happens when the webhook receiver is temporarily unreachable? Grafana should retry notification delivery according to its built-in retry mechanism, and the alert state in the UI should not be affected.
- What happens when multiple alerts fire simultaneously? The notification policy should group related alerts (by alert rule name or service) to avoid notification storms.
- What happens when an alert fires but the contact point configuration is invalid (e.g., unreachable URL)? Grafana should log an error and the alert should remain visible in the UI regardless of notification delivery status.
- What happens during the initial startup period before metrics are available? The noDataState: OK configuration from Feature 005 ensures no false alerts fire, so no spurious notifications are sent.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST provision at least one contact point automatically when Grafana starts, using file-based provisioning.
- **FR-002**: System MUST provision a notification policy automatically when Grafana starts, using file-based provisioning.
- **FR-003**: The contact point MUST be of type webhook, sending notifications to a local receiver included in the deployment stack.
- **FR-004**: The notification policy MUST route alerts based on severity label — critical alerts and warning alerts may have different repeat intervals.
- **FR-005**: The notification policy MUST group alerts to prevent notification storms when multiple alerts fire simultaneously.
- **FR-006**: The webhook receiver MUST log received notification payloads to standard output for easy verification via container logs.
- **FR-007**: The contact point and notification policy MUST be provisioned in both Docker Compose and Kubernetes (Kind) deployments.
- **FR-008**: Resolved notifications MUST be sent when a firing alert returns to normal state.
- **FR-009**: The provisioning MUST follow the same pattern established in Feature 005 (alert rules file-based provisioning) for consistency.
- **FR-010**: The webhook receiver MUST be a lightweight, zero-configuration service that starts automatically as part of the PoC stack.

### Key Entities

- **Contact Point**: A notification destination definition specifying how and where to send alert notifications. Contains: name, type (webhook), URL, and optional settings (HTTP method, headers).
- **Notification Policy**: A routing tree that determines which contact point receives notifications for which alerts. Contains: matching criteria (labels), contact point reference, grouping rules, and timing settings (group wait, group interval, repeat interval).
- **Webhook Receiver**: A lightweight HTTP service that accepts POST requests from Grafana and logs the notification payload. Exists solely for PoC verification purposes.

## Assumptions

- This feature builds on Feature 005 (Grafana Alert Rules) — the 4 alert rules with severity labels (critical, warning) already exist.
- The webhook receiver is a PoC-only component for local verification. In production, the contact point would point to real systems (Slack, PagerDuty, email, etc.).
- The existing Grafana provisioning directory structure (established in Features 004 and 005) will be extended, not replaced.
- Grafana 11.6.0 supports file-based provisioning of contact points and notification policies (same provisioning mechanism as alert rules).
- The default Grafana Alertmanager is used (not an external Alertmanager instance).
- A single contact point is sufficient for this PoC. Multiple contact points (e.g., separate ones for critical vs. warning) may be added but are not required.

## Out of Scope

- External notification channels (Slack, PagerDuty, email, Microsoft Teams) — the PoC uses a local webhook receiver only.
- Silencing or muting rules — operators can manage these manually in the Grafana UI if needed.
- Escalation policies (e.g., "if not acknowledged in 15 minutes, escalate to on-call manager").
- Custom notification templates beyond Grafana's built-in defaults.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Contact point and notification policy are visible in the Grafana UI within 60 seconds of Grafana starting, with zero manual configuration steps.
- **SC-002**: When a critical alert fires (e.g., high error rate), the webhook receiver logs a notification payload within 2 minutes of the alert entering "Alerting" state.
- **SC-003**: When a firing alert resolves, the webhook receiver logs a "resolved" notification within 2 minutes.
- **SC-004**: During a 5-minute normal operation period (no simulations active), the webhook receiver logs zero notification payloads (no false notifications).
- **SC-005**: Contact point, notification policy, and webhook receiver are provisioned identically in both Docker Compose and Kubernetes (Kind) deployments.
- **SC-006**: When 3+ alerts fire simultaneously, they are grouped into a single notification (not sent as individual messages) per the notification policy grouping configuration.
