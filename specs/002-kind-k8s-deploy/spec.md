# Feature Specification: Deploy PoC to Kubernetes on Kind

**Feature Branch**: `002-kind-k8s-deploy`
**Created**: 2026-02-07
**Status**: Draft
**Input**: User description: "add the requirement which deploys this poc to the k8s on Kind. And update PRD.md and TECH.md if it needs."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - One-Command Kind Cluster Deployment (Priority: P1)

As an evaluator, I want to run a single command that creates a Kind cluster, builds all service images, deploys the entire PoC stack (5 microservices + Kafka + Jaeger + Ingress Controller), and exposes the necessary endpoints via Ingress, so I can immediately start testing the distributed tracing scenarios without manual configuration.

**Why this priority**: This is the foundational capability. Without a working Kind deployment, nothing else in this feature can be demonstrated. It represents the minimum viable product for Kubernetes-based PoC evaluation.

**Independent Test**: Can be fully tested by running the deployment script and verifying that all pods (5 services + Kafka + Jaeger + Ingress Controller) reach Running/Ready status, and the Jaeger UI is accessible from the host machine via Ingress.

**Acceptance Scenarios**:

1. **Given** Kind and kubectl are installed on the host, **When** the user runs the deployment script, **Then** a Kind cluster is created with Ingress support, all container images are built and loaded, all Kubernetes resources (including Ingress Controller and Ingress rules) are applied, and all pods reach Ready status within 5 minutes.
2. **Given** the Kind cluster is fully deployed, **When** the user opens the Jaeger UI URL via Ingress (e.g., http://jaeger.localhost), **Then** the Jaeger query page is accessible and lists all 5 service names.
3. **Given** the Kind cluster is fully deployed, **When** the user sends a POST request to the Order Service endpoint via Ingress (e.g., http://api.localhost/api/orders), **Then** the order is processed successfully and a complete trace appears in Jaeger UI.

---

### User Story 2 - All Five Test Scenarios Work on Kind (Priority: P2)

As an SRE engineer, I want all five existing test scenarios (Happy Path, Inventory Shortage, Payment Timeout, Kafka Async Notification, Kafka DLT) to work identically on the Kind cluster as they do on Docker Compose, so the PoC evaluation results are consistent across deployment targets.

**Why this priority**: The core value of this feature is proving the PoC works on Kubernetes. If the test scenarios fail, the Kind deployment has no practical value for PoC evaluation.

**Independent Test**: Can be tested by running each of the 5 curl-based test scenarios against the Ingress-exposed endpoints and verifying the expected HTTP responses and Jaeger trace structures.

**Acceptance Scenarios**:

1. **Given** the Kind cluster is running, **When** the user executes the Happy Path scenario (POST /api/orders with valid data), **Then** the response contains status "CONFIRMED" and a Jaeger trace shows spans across all 5 services.
2. **Given** the Kind cluster is running, **When** the user executes the inventory shortage scenario, **Then** the order status is "FAILED" and the trace shows an error on the inventory-service span.
3. **Given** the Kind cluster is running, **When** the user triggers the payment timeout scenario, **Then** the order status is "PAYMENT_TIMEOUT" and the trace shows payment-service span duration > 3s.
4. **Given** the Kind cluster is running with Kafka, **When** the user places a successful order, **Then** the Kafka async notification is consumed and the trace includes notification-service spans.
5. **Given** the Notification Service failure simulation is enabled, **When** the user places an order, **Then** the consumer retries 3 times and messages are sent to the DLT topic, all visible in the trace.

---

### User Story 3 - Clean Teardown and Repeatable Lifecycle (Priority: P3)

As a developer, I want to be able to tear down the Kind cluster completely and recreate it from scratch, so I can run the PoC evaluation repeatably in a clean environment.

**Why this priority**: Repeatability is important for PoC evaluations but is secondary to the core deployment and scenario validation.

**Independent Test**: Can be tested by running the teardown command, verifying the cluster is removed, then running the setup command again and confirming all services are operational.

**Acceptance Scenarios**:

1. **Given** a running Kind cluster with the PoC deployed, **When** the user runs the teardown command, **Then** the Kind cluster and all associated resources are completely removed.
2. **Given** a previously torn down environment, **When** the user runs the setup command again, **Then** the cluster is recreated and all services reach Ready status, identical to the first deployment.

---

### User Story 4 - Kubernetes Resource Visibility (Priority: P3)

As a platform architect, I want the Kubernetes manifests to follow standard practices (Deployments, Services, ConfigMaps) and be organized clearly, so I can evaluate the deployment model for production readiness.

**Why this priority**: Important for production evaluation but does not affect PoC functionality.

**Independent Test**: Can be tested by reviewing the manifest files against Kubernetes best practices and verifying that kubectl commands show proper resource types and labels.

**Acceptance Scenarios**:

1. **Given** the Kind cluster is deployed, **When** the user runs `kubectl get all -n ecommerce`, **Then** all resources are visible with consistent naming and labeling.
2. **Given** the Kubernetes manifests, **When** reviewed by a platform architect, **Then** each microservice uses a Deployment + Service pair, OTel Agent configuration is externalized via environment variables, and service discovery uses Kubernetes DNS.

---

### User Story 5 - Ingress-Based External Access (Priority: P2)

As a platform architect, I want all externally accessible services (Order Service API, Jaeger UI, Payment/Notification admin endpoints) to be exposed via Kubernetes Ingress with host-based routing (e.g., `api.localhost`, `jaeger.localhost`), so the deployment follows production-like traffic routing patterns instead of relying on NodePort.

**Why this priority**: Ingress is the standard Kubernetes pattern for external traffic routing. Using Ingress instead of NodePort demonstrates a production-ready approach and validates that the OTel tracing works correctly when requests pass through an Ingress Controller.

**Independent Test**: Can be tested by sending HTTP requests to Ingress hostnames/paths from the host machine and verifying correct routing to backend services.

**Acceptance Scenarios**:

1. **Given** the Kind cluster is deployed with an Ingress Controller, **When** the user sends a request to the Ingress hostname for Order Service, **Then** the request is routed to the Order Service and returns a valid response.
2. **Given** the Ingress is configured, **When** the user accesses the Jaeger UI hostname, **Then** the Jaeger UI is accessible and functional.
3. **Given** the Ingress is configured, **When** the user sends a request to the Payment Service admin endpoint via Ingress, **Then** the delay simulation is activated successfully.
4. **Given** the Ingress is configured, **When** a traced request passes through the Ingress Controller, **Then** the distributed trace in Jaeger still shows a complete span chain (the Ingress Controller does not break trace context propagation).

---

### Edge Cases

- What happens when Kind is not installed? The setup script must detect and report a clear error message with installation instructions.
- What happens when Docker is not running? The setup script must detect and report a meaningful error.
- What happens when ports required by Kind are already in use? The setup script should use Kind's extraPortMappings to map to configurable host ports or report the conflict.
- What happens when a pod fails to start (e.g., image build error)? The deployment script should detect non-Ready pods after a timeout and report which pod failed with pod events.
- What happens when the user runs setup twice without teardown? The script should detect an existing cluster and either skip creation or prompt for teardown first.
- What happens when the Ingress Controller pod is not yet ready when Ingress rules are applied? The deployment script must wait for the Ingress Controller to be ready before applying Ingress resources.
- What happens when localhost DNS resolution does not support *.localhost subdomains? The setup documentation must include /etc/hosts entries or alternative access methods.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST provide a Kind cluster configuration file that defines a single-node cluster with port mappings for HTTP (80) and HTTPS (443) to support Ingress Controller traffic.
- **FR-002**: System MUST provide Kubernetes manifests for all components: 5 microservices, Kafka broker, Jaeger all-in-one, and Ingress resources.
- **FR-003**: Each microservice manifest MUST configure the OpenTelemetry Java Agent via environment variables (JAVA_TOOL_OPTIONS, OTEL_SERVICE_NAME, OTEL_EXPORTER_OTLP_ENDPOINT, etc.), matching the existing Docker Compose configuration.
- **FR-004**: System MUST provide a deployment script that automates the full lifecycle: create Kind cluster, install Ingress Controller, build Docker images, load images into Kind, apply Kubernetes manifests, and wait for all pods to be Ready.
- **FR-005**: System MUST provide a teardown script that deletes the Kind cluster and cleans up all associated resources.
- **FR-006**: Inter-service communication MUST use Kubernetes DNS-based service discovery (e.g., `http://product-service:8082`).
- **FR-007**: System MUST deploy an Ingress Controller (e.g., NGINX Ingress Controller) as part of the automated setup.
- **FR-008**: System MUST provide Ingress resources that route external HTTP traffic to backend services using host-based routing rules (e.g., `api.localhost`, `jaeger.localhost`, `payment.localhost`, `notification.localhost`).
- **FR-009**: Jaeger UI MUST be accessible from the host machine via Ingress.
- **FR-010**: Order Service MUST be accessible from the host machine via Ingress for executing test scenarios.
- **FR-011**: Payment Service admin endpoint (simulate-delay) and Notification Service admin endpoint (simulate-failure) MUST be accessible from the host machine via Ingress for test scenario execution.
- **FR-012**: Kafka broker MUST be accessible to all microservices within the cluster via Kubernetes DNS.
- **FR-013**: The deployment MUST work with Kind v0.20+ and kubectl v1.28+.
- **FR-014**: All Kubernetes manifests MUST be organized in a dedicated directory at the project root.
- **FR-015**: System MUST provide a deployment verification step that checks all pods (including Ingress Controller) are Running/Ready and all Ingress endpoints are reachable.
- **FR-016**: The Ingress Controller MUST NOT break W3C Trace Context propagation (traceparent header must be forwarded to backend services).
- **FR-017**: All PoC resources (Deployments, Services, Ingress, ConfigMaps) MUST be deployed in a dedicated `ecommerce` namespace. The Ingress Controller may reside in its own namespace (e.g., `ingress-nginx`) per standard convention.

### Key Entities

- **Kind Cluster Configuration**: Defines the local Kubernetes cluster topology and port mappings (80/443) for Ingress Controller traffic.
- **Kubernetes Deployment**: Represents each microservice as a workload unit with container spec, environment variables, and readiness probes.
- **Kubernetes Service**: Provides stable ClusterIP network endpoints for inter-service communication.
- **Kubernetes Ingress**: Defines host-based routing rules that map external hostnames (e.g., `api.localhost`, `jaeger.localhost`) to backend Services, providing the external access layer.
- **Ingress Controller**: The runtime component (e.g., NGINX) that implements the Ingress rules and handles external HTTP traffic.
- **Deployment Script**: Orchestrates the end-to-end cluster lifecycle from creation to verification.

## Clarifications

### Session 2026-02-07

- Q: Ingress routing strategy — host-based or path-based? → A: Host-based routing (e.g., `api.localhost`, `jaeger.localhost`, `payment.localhost`)
- Q: Kubernetes namespace for PoC resources? → A: Dedicated namespace `ecommerce`

## Assumptions

- Kind is the target local Kubernetes distribution; other distributions (Minikube, k3s) are out of scope.
- The existing Docker images (multi-stage builds with OTel Agent) are reused without modification; only Kubernetes manifests and deployment scripts are new.
- Single-node Kind cluster is sufficient for PoC purposes; multi-node configurations are out of scope.
- The host machine has Docker, Kind, and kubectl pre-installed; the deployment script validates but does not install these tools.
- NGINX Ingress Controller is deployed as the Ingress implementation; other Ingress controllers (Traefik, HAProxy, etc.) are out of scope.
- The Ingress Controller is installed from the official Kind-compatible NGINX Ingress manifest as part of the deployment script.
- TLS/HTTPS is not required for PoC purposes; Ingress routes use HTTP only.
- Kafka runs as a single-broker deployment (matching the existing Docker Compose setup) without persistent storage.
- Jaeger runs in all-in-one mode with in-memory storage (matching the existing Docker Compose setup).
- The existing `docker-compose.yml` remains unchanged; the Kind deployment is an additional deployment option, not a replacement.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: All pods (5 microservices + Kafka + Jaeger + Ingress Controller) reach Running/Ready status within 5 minutes of running the deployment script.
- **SC-002**: All 5 existing test scenarios (Happy Path, Inventory Shortage, Payment Timeout, Kafka Async, Kafka DLT) produce identical results via Ingress endpoints on Kind as on Docker Compose.
- **SC-003**: A complete teardown and redeploy cycle (destroy cluster, recreate, all pods ready) completes within 8 minutes.
- **SC-004**: The deployment process requires no more than 2 commands: one to deploy and one to tear down.
- **SC-005**: Jaeger UI traces on the Kind deployment show the same span structure (5 services, HTTP + Kafka + JDBC spans) as the Docker Compose deployment.
- **SC-006**: All external access to services goes through Ingress; no NodePort is required for test scenario execution.
