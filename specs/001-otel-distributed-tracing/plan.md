# Implementation Plan: E-Commerce Distributed Tracing PoC (OTel Agent)

**Branch**: `001-otel-distributed-tracing` | **Date**: 2026-02-07 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/001-otel-distributed-tracing/spec.md`

## Summary

建立 5 個 Spring Boot 2 微服務（Order、Product、Inventory、Payment、Notification），
採用六角形架構與 DDD 原則，透過 OpenTelemetry Java Agent 1.32.1 零侵入方式導入
分散式追蹤，涵蓋同步 HTTP、非同步 Kafka、JDBC 三種通訊模式的端到端可觀測性。
使用 Docker Compose 編排完整環境，Jaeger 作為追蹤後端。

## Technical Context

**Language/Version**: Java 1.8 (OpenJDK 8)
**Primary Dependencies**: Spring Boot 2.7.18, Spring Data JPA 2.7.x, Spring Kafka 2.9.x, H2 Database, OpenTelemetry Java Agent 1.32.1
**Storage**: H2 (embedded, per-service)
**Testing**: JUnit 5, Spring Boot Test, Embedded Kafka (spring-kafka-test), ArchUnit (architecture tests)
**Target Platform**: Docker containers on Linux (Docker Compose)
**Project Type**: Multi-module microservices (5 independent Maven projects)
**Performance Goals**: Agent overhead < 5% on average response time (100 requests, excluding 10 warmup)
**Constraints**: JDK 8 only (OTel Agent 1.32.1 is last JDK 8 compatible), zero business code modification for tracing
**Scale/Scope**: PoC — 5 services, 5 business scenarios, single-node Docker Compose deployment

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| # | Principle | Status | Notes |
|---|-----------|--------|-------|
| I | Hexagonal Architecture | PASS | Each service follows domain/application/infrastructure layering with ports & adapters. Frameworks (Spring Boot, Spring Kafka, JPA) reside exclusively in infrastructure layer. |
| II | Domain-Driven Design | PASS | Each service = 1 Bounded Context. Entities, Value Objects, Domain Events defined. Ubiquitous Language preserved in package/class names. |
| III | SOLID Principles | PASS | Port interfaces for DIP. Fine-grained interfaces per use case (ISP). SRP enforced via layer separation. |
| IV | Test-Driven Development | PASS | TDD cycle enforced. Unit tests for domain/application, integration tests for adapters, contract tests for inter-service APIs. |
| V | Behavior-Driven Development | PASS | All acceptance scenarios in Given-When-Then format. BDD scenarios mapped to automated acceptance tests. |
| VI | Code Quality Standards | PASS | Checkstyle + SpotBugs configured. Method length ≤ 20 lines. Cyclomatic complexity ≤ 10. Javadoc on public APIs. |

**Layered Architecture Constraints**: PASS — Dependency matrix enforced via ArchUnit tests.

**Mapper Rules**: PASS — Dedicated mapper classes at each layer boundary:
- Infrastructure DTO ↔ Application DTO (infrastructure/mapper/)
- Application DTO ↔ Domain model (application/mapper/)

## Project Structure

### Documentation (this feature)

```text
specs/001-otel-distributed-tracing/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   ├── order-service-api.yaml
│   ├── product-service-api.yaml
│   ├── inventory-service-api.yaml
│   ├── payment-service-api.yaml
│   └── order-confirmed-event.yaml
├── checklists/
│   └── requirements.md
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
docker-compose.yml
order-service/
├── Dockerfile
├── pom.xml
└── src/
    ├── main/java/com/ecommerce/order/
    │   ├── OrderApplication.java
    │   ├── domain/
    │   │   ├── model/
    │   │   │   ├── Order.java               # Aggregate Root
    │   │   │   ├── OrderItem.java            # Entity
    │   │   │   ├── OrderStatus.java          # Value Object (enum)
    │   │   │   └── CustomerId.java           # Value Object
    │   │   ├── event/
    │   │   │   └── OrderConfirmedEvent.java  # Domain Event
    │   │   ├── service/
    │   │   │   └── OrderDomainService.java
    │   │   └── port/
    │   │       └── OrderRepository.java      # Outbound port interface
    │   ├── application/
    │   │   ├── service/
    │   │   │   └── CreateOrderUseCase.java   # Application service
    │   │   ├── port/
    │   │   │   ├── in/
    │   │   │   │   └── CreateOrderPort.java  # Inbound port
    │   │   │   └── out/
    │   │   │       ├── ProductQueryPort.java
    │   │   │       ├── InventoryReservePort.java
    │   │   │       ├── PaymentPort.java
    │   │   │       └── OrderEventPublisherPort.java
    │   │   ├── dto/
    │   │   │   ├── CreateOrderCommand.java
    │   │   │   ├── OrderResult.java
    │   │   │   ├── ProductInfo.java
    │   │   │   └── PaymentResult.java
    │   │   └── mapper/
    │   │       └── OrderApplicationMapper.java
    │   └── infrastructure/
    │       ├── adapter/
    │       │   ├── in/
    │       │   │   └── rest/
    │       │   │       └── OrderController.java
    │       │   └── out/
    │       │       ├── persistence/
    │       │       │   ├── JpaOrderRepository.java
    │       │       │   └── OrderJpaEntity.java
    │       │       ├── rest/
    │       │       │   ├── ProductServiceClient.java
    │       │       │   ├── InventoryServiceClient.java
    │       │       │   └── PaymentServiceClient.java
    │       │       └── kafka/
    │       │           └── KafkaOrderEventPublisher.java
    │       ├── config/
    │       │   ├── RestTemplateConfig.java
    │       │   └── KafkaProducerConfig.java
    │       ├── dto/
    │       │   ├── CreateOrderRequest.java   # REST request DTO
    │       │   ├── CreateOrderResponse.java  # REST response DTO
    │       │   └── OrderConfirmedMessage.java # Kafka message DTO
    │       └── mapper/
    │           └── OrderInfraMapper.java
    └── test/java/com/ecommerce/order/
        ├── domain/
        │   └── model/OrderTest.java
        ├── application/
        │   └── service/CreateOrderUseCaseTest.java
        └── infrastructure/
            ├── adapter/in/rest/OrderControllerIntegrationTest.java
            └── adapter/out/
                ├── persistence/JpaOrderRepositoryIntegrationTest.java
                └── kafka/KafkaOrderEventPublisherIntegrationTest.java

product-service/
├── Dockerfile
├── pom.xml
└── src/
    ├── main/java/com/ecommerce/product/
    │   ├── ProductApplication.java
    │   ├── domain/
    │   │   ├── model/
    │   │   │   └── Product.java
    │   │   └── port/
    │   │       └── ProductRepository.java
    │   ├── application/
    │   │   ├── service/
    │   │   │   └── QueryProductUseCase.java
    │   │   ├── port/in/
    │   │   │   └── QueryProductPort.java
    │   │   ├── dto/
    │   │   │   └── ProductResult.java
    │   │   └── mapper/
    │   │       └── ProductApplicationMapper.java
    │   └── infrastructure/
    │       ├── adapter/
    │       │   ├── in/rest/
    │       │   │   └── ProductController.java
    │       │   └── out/persistence/
    │       │       ├── JpaProductRepository.java
    │       │       └── ProductJpaEntity.java
    │       ├── dto/
    │       │   └── ProductResponse.java
    │       └── mapper/
    │           └── ProductInfraMapper.java
    └── test/java/com/ecommerce/product/
        ├── domain/model/ProductTest.java
        ├── application/service/QueryProductUseCaseTest.java
        └── infrastructure/adapter/
            ├── in/rest/ProductControllerIntegrationTest.java
            └── out/persistence/JpaProductRepositoryIntegrationTest.java

inventory-service/
├── Dockerfile
├── pom.xml
└── src/
    ├── main/java/com/ecommerce/inventory/
    │   ├── InventoryApplication.java
    │   ├── domain/
    │   │   ├── model/
    │   │   │   └── Inventory.java
    │   │   ├── service/
    │   │   │   └── InventoryDomainService.java
    │   │   └── port/
    │   │       └── InventoryRepository.java
    │   ├── application/
    │   │   ├── service/
    │   │   │   ├── ReserveInventoryUseCase.java
    │   │   │   └── ReleaseInventoryUseCase.java
    │   │   ├── port/in/
    │   │   │   ├── ReserveInventoryPort.java
    │   │   │   └── ReleaseInventoryPort.java
    │   │   ├── dto/
    │   │   │   ├── ReserveCommand.java
    │   │   │   ├── ReleaseCommand.java
    │   │   │   └── ReserveResult.java
    │   │   └── mapper/
    │   │       └── InventoryApplicationMapper.java
    │   └── infrastructure/
    │       ├── adapter/
    │       │   ├── in/rest/
    │       │   │   └── InventoryController.java
    │       │   └── out/persistence/
    │       │       ├── JpaInventoryRepository.java
    │       │       └── InventoryJpaEntity.java
    │       ├── dto/
    │       │   ├── ReserveRequest.java
    │       │   ├── ReserveResponse.java
    │       │   ├── ReleaseRequest.java
    │       │   └── ReleaseResponse.java
    │       └── mapper/
    │           └── InventoryInfraMapper.java
    └── test/java/com/ecommerce/inventory/
        ├── domain/
        │   ├── model/InventoryTest.java
        │   └── service/InventoryDomainServiceTest.java
        ├── application/service/
        │   ├── ReserveInventoryUseCaseTest.java
        │   └── ReleaseInventoryUseCaseTest.java
        └── infrastructure/adapter/
            ├── in/rest/InventoryControllerIntegrationTest.java
            └── out/persistence/JpaInventoryRepositoryIntegrationTest.java

payment-service/
├── Dockerfile
├── pom.xml
└── src/
    ├── main/java/com/ecommerce/payment/
    │   ├── PaymentApplication.java
    │   ├── domain/
    │   │   ├── model/
    │   │   │   ├── Payment.java
    │   │   │   └── PaymentStatus.java
    │   │   └── port/
    │   │       └── PaymentRepository.java
    │   ├── application/
    │   │   ├── service/
    │   │   │   └── ProcessPaymentUseCase.java
    │   │   ├── port/in/
    │   │   │   └── ProcessPaymentPort.java
    │   │   ├── dto/
    │   │   │   ├── PaymentCommand.java
    │   │   │   └── PaymentResult.java
    │   │   └── mapper/
    │   │       └── PaymentApplicationMapper.java
    │   └── infrastructure/
    │       ├── adapter/
    │       │   ├── in/rest/
    │       │   │   ├── PaymentController.java
    │       │   │   └── AdminController.java
    │       │   └── out/persistence/
    │       │       ├── JpaPaymentRepository.java
    │       │       └── PaymentJpaEntity.java
    │       ├── config/
    │       │   └── DelaySimulatorConfig.java
    │       ├── dto/
    │       │   ├── PaymentRequest.java
    │       │   └── PaymentResponse.java
    │       └── mapper/
    │           └── PaymentInfraMapper.java
    └── test/java/com/ecommerce/payment/
        ├── domain/model/PaymentTest.java
        ├── application/service/ProcessPaymentUseCaseTest.java
        └── infrastructure/adapter/
            ├── in/rest/PaymentControllerIntegrationTest.java
            └── out/persistence/JpaPaymentRepositoryIntegrationTest.java

notification-service/
├── Dockerfile
├── pom.xml
└── src/
    ├── main/java/com/ecommerce/notification/
    │   ├── NotificationApplication.java
    │   ├── domain/
    │   │   ├── model/
    │   │   │   ├── Notification.java
    │   │   │   └── NotificationStatus.java
    │   │   └── port/
    │   │       ├── NotificationRepository.java
    │   │       └── CustomerRepository.java
    │   ├── application/
    │   │   ├── service/
    │   │   │   └── ProcessOrderNotificationUseCase.java
    │   │   ├── port/in/
    │   │   │   └── ProcessOrderNotificationPort.java
    │   │   ├── port/out/
    │   │   │   └── NotificationSenderPort.java
    │   │   ├── dto/
    │   │   │   └── OrderConfirmedDto.java
    │   │   └── mapper/
    │   │       └── NotificationApplicationMapper.java
    │   └── infrastructure/
    │       ├── adapter/
    │       │   ├── in/
    │       │   │   ├── kafka/
    │       │   │   │   └── OrderConfirmedListener.java
    │       │   │   └── rest/
    │       │   │       └── AdminController.java
    │       │   └── out/
    │       │       ├── persistence/
    │       │       │   ├── JpaNotificationRepository.java
    │       │       │   ├── JpaCustomerRepository.java
    │       │       │   ├── NotificationJpaEntity.java
    │       │       │   └── CustomerJpaEntity.java
    │       │       └── sender/
    │       │           └── MockNotificationSender.java
    │       ├── config/
    │       │   ├── KafkaConsumerConfig.java
    │       │   └── FailureSimulatorConfig.java
    │       ├── dto/
    │       │   └── OrderConfirmedMessage.java
    │       └── mapper/
    │           └── NotificationInfraMapper.java
    └── test/java/com/ecommerce/notification/
        ├── domain/model/NotificationTest.java
        ├── application/service/ProcessOrderNotificationUseCaseTest.java
        └── infrastructure/adapter/
            ├── in/kafka/OrderConfirmedListenerIntegrationTest.java
            └── out/persistence/JpaNotificationRepositoryIntegrationTest.java
```

**Structure Decision**: Multi-module microservices pattern with 5 independent Maven projects.
Each service follows the hexagonal architecture package structure defined in the constitution:
`domain/` → `application/` → `infrastructure/`. All framework dependencies (Spring Boot,
Spring Kafka, JPA) reside exclusively in the `infrastructure/` package. Cross-layer data
transfer uses dedicated mapper classes at each boundary.

## Complexity Tracking

> **No constitution violations detected. All gates pass.**

| Aspect | Justification |
|--------|---------------|
| 5 independent Maven projects | Required by PRD — each microservice is independently deployable. Not a complexity violation. |
| Hexagonal architecture in PoC | Constitution mandates hexagonal architecture. Although PoC could use simpler structure, compliance with constitution is non-negotiable. |
| Mapper classes at layer boundaries | Constitution requires data transfer via mappers. Adds classes but ensures domain model isolation. |
