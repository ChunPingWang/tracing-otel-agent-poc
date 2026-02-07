# Tasks: E-Commerce Distributed Tracing PoC (OTel Agent)

**Input**: Design documents from `/specs/001-otel-distributed-tracing/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: TDD is NON-NEGOTIABLE per project constitution (Principle IV). All tests MUST be written FIRST (Red-Green-Refactor).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- Multi-module microservices: `{service-name}/src/main/java/com/ecommerce/{service}/`
- Tests: `{service-name}/src/test/java/com/ecommerce/{service}/`
- Each service follows hexagonal architecture: `domain/` → `application/` → `infrastructure/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization, Maven project structure, shared configuration

- [x] T001 Create root project directory structure with 5 service directories (order-service/, product-service/, inventory-service/, payment-service/, notification-service/)
- [x] T002 [P] Initialize order-service Maven project with pom.xml (Spring Boot 2.7.18, Spring Web, Spring Data JPA, Spring Kafka, H2) in order-service/pom.xml
- [x] T003 [P] Initialize product-service Maven project with pom.xml (Spring Boot 2.7.18, Spring Web, Spring Data JPA, H2) in product-service/pom.xml
- [x] T004 [P] Initialize inventory-service Maven project with pom.xml (Spring Boot 2.7.18, Spring Web, Spring Data JPA, H2) in inventory-service/pom.xml
- [x] T005 [P] Initialize payment-service Maven project with pom.xml (Spring Boot 2.7.18, Spring Web, Spring Data JPA, H2) in payment-service/pom.xml
- [x] T006 [P] Initialize notification-service Maven project with pom.xml (Spring Boot 2.7.18, Spring Web, Spring Data JPA, Spring Kafka, H2) in notification-service/pom.xml
- [x] T007 [P] Create hexagonal architecture package structure for all 5 services (domain/model, domain/event, domain/service, domain/port, application/service, application/port/in, application/port/out, application/dto, application/mapper, infrastructure/adapter/in/rest, infrastructure/adapter/out/persistence, infrastructure/config, infrastructure/dto, infrastructure/mapper)
- [x] T008 [P] Add test dependencies to all pom.xml files (JUnit 5, Spring Boot Test, spring-kafka-test for order/notification services, ArchUnit for all services)
- [x] T009 [P] Configure Checkstyle plugin in all pom.xml files with method length ≤ 20 lines and cyclomatic complexity ≤ 10 rules
- [x] T010 [P] Create Spring Boot application.yml for each service with server port, H2 console, JPA DDL-auto settings (order:8081, product:8082, inventory:8083, payment:8084, notification:8085)
- [x] T011 [P] Create Spring Boot main application class for each service (OrderApplication.java, ProductApplication.java, etc.)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core domain models, port interfaces, and architecture tests shared across all user stories

**CRITICAL**: No user story work can begin until this phase is complete

### Architecture Tests

- [x] T012 [P] Write ArchUnit test to enforce hexagonal layering constraints (domain MUST NOT depend on application/infrastructure, application MUST NOT depend on infrastructure) in order-service/src/test/java/com/ecommerce/order/ArchitectureTest.java
- [x] T013 [P] Write ArchUnit test for product-service in product-service/src/test/java/com/ecommerce/product/ArchitectureTest.java
- [x] T014 [P] Write ArchUnit test for inventory-service in inventory-service/src/test/java/com/ecommerce/inventory/ArchitectureTest.java
- [x] T015 [P] Write ArchUnit test for payment-service in payment-service/src/test/java/com/ecommerce/payment/ArchitectureTest.java
- [x] T016 [P] Write ArchUnit test for notification-service in notification-service/src/test/java/com/ecommerce/notification/ArchitectureTest.java

### Domain Models (all services)

- [x] T017 [P] Write unit test for Order domain model (creation, state transitions) in order-service/src/test/java/com/ecommerce/order/domain/model/OrderTest.java
- [x] T018 [P] Implement Order aggregate root with OrderItem, OrderStatus (CREATED/CONFIRMED/FAILED/PAYMENT_TIMEOUT), CustomerId value object in order-service/src/main/java/com/ecommerce/order/domain/model/
- [x] T019 [P] Write unit test for Product domain model in product-service/src/test/java/com/ecommerce/product/domain/model/ProductTest.java
- [x] T020 [P] Implement Product entity in product-service/src/main/java/com/ecommerce/product/domain/model/Product.java
- [x] T021 [P] Write unit test for Inventory domain model (reserve, release, insufficient stock) in inventory-service/src/test/java/com/ecommerce/inventory/domain/model/InventoryTest.java
- [x] T022 [P] Implement Inventory entity with reserve()/release() domain logic in inventory-service/src/main/java/com/ecommerce/inventory/domain/model/Inventory.java
- [x] T023 [P] Write unit test for Payment domain model in payment-service/src/test/java/com/ecommerce/payment/domain/model/PaymentTest.java
- [x] T024 [P] Implement Payment entity with PaymentStatus (SUCCESS/FAILED) in payment-service/src/main/java/com/ecommerce/payment/domain/model/
- [x] T025 [P] Write unit test for Notification domain model in notification-service/src/test/java/com/ecommerce/notification/domain/model/NotificationTest.java
- [x] T026 [P] Implement Notification entity with NotificationStatus (SENT/FAILED) and Customer entity in notification-service/src/main/java/com/ecommerce/notification/domain/model/

### Domain Events

- [x] T027 [P] Implement OrderConfirmedEvent domain event in order-service/src/main/java/com/ecommerce/order/domain/event/OrderConfirmedEvent.java

### Port Interfaces (all services)

- [x] T028 [P] Define OrderRepository port interface in order-service/src/main/java/com/ecommerce/order/domain/port/OrderRepository.java
- [x] T029 [P] Define ProductRepository port interface in product-service/src/main/java/com/ecommerce/product/domain/port/ProductRepository.java
- [x] T030 [P] Define InventoryRepository port interface in inventory-service/src/main/java/com/ecommerce/inventory/domain/port/InventoryRepository.java
- [x] T031 [P] Define PaymentRepository port interface in payment-service/src/main/java/com/ecommerce/payment/domain/port/PaymentRepository.java
- [x] T032 [P] Define NotificationRepository and CustomerRepository port interfaces in notification-service/src/main/java/com/ecommerce/notification/domain/port/

### JPA Persistence Adapters (all services)

- [x] T033 [P] Write integration test for JPA order repository in order-service/src/test/java/com/ecommerce/order/infrastructure/adapter/out/persistence/JpaOrderRepositoryIntegrationTest.java
- [x] T034 [P] Implement OrderJpaEntity and JpaOrderRepository adapter in order-service/src/main/java/com/ecommerce/order/infrastructure/adapter/out/persistence/
- [x] T035 [P] Write integration test for JPA product repository in product-service/src/test/java/com/ecommerce/product/infrastructure/adapter/out/persistence/JpaProductRepositoryIntegrationTest.java
- [x] T036 [P] Implement ProductJpaEntity and JpaProductRepository adapter in product-service/src/main/java/com/ecommerce/product/infrastructure/adapter/out/persistence/
- [x] T037 [P] Write integration test for JPA inventory repository in inventory-service/src/test/java/com/ecommerce/inventory/infrastructure/adapter/out/persistence/JpaInventoryRepositoryIntegrationTest.java
- [x] T038 [P] Implement InventoryJpaEntity and JpaInventoryRepository adapter in inventory-service/src/main/java/com/ecommerce/inventory/infrastructure/adapter/out/persistence/
- [x] T039 [P] Write integration test for JPA payment repository in payment-service/src/test/java/com/ecommerce/payment/infrastructure/adapter/out/persistence/JpaPaymentRepositoryIntegrationTest.java
- [x] T040 [P] Implement PaymentJpaEntity and JpaPaymentRepository adapter in payment-service/src/main/java/com/ecommerce/payment/infrastructure/adapter/out/persistence/
- [x] T041 [P] Write integration test for JPA notification/customer repositories in notification-service/src/test/java/com/ecommerce/notification/infrastructure/adapter/out/persistence/JpaNotificationRepositoryIntegrationTest.java
- [x] T042 [P] Implement NotificationJpaEntity, CustomerJpaEntity, JpaNotificationRepository, JpaCustomerRepository adapters in notification-service/src/main/java/com/ecommerce/notification/infrastructure/adapter/out/persistence/

### Initial Data

- [x] T043 [P] Create data.sql with initial product data (P001, P002, P003) in product-service/src/main/resources/data.sql
- [x] T044 [P] Create data.sql with initial inventory data in inventory-service/src/main/resources/data.sql
- [x] T045 [P] Create data.sql with initial customer data (C001, C002) in notification-service/src/main/resources/data.sql

### Mapper Classes

- [x] T046 [P] Create infrastructure and application mapper classes for order-service in order-service/src/main/java/com/ecommerce/order/infrastructure/mapper/OrderInfraMapper.java and order-service/src/main/java/com/ecommerce/order/application/mapper/OrderApplicationMapper.java
- [x] T047 [P] Create infrastructure and application mapper classes for product-service in product-service/src/main/java/com/ecommerce/product/infrastructure/mapper/ProductInfraMapper.java and product-service/src/main/java/com/ecommerce/product/application/mapper/ProductApplicationMapper.java
- [x] T048 [P] Create infrastructure and application mapper classes for inventory-service in inventory-service/src/main/java/com/ecommerce/inventory/infrastructure/mapper/InventoryInfraMapper.java and inventory-service/src/main/java/com/ecommerce/inventory/application/mapper/InventoryApplicationMapper.java
- [x] T049 [P] Create infrastructure and application mapper classes for payment-service in payment-service/src/main/java/com/ecommerce/payment/infrastructure/mapper/PaymentInfraMapper.java and payment-service/src/main/java/com/ecommerce/payment/application/mapper/PaymentApplicationMapper.java
- [x] T050 [P] Create infrastructure and application mapper classes for notification-service in notification-service/src/main/java/com/ecommerce/notification/infrastructure/mapper/NotificationInfraMapper.java and notification-service/src/main/java/com/ecommerce/notification/application/mapper/NotificationApplicationMapper.java

**Checkpoint**: Foundation ready — all domain models, port interfaces, JPA adapters, mappers, and ArchUnit tests in place. User story implementation can now begin.

---

## Phase 3: User Story 1 — 訂單流程端到端追蹤 (Priority: P1) MVP

**Goal**: 實現完整的下單 Happy Path（Order → Product → Inventory → Payment），4 個服務的同步 HTTP 呼叫鏈可被 OTel Agent 自動追蹤。

**Independent Test**: 發送下單請求，在 Jaeger UI 以 TraceID 搜尋，看到 Order → Product → Inventory → Payment 完整 Trace。

### Tests for User Story 1 (TDD — Constitution Principle IV)

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T051 [P] [US1] Write unit test for QueryProductUseCase in product-service/src/test/java/com/ecommerce/product/application/service/QueryProductUseCaseTest.java
- [x] T052 [P] [US1] Write unit test for ReserveInventoryUseCase in inventory-service/src/test/java/com/ecommerce/inventory/application/service/ReserveInventoryUseCaseTest.java
- [x] T053 [P] [US1] Write unit test for ProcessPaymentUseCase in payment-service/src/test/java/com/ecommerce/payment/application/service/ProcessPaymentUseCaseTest.java
- [x] T054 [P] [US1] Write unit test for CreateOrderUseCase (happy path: product query → inventory reserve → payment → confirm) in order-service/src/test/java/com/ecommerce/order/application/service/CreateOrderUseCaseTest.java
- [x] T055 [P] [US1] Write integration test for ProductController (GET /api/products/{id}) in product-service/src/test/java/com/ecommerce/product/infrastructure/adapter/in/rest/ProductControllerIntegrationTest.java
- [x] T056 [P] [US1] Write integration test for InventoryController (POST /api/inventory/reserve) in inventory-service/src/test/java/com/ecommerce/inventory/infrastructure/adapter/in/rest/InventoryControllerIntegrationTest.java
- [x] T057 [P] [US1] Write integration test for PaymentController (POST /api/payments) in payment-service/src/test/java/com/ecommerce/payment/infrastructure/adapter/in/rest/PaymentControllerIntegrationTest.java
- [x] T058 [P] [US1] Write integration test for OrderController (POST /api/orders, happy path) in order-service/src/test/java/com/ecommerce/order/infrastructure/adapter/in/rest/OrderControllerIntegrationTest.java

### Implementation for User Story 1

- [x] T059 [P] [US1] Implement QueryProductPort (inbound) and QueryProductUseCase in product-service/src/main/java/com/ecommerce/product/application/
- [x] T060 [P] [US1] Implement ProductController REST adapter (GET /api/products/{id}) with ProductResponse DTO and ProductInfraMapper in product-service/src/main/java/com/ecommerce/product/infrastructure/
- [x] T061 [P] [US1] Implement ReserveInventoryPort, ReleaseInventoryPort (inbound) and ReserveInventoryUseCase with InventoryDomainService in inventory-service/src/main/java/com/ecommerce/inventory/application/
- [x] T062 [P] [US1] Implement InventoryController REST adapter (POST /api/inventory/reserve, POST /api/inventory/release) with DTOs and mapper in inventory-service/src/main/java/com/ecommerce/inventory/infrastructure/
- [x] T063 [P] [US1] Implement ProcessPaymentPort (inbound) and ProcessPaymentUseCase in payment-service/src/main/java/com/ecommerce/payment/application/
- [x] T064 [P] [US1] Implement PaymentController REST adapter (POST /api/payments) with DTOs and mapper in payment-service/src/main/java/com/ecommerce/payment/infrastructure/
- [x] T065 [US1] Define outbound port interfaces for order-service (ProductQueryPort, InventoryReservePort, PaymentPort, OrderEventPublisherPort) in order-service/src/main/java/com/ecommerce/order/application/port/out/
- [x] T066 [US1] Implement CreateOrderPort (inbound) and CreateOrderUseCase (orchestrates: query product → reserve inventory → process payment → confirm order) in order-service/src/main/java/com/ecommerce/order/application/
- [x] T067 [US1] Configure RestTemplate with 3s timeout in order-service/src/main/java/com/ecommerce/order/infrastructure/config/RestTemplateConfig.java
- [x] T068 [P] [US1] Implement ProductServiceClient (outbound REST adapter for ProductQueryPort) in order-service/src/main/java/com/ecommerce/order/infrastructure/adapter/out/rest/ProductServiceClient.java
- [x] T069 [P] [US1] Implement InventoryServiceClient (outbound REST adapter for InventoryReservePort) in order-service/src/main/java/com/ecommerce/order/infrastructure/adapter/out/rest/InventoryServiceClient.java
- [x] T070 [P] [US1] Implement PaymentServiceClient (outbound REST adapter for PaymentPort) in order-service/src/main/java/com/ecommerce/order/infrastructure/adapter/out/rest/PaymentServiceClient.java
- [x] T071 [US1] Implement OrderController REST adapter (POST /api/orders) with CreateOrderRequest/Response DTOs and OrderInfraMapper in order-service/src/main/java/com/ecommerce/order/infrastructure/adapter/in/rest/
- [x] T072 [US1] Create Dockerfile for each of the 4 services (order, product, inventory, payment) based on openjdk:8-jre-slim with OTel Agent download
- [x] T073 [US1] Create docker-compose.yml with Jaeger (all-in-one) + 4 services (order, product, inventory, payment) with OTel Agent environment variables

**Checkpoint**: Happy Path 下單流程完成。`docker-compose up` 後發送 POST /api/orders，在 Jaeger UI 看到 Order → Product → Inventory → Payment 完整 Trace。

---

## Phase 4: User Story 2 — 異常路徑追蹤（庫存不足與支付超時）(Priority: P2)

**Goal**: 庫存不足時訂單 FAILED 且 Trace 帶 error 標記；支付超時時訂單 PAYMENT_TIMEOUT 且自動庫存回滾。

**Independent Test**: 觸發庫存不足與支付超時場景，在 Jaeger 確認 error span 與長延遲 span。

### Tests for User Story 2 (TDD)

- [x] T074 [P] [US2] Write unit test for CreateOrderUseCase (inventory insufficient scenario: status=FAILED, no payment call) in order-service/src/test/java/com/ecommerce/order/application/service/CreateOrderUseCaseTest.java (add test method)
- [x] T075 [P] [US2] Write unit test for CreateOrderUseCase (payment timeout scenario: status=PAYMENT_TIMEOUT, inventory release called) in order-service/src/test/java/com/ecommerce/order/application/service/CreateOrderUseCaseTest.java (add test method)
- [x] T076 [P] [US2] Write unit test for InventoryDomainService (insufficient stock throws domain exception) in inventory-service/src/test/java/com/ecommerce/inventory/domain/model/InventoryTest.java (已由 InventoryTest.should_throw_when_insufficient_stock 涵蓋)
- [x] T077 [P] [US2] Write integration test for OrderController (inventory insufficient scenario) in order-service/src/test/java/com/ecommerce/order/infrastructure/adapter/in/rest/OrderControllerIntegrationTest.java (add test method)
- [x] T078 [P] [US2] Write integration test for PaymentController delay simulation endpoint (POST /api/admin/simulate-delay) in payment-service/src/test/java/com/ecommerce/payment/infrastructure/adapter/in/rest/PaymentControllerIntegrationTest.java (add test method)

### Implementation for User Story 2

- [x] T079 [US2] Implement InsufficientStockException in inventory-service/src/main/java/com/ecommerce/inventory/domain/model/InsufficientStockException.java and wire into InventoryDomainService (已於 Phase 2 完成)
- [x] T080 [US2] Add inventory insufficient error handling in CreateOrderUseCase: catch exception → set order status FAILED → skip payment call in order-service/src/main/java/com/ecommerce/order/application/service/CreateOrderUseCase.java
- [x] T081 [US2] Add payment timeout handling in CreateOrderUseCase: catch ResourceAccessException → call inventory release → set order status PAYMENT_TIMEOUT in order-service/src/main/java/com/ecommerce/order/application/service/CreateOrderUseCase.java
- [x] T082 [US2] Implement ReleaseInventoryUseCase in inventory-service/src/main/java/com/ecommerce/inventory/application/service/ReleaseInventoryUseCase.java (已於 Phase 3 完成)
- [x] T083 [US2] Implement DelaySimulatorConfig and AdminController (POST /api/admin/simulate-delay?ms=) in payment-service/src/main/java/com/ecommerce/payment/infrastructure/

**Checkpoint**: 庫存不足與支付超時場景可正確處理。Jaeger 中可看到 error 標記與長延遲 Span。

---

## Phase 5: User Story 3 — Kafka 非同步鏈路追蹤 (Priority: P3)

**Goal**: 訂單確認後發送 Kafka 事件，Notification Service 消費事件並查詢 DB、發送通知，HTTP 同步段與 Kafka 非同步段串聯在同一條 Trace。

**Independent Test**: 正常下單後，Jaeger 同一條 Trace 涵蓋 5 個服務，包含 Kafka produce/consume Span 與 Notification 的 JDBC Span。

### Tests for User Story 3 (TDD)

- [x] T084 [P] [US3] Write unit test for ProcessOrderNotificationUseCase in notification-service/src/test/java/com/ecommerce/notification/application/service/ProcessOrderNotificationUseCaseTest.java
- [x] T085 [P] [US3] Write integration test for KafkaOrderEventPublisher (publishes to order-confirmed topic) using EmbeddedKafka in order-service/src/test/java/com/ecommerce/order/infrastructure/adapter/out/kafka/KafkaOrderEventPublisherIntegrationTest.java
- [x] T086 [P] [US3] Write integration test for OrderConfirmedListener (consumes from order-confirmed topic) using EmbeddedKafka in notification-service/src/test/java/com/ecommerce/notification/infrastructure/adapter/in/kafka/OrderConfirmedListenerIntegrationTest.java

### Implementation for User Story 3

- [x] T087 [US3] Implement OrderEventPublisherPort (outbound) interface in order-service/src/main/java/com/ecommerce/order/application/port/out/OrderEventPublisherPort.java (已於 T065 完成)
- [x] T088 [US3] Implement KafkaProducerConfig in order-service (Spring Boot 自動設定已足夠，無需額外 Config)
- [x] T089 [US3] Implement OrderConfirmedMessage infrastructure DTO in order-service/src/main/java/com/ecommerce/order/infrastructure/dto/OrderConfirmedMessage.java
- [x] T090 [US3] Implement KafkaOrderEventPublisher (outbound adapter using KafkaTemplate) in order-service/src/main/java/com/ecommerce/order/infrastructure/adapter/out/kafka/KafkaOrderEventPublisher.java
- [x] T091 [US3] Wire Kafka event publishing into CreateOrderUseCase (publish after order confirmed) in order-service/src/main/java/com/ecommerce/order/application/service/CreateOrderUseCase.java
- [x] T092 [US3] Implement ProcessOrderNotificationPort (inbound) and ProcessOrderNotificationUseCase (query customer → send notification → persist) in notification-service/src/main/java/com/ecommerce/notification/application/
- [x] T093 [US3] Implement NotificationSenderPort (outbound) and MockNotificationSender adapter in notification-service/src/main/java/com/ecommerce/notification/infrastructure/adapter/out/sender/MockNotificationSender.java
- [x] T094 [US3] Implement KafkaConsumerConfig in notification-service/src/main/java/com/ecommerce/notification/infrastructure/config/KafkaConsumerConfig.java
- [x] T095 [US3] Implement OrderConfirmedMessage infrastructure DTO for notification-service in notification-service/src/main/java/com/ecommerce/notification/infrastructure/dto/OrderConfirmedMessage.java
- [x] T096 [US3] Implement OrderConfirmedListener (@KafkaListener, inbound Kafka adapter) in notification-service/src/main/java/com/ecommerce/notification/infrastructure/adapter/in/kafka/OrderConfirmedListener.java
- [x] T097 [US3] Create Dockerfile for notification-service with OTel Agent in notification-service/Dockerfile
- [x] T098 [US3] Update docker-compose.yml to add Kafka broker (KRaft mode) and notification-service with OTel Agent environment variables

**Checkpoint**: 完整 5 服務鏈路可追蹤。Jaeger 同一條 Trace 涵蓋 HTTP 同步段 + Kafka 非同步段。

---

## Phase 6: User Story 4 — Kafka 消費失敗與重試追蹤 (Priority: P4)

**Goal**: Notification Service 消費失敗時自動重試（最多 3 次），重試耗盡後投遞至 DLT，所有重試 Span 關聯到同一 Trace。

**Independent Test**: 啟用失敗模擬後下單，Jaeger 中看到多個 Consumer Span（含 error=true）與 DLT Producer Span。

### Tests for User Story 4 (TDD)

- [x] T099 [P] [US4] Write integration test for OrderConfirmedListener failure retry (3 retries then DLT) using EmbeddedKafka in notification-service/src/test/java/com/ecommerce/notification/infrastructure/adapter/in/kafka/OrderConfirmedListenerRetryIntegrationTest.java
- [x] T100 [P] [US4] Write integration test for AdminController failure simulation (POST /api/admin/simulate-failure) in notification-service/src/test/java/com/ecommerce/notification/infrastructure/adapter/in/rest/AdminControllerIntegrationTest.java

### Implementation for User Story 4

- [x] T101 [US4] Configure Spring Kafka retry with max 3 attempts and DLT (order-confirmed.DLT) in notification-service/src/main/java/com/ecommerce/notification/infrastructure/config/KafkaConsumerConfig.java
- [x] T102 [US4] Implement FailureSimulatorConfig (thread-safe boolean toggle) in notification-service/src/main/java/com/ecommerce/notification/infrastructure/config/FailureSimulatorConfig.java
- [x] T103 [US4] Implement AdminController (POST /api/admin/simulate-failure?enabled=) in notification-service/src/main/java/com/ecommerce/notification/infrastructure/adapter/in/rest/AdminController.java
- [x] T104 [US4] Wire failure simulation into OrderConfirmedListener (check toggle → throw exception to trigger retry) in notification-service/src/main/java/com/ecommerce/notification/infrastructure/adapter/in/kafka/OrderConfirmedListener.java

**Checkpoint**: Kafka 消費失敗、重試、DLT 場景完成。Jaeger 中所有重試 Span 可追蹤。

---

## Phase 7: User Story 5 — 零侵入驗證與效能影響評估 (Priority: P5)

**Goal**: 驗證業務程式碼零追蹤依賴，量化 Agent overhead（< 5%），驗證 graceful degradation。

**Independent Test**: 檢查所有 pom.xml 無 OTel 依賴，程式碼無追蹤 import；效能基準測試比較有/無 Agent 的回應時間。

### Tests for User Story 5 (TDD)

- [ ] T105 [P] [US5] Write verification test to assert no OTel/tracing imports in any service's main source code in order-service/src/test/java/com/ecommerce/order/ZeroIntrusionVerificationTest.java

### Implementation for User Story 5

- [ ] T106 [US5] Create docker-compose.no-agent.yml (same services but without JAVA_TOOL_OPTIONS agent flag) for baseline performance testing
- [ ] T107 [US5] Create benchmark shell script (100 requests, exclude first 10 warmup, compare avg response time with/without agent) in scripts/benchmark.sh
- [ ] T108 [US5] Create startup time measurement script (record JVM startup time with/without agent for all 5 services) in scripts/startup-benchmark.sh
- [ ] T109 [US5] Create graceful degradation test script (stop Jaeger container → send requests → verify services still respond normally) in scripts/graceful-degradation-test.sh
- [ ] T110 [US5] Create performance report template in docs/performance-report.md with placeholders for benchmark results

**Checkpoint**: 零侵入已驗證，效能基準測試腳本就緒，graceful degradation 已測試。

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Documentation, cleanup, final validation

- [ ] T111 [P] Create README.md with project overview, architecture diagram, and quickstart instructions
- [ ] T112 [P] Add Javadoc to all public API classes across all services
- [ ] T113 Run all unit tests across all 5 services and verify green
- [ ] T114 Run all integration tests across all 5 services and verify green
- [ ] T115 Run all ArchUnit tests and verify hexagonal architecture compliance
- [ ] T116 Run Checkstyle across all services and fix violations
- [ ] T117 Execute quickstart.md end-to-end validation: docker-compose up → run all 5 scenarios → verify Jaeger traces
- [ ] T118 Execute benchmark scripts and fill in performance-report.md with actual results

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion — BLOCKS all user stories
- **US1 (Phase 3)**: Depends on Foundational — BLOCKS US2 (error handling builds on happy path)
- **US2 (Phase 4)**: Depends on US1 (adds error handling to existing CreateOrderUseCase)
- **US3 (Phase 5)**: Depends on US1 (adds Kafka publishing after order confirmed)
- **US4 (Phase 6)**: Depends on US3 (adds retry/DLT to Kafka consumer)
- **US5 (Phase 7)**: Depends on US3 (needs full 5-service stack for meaningful benchmarks)
- **Polish (Phase 8)**: Depends on all user stories being complete

### Execution Flow

```text
Phase 1 (Setup)
    │
    ▼
Phase 2 (Foundational)
    │
    ▼
Phase 3 (US1: Happy Path) ◀── MVP
    │
    ├──▶ Phase 4 (US2: Error Paths)
    │
    └──▶ Phase 5 (US3: Kafka Async)
              │
              ├──▶ Phase 6 (US4: Kafka Retry/DLT)
              │
              └──▶ Phase 7 (US5: Benchmark)
                        │
                        ▼
                  Phase 8 (Polish)
```

### Within Each User Story

1. Tests MUST be written and FAIL before implementation (Red)
2. Implement minimum code to make tests pass (Green)
3. Refactor while keeping tests green (Refactor)
4. Models before services
5. Services before endpoints/adapters
6. Core implementation before integration

### Parallel Opportunities

- Phase 1: T002-T011 are all parallelizable (independent service setups)
- Phase 2: T012-T016 (ArchUnit), T017-T026 (domain models), T028-T032 (ports), T033-T042 (JPA), T043-T050 (data/mappers) — all [P] groups can run in parallel
- Phase 3: T051-T058 (tests), T059-T064 (product/inventory/payment impl) — [P] groups within each phase
- Phase 4-7: Primarily sequential within each phase

---

## Parallel Example: Phase 2 (Foundational)

```bash
# Launch all ArchUnit tests in parallel:
Task: T012 "ArchUnit test for order-service"
Task: T013 "ArchUnit test for product-service"
Task: T014 "ArchUnit test for inventory-service"
Task: T015 "ArchUnit test for payment-service"
Task: T016 "ArchUnit test for notification-service"

# Launch all domain model tests + implementations in parallel:
Task: T017 "Unit test for Order model"
Task: T019 "Unit test for Product model"
Task: T021 "Unit test for Inventory model"
Task: T023 "Unit test for Payment model"
Task: T025 "Unit test for Notification model"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL — blocks all stories)
3. Complete Phase 3: User Story 1 (Happy Path)
4. **STOP and VALIDATE**: `docker-compose up` → POST /api/orders → Jaeger UI shows 4-service Trace
5. Demo ready with synchronous HTTP tracing

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. US1 (Happy Path) → MVP: 4 services, HTTP tracing (Phase 3)
3. US2 (Error Paths) → Add error/timeout handling (Phase 4)
4. US3 (Kafka Async) → Add 5th service + Kafka tracing (Phase 5)
5. US4 (Kafka Retry) → Add retry/DLT tracing (Phase 6)
6. US5 (Benchmark) → Performance validation (Phase 7)
7. Polish → Documentation + final validation (Phase 8)

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- TDD is mandatory (Constitution Principle IV): write tests FIRST, verify they FAIL, then implement
- Hexagonal architecture is mandatory (Constitution Principle I): domain → application → infrastructure
- Mapper classes mandatory at each layer boundary (Constitution)
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
