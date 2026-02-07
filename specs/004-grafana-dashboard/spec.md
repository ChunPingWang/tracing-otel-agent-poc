# Feature Specification: Grafana Monitoring Dashboard

**Feature Branch**: `004-grafana-dashboard`
**Created**: 2026-02-07
**Status**: Draft
**Input**: User description: "add Grafana dashboard for monitoring the microservices"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Service Health Overview Dashboard (Priority: P1)

As an SRE engineer, I want to open a Grafana dashboard and immediately see the health status of all 5 microservices (request rates, error rates, response latencies), so I can quickly identify which service is degraded or down.

**Why this priority**: A health overview is the foundational monitoring capability. Without it, operators have no visibility into system behavior. It delivers immediate value for any deployment scenario.

**Independent Test**: Can be fully tested by deploying the environment, sending several requests to the order API, then opening Grafana and verifying that all 5 services appear with request rate, error rate, and latency panels populated with data.

**Acceptance Scenarios**:

1. **Given** all 5 microservices are running and instrumented, **When** the operator opens the Service Health Overview dashboard in Grafana, **Then** the dashboard shows panels for each service with request rate (req/s), error rate (%), and response latency (p50, p95, p99).
2. **Given** the dashboard is open, **When** 10 order requests are sent through the system, **Then** the dashboard updates to reflect the new traffic within 30 seconds.
3. **Given** the Payment Service is configured to simulate 5-second delays, **When** the operator views the dashboard, **Then** the Payment Service latency panel clearly shows elevated p95/p99 values compared to other services.
4. **Given** a request results in an error (e.g., inventory shortage), **When** the operator views the dashboard, **Then** the error rate panel for the affected service shows the increase.

---

### User Story 2 - JVM & Runtime Metrics Dashboard (Priority: P2)

As an SRE engineer, I want to see JVM-level metrics (heap memory usage, GC activity, thread counts) for each microservice, so I can detect memory leaks, excessive GC pauses, or thread pool exhaustion before they cause outages.

**Why this priority**: JVM metrics complement the service-level view. After confirming services are healthy at the request level, operators need deeper runtime visibility for capacity planning and proactive issue detection.

**Independent Test**: Can be tested by opening the JVM dashboard after deploying the environment and verifying that heap usage, GC counts, and thread count panels display data for all 5 services.

**Acceptance Scenarios**:

1. **Given** all services are running, **When** the operator opens the JVM Metrics dashboard, **Then** panels display heap memory used/committed/max, non-heap memory, GC pause count and duration, and live thread count for each service.
2. **Given** the JVM dashboard is open, **When** 100 rapid requests are sent to generate load, **Then** heap memory usage visibly increases and GC activity is reflected in the GC panels.
3. **Given** the dashboard allows service selection, **When** the operator selects "order-service" from a dropdown, **Then** only order-service JVM metrics are displayed.

---

### User Story 3 - Kafka Messaging Metrics Dashboard (Priority: P2)

As an SRE engineer, I want to see Kafka producer and consumer metrics (message rates, consumer lag, processing times), so I can monitor the health of the asynchronous notification pipeline and detect message processing failures or backpressure.

**Why this priority**: The Kafka async path (order-confirmed → notification-service) is a critical part of the order flow. Monitoring Kafka metrics ensures the async pipeline is healthy and messages are not being lost or delayed.

**Independent Test**: Can be tested by sending orders, then checking the Kafka dashboard to verify producer send rates, consumer receive rates, and consumer lag panels have data.

**Acceptance Scenarios**:

1. **Given** the Kafka dashboard is open, **When** 10 orders are placed, **Then** the producer panel shows message send rate for the `order-confirmed` topic and the consumer panel shows messages consumed by `notification-group`.
2. **Given** notification-service failure simulation is enabled, **When** orders are placed, **Then** the consumer lag panel shows increasing lag for the `notification-group` consumer group.

---

### User Story 4 - One-Command Setup with Grafana (Priority: P1)

As an evaluator, I want Grafana (with pre-configured dashboards and data source) to be automatically deployed as part of the existing setup process, so I can start monitoring immediately without manual configuration.

**Why this priority**: If Grafana requires manual setup (adding data sources, importing dashboards), adoption friction is too high for a PoC. Automatic provisioning is essential.

**Independent Test**: Can be tested by running the setup command and verifying Grafana is accessible with dashboards pre-loaded and showing data — no manual configuration needed.

**Acceptance Scenarios**:

1. **Given** Docker and prerequisites are installed, **When** the user runs `docker-compose up --build -d`, **Then** Grafana starts alongside all other services, accessible at a designated port with pre-configured data source and dashboards.
2. **Given** the APISIX Kind cluster setup is used, **When** the user runs `./scripts/apisix-deploy.sh`, **Then** Grafana is deployed in the cluster with pre-configured dashboards accessible from the host.
3. **Given** Grafana is deployed, **When** the user opens the Grafana URL, **Then** dashboards are immediately available without needing to log in with credentials or manually import anything.

---

### User Story 5 - Database Query Metrics (Priority: P3)

As a developer, I want to see database query metrics (query count, query duration) per service, so I can identify slow queries or excessive database calls that degrade performance.

**Why this priority**: Database metrics provide the deepest layer of observability. After service health and JVM metrics are confirmed working, DB metrics complete the full-stack monitoring picture.

**Independent Test**: Can be tested by sending order requests and verifying the database panel shows query counts and durations for each service's H2 database operations.

**Acceptance Scenarios**:

1. **Given** the database metrics panel is available, **When** 10 orders are placed, **Then** the panel shows JDBC connection pool usage and query execution counts/durations for each service.

---

### Edge Cases

- What happens when Grafana starts before the metrics backend has data? Dashboards should display gracefully with "No data" indicators rather than errors.
- What happens when a service is stopped? The dashboard should reflect the absence of data for that service (flat line or gap) rather than showing stale data as current.
- What happens when the metrics backend is unreachable? Grafana should show a data source error message; the microservices should continue operating normally (metrics export failure must not affect business logic).
- What happens when the metrics volume is high (e.g., 1000 req/s)? The metrics pipeline should aggregate and downsample appropriately to avoid excessive storage or Grafana query slowness.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST collect and store metrics from all 5 microservices using the existing OpenTelemetry Java Agent instrumentation, without requiring application code changes.
- **FR-002**: System MUST provide a Grafana instance with at least 3 pre-configured dashboards: Service Health Overview, JVM Metrics, and Kafka Metrics.
- **FR-003**: All Grafana dashboards and data sources MUST be auto-provisioned at startup — no manual configuration required.
- **FR-004**: The Service Health Overview dashboard MUST display request rate, error rate, and response latency percentiles (p50, p95, p99) for each of the 5 microservices.
- **FR-005**: The JVM Metrics dashboard MUST display heap memory usage, GC activity (count and duration), and thread counts, with a service selector dropdown.
- **FR-006**: The Kafka Metrics dashboard MUST display producer send rates, consumer receive rates, and consumer group lag for the order-confirmed topic.
- **FR-007**: Grafana MUST be integrated into both deployment modes: Docker Compose and APISIX Kind Kubernetes cluster.
- **FR-008**: Grafana MUST be accessible from the host machine without authentication (anonymous access enabled for PoC).
- **FR-009**: The metrics pipeline MUST NOT require changes to existing microservice application code or Docker images — only infrastructure configuration changes (docker-compose.yml, environment variables, Kubernetes manifests).
- **FR-010**: The existing OTel Java Agent `OTEL_METRICS_EXPORTER` setting MUST be changed from `none` to enable metrics export.
- **FR-011**: Dashboard panels MUST auto-refresh at a configurable interval (default: 10 seconds) so operators see near-real-time data.
- **FR-012**: System MUST provide a teardown process that cleanly removes all Grafana and metrics backend components alongside existing cleanup.

### Key Entities

- **Metrics Backend**: The time-series database that receives, stores, and serves metrics data for Grafana queries.
- **Dashboard**: A Grafana dashboard definition (JSON) containing panels, queries, and layout for a specific monitoring concern.
- **Data Source**: The Grafana configuration that connects to the metrics backend for querying stored metrics.
- **Panel**: A single visualization within a dashboard (graph, gauge, stat, table) displaying one or more metric queries.

## Assumptions

- The OpenTelemetry Java Agent 1.32.1 already collects HTTP server/client metrics, JVM metrics, JDBC metrics, and Kafka metrics — they are just not being exported because `OTEL_METRICS_EXPORTER` is set to `none`.
- Enabling metrics export requires only changing the `OTEL_METRICS_EXPORTER` environment variable and pointing it at a metrics receiver — no code changes.
- Grafana dashboards are provisioned via Grafana's file-based provisioning system (JSON files mounted into the container).
- The metrics backend uses in-memory or ephemeral storage suitable for a PoC — no persistent storage required.
- Grafana uses anonymous access (no login) for PoC simplicity.
- Dashboard JSON files are stored in the project repository alongside other deployment artifacts.
- The existing docker-compose.yml is modified in-place to add Grafana and metrics backend services; no separate compose file is created.
- For the APISIX Kind cluster deployment, Grafana and metrics backend are added as additional Kubernetes manifests in the existing `apisix-k8s/` directory.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: All 3 dashboards (Service Health, JVM, Kafka) display live data within 60 seconds of sending the first request through the system.
- **SC-002**: The Service Health Overview dashboard correctly reflects traffic distribution — when 100 requests are sent, the request count panels show approximately 100 total requests distributed across the relevant services.
- **SC-003**: Latency anomalies are visible — when Payment Service delay simulation is enabled (5 seconds), the p95 latency panel for payment-service shows values greater than 4 seconds within 30 seconds.
- **SC-004**: The complete environment (including Grafana and metrics) deploys and becomes functional with zero manual configuration steps beyond the existing setup commands.
- **SC-005**: Enabling metrics export does not increase average request response time by more than 5% compared to metrics-disabled baseline.
- **SC-006**: The environment teardown cleanly removes all Grafana and metrics components within the existing teardown time targets.
