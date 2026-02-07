# Feature Specification: Apache APISIX Blue-Green Deployment

**Feature Branch**: `003-apisix-blue-green`
**Created**: 2026-02-07
**Status**: Clarified
**Input**: User description: "add Apache APISix for blue-green deployment"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Blue-Green Traffic Switching via API Gateway (Priority: P1)

As a release engineer, I want to deploy two versions of the e-commerce order service (Blue/current and Green/new) behind an API gateway, and dynamically switch traffic between them using weighted routing, so I can perform zero-downtime deployments and gradually shift traffic to the new version.

**Why this priority**: This is the core blue-green deployment capability. Without weighted traffic splitting between two service versions, none of the other deployment scenarios can be demonstrated. It delivers the fundamental value of risk-free releases.

**Independent Test**: Can be fully tested by deploying both service versions, configuring weighted traffic split (e.g., 90/10), sending 100 requests, and verifying that traffic distribution matches the configured weights.

**Acceptance Scenarios**:

1. **Given** Blue (v1) and Green (v2) versions of the order service are deployed, **When** the gateway is configured with 100% traffic to Blue, **Then** all requests are served by the Blue version.
2. **Given** both versions are deployed, **When** the gateway is configured with 90% Blue / 10% Green (canary), **Then** approximately 90% of requests go to Blue and 10% to Green over 100 requests.
3. **Given** both versions are deployed, **When** the gateway is configured with 50% Blue / 50% Green, **Then** traffic is approximately evenly distributed between both versions.
4. **Given** the canary phase is successful, **When** the gateway is configured with 0% Blue / 100% Green, **Then** all traffic shifts to the Green version with zero downtime.

---

### User Story 2 - Header-Based Routing for Testing (Priority: P2)

As a QA engineer, I want to access the Green (new) version directly by adding a specific HTTP header to my requests, while all other users continue to be routed to the Blue (current) version, so I can validate the new version in production conditions before opening it to general traffic.

**Why this priority**: Header-based routing is essential for pre-release validation. QA teams need to test the new version against real infrastructure without affecting end users.

**Independent Test**: Can be tested by sending a request with the canary header and verifying it reaches the Green version, then sending a request without the header and verifying it reaches the Blue version.

**Acceptance Scenarios**:

1. **Given** both versions are deployed and header-based routing is configured, **When** a request includes the canary header, **Then** the request is routed to the Green version regardless of weight configuration.
2. **Given** header-based routing is active, **When** a request does not include the canary header, **Then** the request follows the default weighted routing (Blue).
3. **Given** a QA engineer sends a full order creation request with the canary header, **Then** the order is processed by the Green version and the response identifies it as the Green version.

---

### User Story 3 - Instant Rollback (Priority: P2)

As a release engineer, I want to immediately roll back all traffic to the Blue (previous) version if the Green version shows errors or degraded performance, so I can minimize the blast radius of a bad deployment.

**Why this priority**: Rollback is the safety net for blue-green deployments. Without instant rollback, the deployment strategy cannot be considered production-ready.

**Independent Test**: Can be tested by switching 100% traffic to Green, then issuing a rollback command to switch 100% back to Blue, and verifying all subsequent requests go to Blue.

**Acceptance Scenarios**:

1. **Given** 100% traffic is routed to Green, **When** the operator executes a rollback, **Then** all traffic immediately shifts back to Blue with zero downtime.
2. **Given** a canary configuration (90/10), **When** the operator detects errors in Green and executes a rollback, **Then** traffic reverts to 100% Blue and no further requests reach Green.

---

### User Story 4 - Distributed Tracing Preserved Through Gateway (Priority: P3)

As an SRE engineer, I want the distributed tracing context (W3C traceparent) to be preserved when requests pass through the API gateway, so I can see the complete end-to-end trace in Jaeger including the gateway hop.

**Why this priority**: Tracing through the gateway validates that the blue-green deployment infrastructure does not break the existing observability tooling. This is important but secondary to the core deployment functionality.

**Independent Test**: Can be tested by sending an order request through the gateway, then verifying in Jaeger that the trace includes spans for the gateway and all downstream services.

**Acceptance Scenarios**:

1. **Given** the gateway routes a request to the order service, **When** the order is processed through all 5 microservices, **Then** a single Jaeger trace shows the complete span chain including the gateway.
2. **Given** the gateway is configured with the tracing plugin, **When** 10 requests are sent, **Then** all 10 traces appear in Jaeger with consistent span structure.

---

### User Story 5 - One-Command Environment Setup (Priority: P1)

As an evaluator, I want to run a single script that sets up the entire blue-green deployment environment (Kind cluster, API gateway, Blue version, Green version, observability stack), so I can start demonstrating the deployment scenarios without manual configuration.

**Why this priority**: Ease of setup is critical for PoC adoption. A complex manual setup would undermine the PoC's value as a reference implementation.

**Independent Test**: Can be fully tested by running the setup script on a clean machine (with prerequisites installed) and verifying all pods are Running/Ready and the gateway is accepting requests.

**Acceptance Scenarios**:

1. **Given** Docker, Kind, kubectl, and Helm are installed, **When** the user runs the setup script, **Then** a Kind cluster is created, the API gateway and both service versions are deployed, and all pods reach Ready status within 5 minutes.
2. **Given** the environment is fully deployed, **When** the user runs the teardown script, **Then** the Kind cluster and all resources are completely removed.

---

### Edge Cases

- What happens when the Green version's pods are not yet ready when traffic is shifted? The gateway must respect readiness probes and only route to healthy pods.
- What happens when a traffic weight update fails (e.g., gateway admin API is unreachable)? The current routing configuration must remain unchanged (fail-safe).
- What happens when both Blue and Green versions are scaled to zero replicas? The gateway should return appropriate error responses (502/503).
- What happens when the operator configures weights that don't sum to 100? The system should normalize weights proportionally.
- What happens during a weight change — are in-flight requests affected? In-flight requests should complete against the version they started with; only new requests follow the updated weights.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST deploy two versions (Blue and Green) of the order service simultaneously on a Kind cluster, each as an independent workload with its own service endpoint.
- **FR-002**: System MUST provide an API gateway that routes incoming HTTP traffic to Blue or Green versions based on configurable weighted traffic splitting.
- **FR-003**: Traffic weight configuration MUST be updatable dynamically without restarting any component or causing request failures.
- **FR-004**: System MUST support header-based routing where a specific HTTP header (e.g., `X-Canary: true`) directs the request to the Green version, regardless of weight configuration.
- **FR-005**: System MUST support the following traffic split scenarios: 100/0 (all Blue), 90/10 (canary), 50/50 (equal split), 0/100 (all Green), and rollback to 100/0.
- **FR-006**: System MUST preserve W3C Trace Context (traceparent header) when proxying requests through the gateway, so distributed traces remain intact.
- **FR-007**: System MUST provide a deployment script that automates the full environment setup: Kind cluster creation, gateway installation, both service version deployments, and observability stack.
- **FR-008**: System MUST provide a teardown script that completely removes the Kind cluster and all associated resources.
- **FR-009**: The gateway MUST expose the order service API to the host machine so test scenarios can be executed from the host.
- **FR-010**: System MUST provide convenience scripts or documented commands for common operations: switch to canary (90/10), switch to 50/50, full cutover (0/100), rollback (100/0).
- **FR-011**: The Green version MUST be identifiable in responses (e.g., via a response header or body field) so traffic distribution can be verified.
- **FR-012**: The gateway admin interface MUST be accessible from the host machine for route and upstream configuration.
- **FR-013**: System MUST include readiness probes for all service deployments so the gateway only routes to healthy pods.
- **FR-014**: All deployment artifacts (manifests, scripts, configuration) MUST be organized in a dedicated directory at the project root.

### Key Entities

- **Blue Version (v1)**: The current/stable version of the order service receiving production traffic by default. Represents the existing e-commerce order flow.
- **Green Version (v2)**: The new version of the order service being validated. Functionally identical to Blue but identifiable via version markers in responses.
- **API Gateway**: The traffic management component that sits in front of both versions, handling weighted routing, header-based routing, and request proxying.
- **Upstream**: A gateway concept representing a backend service endpoint (Blue or Green) with health checking and load balancing configuration.
- **Route**: A gateway concept that maps incoming request patterns (URI, method, headers) to upstream targets with plugin configurations (traffic-split, proxy-rewrite, etc.).
- **Traffic Split Configuration**: The weighted distribution rules determining what percentage of traffic goes to each version.

## Clarifications

### Session 2026-02-07

- Structured ambiguity scan performed across all taxonomy categories (behavioral, interface, data/entity, integration, scope, performance/quality, deployment/operational).
- No critical ambiguities identified. Spec is comprehensive with clear acceptance scenarios, measurable success criteria, and well-bounded scope.
- Minor note: FR-011 Green version identification method ("response header or body field") left intentionally flexible as an implementation detail.

## Assumptions

- The existing e-commerce microservices (order, product, inventory, payment, notification) from the tracing PoC are reused. Blue and Green represent two deployed versions of the order service; other services have a single version.
- The gateway is deployed via Helm on the Kind cluster; no external gateway infrastructure is required.
- The Green version is created by deploying the same order service Docker image with a different version identifier (e.g., response header or environment variable), not by modifying application code.
- The Kind cluster is a single-node cluster matching the existing PoC pattern.
- The gateway uses its Admin API for dynamic configuration; no dashboard UI is required for the PoC.
- Jaeger is deployed alongside the gateway to validate end-to-end tracing through the gateway.
- Kafka and all supporting services (product, inventory, payment, notification) are deployed once — only the order service has Blue/Green versions.
- TLS is not required for PoC purposes; all traffic is HTTP.
- The existing `docker-compose.yml` and `k8s/` directory (from Feature 002) remain unchanged; this feature adds a separate APISIX-focused deployment.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Traffic distribution matches configured weights within a 5% margin over 100 requests (e.g., 90/10 config produces 85-95% Blue and 5-15% Green).
- **SC-002**: Header-based routing correctly directs 100% of canary-header requests to the Green version and 100% of non-header requests to the default weighted routing.
- **SC-003**: A full traffic cutover from Blue to Green (or rollback from Green to Blue) takes effect within 5 seconds of the configuration change, with zero failed requests during the transition.
- **SC-004**: Distributed traces in Jaeger show complete span chains through the gateway for all 5 test scenarios (Happy Path, Inventory Shortage, Payment Timeout, Kafka Async, Kafka DLT).
- **SC-005**: The complete environment setup (Kind cluster + gateway + both versions + supporting services) deploys and reaches Ready status within 6 minutes.
- **SC-006**: The environment teardown completes within 30 seconds.
