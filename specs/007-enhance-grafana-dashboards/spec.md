# Feature Specification: Enhance Existing Grafana Dashboards

**Feature Branch**: `007-enhance-grafana-dashboards`
**Created**: 2026-02-07
**Status**: Draft
**Input**: User description: "Enhance existing Grafana dashboards with DB connection pool details, per-endpoint breakdown, JVM memory pool details, class loading, and Kafka DLT monitoring"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Service Health: Per-Endpoint Request Breakdown (Priority: P1)

As an SRE engineer, I want to see request rate and latency broken down by individual HTTP endpoint (e.g., `/api/orders`, `/api/products/{id}`), so I can identify which specific endpoint is slow or error-prone, rather than only seeing aggregate per-service metrics.

**Why this priority**: The current Service Health dashboard only shows aggregate metrics per service. When a service has multiple endpoints, operators cannot pinpoint which endpoint is causing elevated latency or errors. Per-endpoint visibility is the most impactful enhancement for root cause analysis.

**Independent Test**: Can be tested by sending requests to different endpoints (POST /api/orders, GET /api/products/P001), then verifying the dashboard shows separate series for each endpoint with correct request rate and latency values.

**Acceptance Scenarios**:

1. **Given** the Service Health Overview dashboard is open, **When** the operator views the new "Request Rate by Endpoint" panel, **Then** each HTTP endpoint (e.g., `POST /api/orders`, `GET /api/products/{id}`) appears as a separate series with its own request rate.
2. **Given** 10 orders are placed (which generates calls to multiple downstream endpoints), **When** the operator views the "Latency by Endpoint" panel, **Then** latency values are shown per endpoint, allowing comparison between fast endpoints (e.g., product query) and slower ones (e.g., payment processing).
3. **Given** Payment Service delay simulation is enabled, **When** the operator views the per-endpoint panels, **Then** the `POST /api/payments` endpoint shows elevated latency while other endpoints remain normal.

---

### User Story 2 - JVM: Memory Pool Detail & Class Loading (Priority: P2)

As an SRE engineer, I want to see per-memory-pool JVM metrics (Eden Space, Survivor Space, Old Gen, Metaspace) and class loading statistics, so I can diagnose memory fragmentation, detect Metaspace leaks, and understand class loading behavior across services.

**Why this priority**: The current JVM dashboard shows only aggregate heap/non-heap totals. Per-pool breakdown is essential for diagnosing specific GC pressure (e.g., Old Gen filling up while Eden is fine), Metaspace growth, and class loading issues that affect long-running services.

**Independent Test**: Can be tested by deploying the environment, sending requests, and verifying the dashboard shows separate memory pool series (Eden, Survivor, Old Gen, Metaspace) and class loaded/unloaded counts.

**Acceptance Scenarios**:

1. **Given** the JVM Metrics dashboard is open, **When** the operator views the "Memory Pool Usage" panel, **Then** individual memory pools (Eden Space, Survivor Space, Tenured/Old Gen, Metaspace, Code Cache) are shown as separate series with used bytes.
2. **Given** the dashboard has a service selector, **When** the operator selects "order-service", **Then** only order-service memory pools are displayed.
3. **Given** the JVM Metrics dashboard is open, **When** the operator views the "Class Loading" panel, **Then** total loaded classes and total unloaded classes are displayed as time series.

---

### User Story 3 - Kafka: DLT Monitoring & Per-Partition Consumer Lag (Priority: P2)

As an SRE engineer, I want to see Dead Letter Topic (DLT) message activity and per-partition consumer lag details, so I can detect failed message processing and identify specific partitions with processing bottlenecks.

**Why this priority**: The current Kafka dashboard shows only aggregate consumer lag. DLT monitoring is critical because DLT messages represent orders that failed all retry attempts — a business-impacting event. Per-partition lag helps identify uneven processing.

**Independent Test**: Can be tested by enabling notification-service failure simulation, sending orders (which triggers DLT routing after retries), then verifying the dashboard shows DLT producer activity and partition-level lag.

**Acceptance Scenarios**:

1. **Given** the Kafka Metrics dashboard is open and notification-service failure simulation is enabled, **When** 5 orders are placed and retries are exhausted, **Then** the "DLT Messages" panel shows message send activity to the `order-confirmed.DLT` topic.
2. **Given** the Kafka dashboard is open, **When** the operator views the "Consumer Lag by Partition" panel, **Then** lag is shown per partition rather than only as an aggregate max.
3. **Given** the Kafka dashboard currently has no service selector, **When** this enhancement is applied, **Then** a `service` template variable is added for filtering Kafka metrics by service name.

---

### Edge Cases

- What happens when a memory pool name is not available (e.g., G1 GC uses different pool names than Parallel GC)? Panels should display whatever pool names the JVM reports, since the instrumentation exposes the `pool` label with actual JVM pool names.
- What happens when no DLT messages have been produced? The DLT panel should show "No data" or a flat zero line rather than an error.
- What happens when a service has only one endpoint? The per-endpoint panel should still work correctly, showing that single endpoint.
- What happens when the message broker has only 1 partition? The per-partition lag panel should show 1 partition's data.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The Service Health Overview dashboard MUST add a "Request Rate by Endpoint" panel showing request rate broken down by HTTP method and route (e.g., `POST /api/orders`, `GET /api/products/{id}`).
- **FR-002**: The Service Health Overview dashboard MUST add a "Latency by Endpoint" panel showing p95 response latency broken down by HTTP method and route.
- **FR-003**: The JVM Metrics dashboard MUST add a "Memory Pool Usage" panel showing used bytes for each JVM memory pool (identified by pool name label).
- **FR-004**: The JVM Metrics dashboard MUST add a "Class Loading" panel showing total loaded classes and total unloaded classes over time.
- **FR-005**: The Kafka Metrics dashboard MUST add a "DLT Messages" panel showing message send rate to any topic matching the Dead Letter Topic naming pattern.
- **FR-006**: The Kafka Metrics dashboard MUST add a "Consumer Lag by Partition" panel showing lag per topic-partition rather than only aggregate max.
- **FR-007**: The Kafka Metrics dashboard MUST add a service selector (matching the existing pattern in Service Health and JVM dashboards) for filtering by service name.
- **FR-008**: All new panels MUST respect the existing service selector filter.
- **FR-009**: All enhancements MUST be applied to both Docker Compose and Kubernetes (Kind) deployments — updating the dashboard definition files and corresponding deployment manifests accordingly.
- **FR-010**: Existing panels MUST NOT be removed or have their behavior changed — enhancements are purely additive.

### Key Entities

- **Endpoint Panel**: A dashboard panel that breaks down metrics by the combination of HTTP method + route, enabling per-endpoint visibility within a service.
- **Memory Pool**: A specific region of JVM memory (e.g., Eden Space, Old Gen, Metaspace) tracked individually via the pool label in instrumentation metrics.
- **Dead Letter Topic (DLT)**: A message broker topic suffixed with `.DLT` where messages are sent after all consumer retry attempts are exhausted.

## Assumptions

- The existing instrumentation agent already exports HTTP method and route labels on HTTP server metrics — these are available for per-endpoint breakdown without any configuration changes.
- JVM memory pool metrics are exported with a pool label containing the JVM-specific pool name (e.g., `PS Eden Space`, `PS Old Gen`, `Metaspace`).
- Class loading metrics are exported by the instrumentation agent as current loaded class count and unloaded class count.
- DLT messages are produced by notification-service using the standard retry mechanism, and the instrumentation agent captures these producer operations — meaning message producer metrics will include sends to DLT topics.
- The existing dashboard definition structure supports adding new panels by appending to the panels collection with appropriate positioning.
- The Kubernetes deployment manifest for dashboards embeds the dashboard definitions and must be regenerated when dashboard files change.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: After sending 10 orders, the per-endpoint request rate panel shows at least 4 distinct endpoints (e.g., order creation, product query, inventory reserve, payment processing) with non-zero request rates within 60 seconds.
- **SC-002**: When payment delay simulation is enabled, the per-endpoint latency panel shows the payment endpoint p95 latency exceeding 4 seconds while other endpoints remain below 1 second.
- **SC-003**: The JVM Memory Pool panel shows at least 3 distinct pools (across heap and non-heap regions) per service within 60 seconds of dashboard load.
- **SC-004**: After triggering notification failure simulation and sending 5 orders, the DLT Messages panel shows non-zero send rate to the Dead Letter Topic within 2 minutes.
- **SC-005**: All new panels display data correctly in both Docker Compose and Kubernetes (Kind) deployments.
- **SC-006**: Existing panels continue to display correctly with no visual or data regression after enhancements are applied.
