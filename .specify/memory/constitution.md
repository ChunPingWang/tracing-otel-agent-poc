<!--
  Sync Impact Report
  ===================
  Version change: N/A (new) → 1.0.0
  Modified principles: N/A (initial creation)
  Added sections:
    - Principle I: Hexagonal Architecture (六角形架構)
    - Principle II: Domain-Driven Design (領域驅動設計)
    - Principle III: SOLID Principles (SOLID 原則)
    - Principle IV: Test-Driven Development (測試驅動開發)
    - Principle V: Behavior-Driven Development (行為驅動開發)
    - Principle VI: Code Quality Standards (程式碼品質標準)
    - Section: Layered Architecture Constraints (分層架構約束)
    - Section: Testing Standards (測試標準)
    - Section: Governance
  Removed sections: N/A
  Templates requiring updates:
    - .specify/templates/plan-template.md ✅ reviewed (no update needed)
    - .specify/templates/spec-template.md ✅ reviewed (no update needed)
    - .specify/templates/tasks-template.md ✅ reviewed (no update needed)
    - .specify/templates/checklist-template.md ✅ reviewed (no update needed)
    - .specify/templates/agent-file-template.md ✅ reviewed (no update needed)
  Follow-up TODOs: None
-->

# E-Commerce Distributed Tracing PoC Constitution

## Core Principles

### I. Hexagonal Architecture (六角形架構)

All services MUST adopt Hexagonal Architecture (Ports & Adapters).
The architecture defines three concentric layers:

- **Domain Layer (innermost)**: Contains domain entities, value objects,
  domain events, and domain services. MUST have zero dependencies on
  frameworks or infrastructure. MUST be pure Java with no Spring or
  library annotations.
- **Application Layer (middle)**: Contains use cases (application services),
  input/output port interfaces, and orchestration logic. MUST depend only
  on the domain layer. MUST NOT reference any infrastructure implementation.
- **Infrastructure Layer (outermost)**: Contains adapters (REST controllers,
  Kafka consumers/producers, JPA repositories, HTTP clients), framework
  configuration, and all third-party integrations. Spring Boot, Spring
  Data JPA, Spring Kafka, and any other framework MUST reside exclusively
  in this layer.

**Dependency Direction**: Infrastructure → Application → Domain.
Infrastructure MAY directly use application and domain layer types.
Application and domain layers MUST access infrastructure capabilities
exclusively through port interfaces (Dependency Inversion Principle).

**Data Transfer**: Data crossing layer boundaries MUST be converted via
dedicated mapper classes. Domain entities MUST NOT leak into API responses
or persistence models. Each layer boundary MUST have its own DTO/model
with a corresponding mapper.

### II. Domain-Driven Design (領域驅動設計)

Each microservice MUST represent a distinct Bounded Context with clearly
defined boundaries and a Ubiquitous Language.

- Domain logic MUST reside in domain entities and domain services, not
  in application services or infrastructure adapters.
- Aggregates MUST enforce invariants and serve as the consistency boundary.
- Value Objects MUST be used for concepts without identity (e.g., Money,
  Address, OrderStatus).
- Domain Events MUST be used for cross-aggregate and cross-service
  communication.
- Repositories (port interfaces) MUST be defined in the domain or
  application layer; implementations reside in infrastructure.
- The Ubiquitous Language MUST be reflected in class names, method names,
  and package names within each bounded context.

### III. SOLID Principles (SOLID 原則)

All code MUST adhere to the SOLID principles:

- **Single Responsibility (SRP)**: Each class MUST have exactly one reason
  to change. Controllers handle HTTP, services handle business logic,
  repositories handle persistence.
- **Open/Closed (OCP)**: Modules MUST be open for extension and closed for
  modification. Use strategy patterns and port interfaces to enable
  extensibility without changing existing code.
- **Liskov Substitution (LSP)**: Subtypes MUST be substitutable for their
  base types. Port interface implementations MUST honor the contract
  defined by the interface.
- **Interface Segregation (ISP)**: Clients MUST NOT be forced to depend on
  interfaces they do not use. Define fine-grained port interfaces specific
  to each use case.
- **Dependency Inversion (DIP)**: High-level modules (domain, application)
  MUST NOT depend on low-level modules (infrastructure). Both MUST depend
  on abstractions (port interfaces).

### IV. Test-Driven Development (測試驅動開發)

TDD is NON-NEGOTIABLE for all production code.

- The Red-Green-Refactor cycle MUST be strictly followed:
  1. **Red**: Write a failing test that defines the desired behavior.
  2. **Green**: Write the minimum code to make the test pass.
  3. **Refactor**: Improve code structure while keeping tests green.
- Tests MUST be written BEFORE implementation code. No production code
  may be committed without a corresponding test.
- Unit tests MUST cover domain logic and application services in isolation,
  using test doubles for port interfaces.
- Integration tests MUST verify adapter implementations (REST endpoints,
  Kafka producers/consumers, JPA repositories) against real or embedded
  infrastructure.
- Contract tests MUST validate API contracts between services.

### V. Behavior-Driven Development (行為驅動開發)

User stories and acceptance criteria MUST follow the BDD format:

- All acceptance scenarios MUST use the Given-When-Then structure.
- Acceptance scenarios MUST be written in collaboration with domain
  experts before implementation begins.
- Acceptance tests MUST be automated and executable as part of the CI
  pipeline.
- Scenario names MUST describe business behavior in the Ubiquitous
  Language, not technical implementation details.
- Each user story MUST have at least one happy path and one error path
  acceptance scenario.

### VI. Code Quality Standards (程式碼品質標準)

- All code MUST pass static analysis checks (linting, formatting) before
  merge.
- Methods MUST NOT exceed 20 lines of logic (excluding blank lines and
  braces). Methods exceeding this limit MUST be refactored.
- Cyclomatic complexity per method MUST NOT exceed 10.
- All public APIs MUST have Javadoc documentation.
- Magic numbers and magic strings MUST be extracted to named constants
  or enums.
- Null MUST be avoided in domain models; use Optional or domain-specific
  null-object patterns.

## Layered Architecture Constraints (分層架構約束)

The following constraints codify the hexagonal architecture dependency
rules and MUST be enforced via build tooling or architecture tests:

| Source Layer    | Can Use          | Access Method           |
|-----------------|------------------|-------------------------|
| Infrastructure  | Application      | Direct reference        |
| Infrastructure  | Domain           | Direct reference        |
| Application     | Domain           | Direct reference        |
| Application     | Infrastructure   | Via port interface ONLY  |
| Domain          | Application      | PROHIBITED              |
| Domain          | Infrastructure   | PROHIBITED              |

**Package Structure** (per microservice):

```text
com.example.{service-name}/
├── domain/
│   ├── model/          # Entities, Value Objects, Aggregates
│   ├── event/          # Domain Events
│   ├── service/        # Domain Services
│   └── port/           # Repository interfaces (outbound ports)
├── application/
│   ├── service/        # Use Cases / Application Services
│   ├── port/
│   │   ├── in/         # Inbound ports (use case interfaces)
│   │   └── out/        # Outbound ports (driven adapter interfaces)
│   ├── dto/            # Application-layer DTOs
│   └── mapper/         # Domain ↔ Application DTO mappers
└── infrastructure/
    ├── adapter/
    │   ├── in/
    │   │   ├── rest/   # REST Controllers (driving adapters)
    │   │   └── kafka/  # Kafka Consumers (driving adapters)
    │   └── out/
    │       ├── persistence/ # JPA Repositories (driven adapters)
    │       ├── kafka/       # Kafka Producers (driven adapters)
    │       └── rest/        # HTTP Clients (driven adapters)
    ├── config/         # Spring configuration
    ├── dto/            # Infrastructure-layer DTOs (API models, JPA entities)
    └── mapper/         # Application DTO ↔ Infrastructure DTO mappers
```

**Mapper Rules**:
- Infrastructure inbound adapters MUST convert infrastructure DTOs
  (e.g., REST request bodies) to application DTOs before invoking
  application services.
- Application services MUST convert application DTOs to domain objects
  before invoking domain logic.
- Return paths follow the reverse mapping chain.
- Mappers MUST be stateless and MUST NOT contain business logic.
- Each mapper MUST be a dedicated class (not inline conversion code).

## Testing Standards (測試標準)

### Test Pyramid

| Level            | Scope                           | Target               |
|------------------|---------------------------------|----------------------|
| Unit Tests       | Single class in isolation       | Domain, Application  |
| Integration Tests| Adapter + real/embedded infra   | Infrastructure       |
| Contract Tests   | API contract between services   | REST endpoints       |
| Acceptance Tests | End-to-end BDD scenarios        | Full service chain   |

### Coverage Requirements

- Domain layer: MUST achieve >= 90% line coverage.
- Application layer: MUST achieve >= 80% line coverage.
- Infrastructure adapters: MUST have integration tests for all
  public endpoints and message handlers.
- Overall project: MUST achieve >= 75% line coverage.

### Test Naming Convention

Tests MUST follow the pattern:
`should_[expectedBehavior]_when_[condition]`

Example: `should_reserveInventory_when_stockIsAvailable`

### Test Independence

- Each test MUST be independently executable and MUST NOT depend on
  execution order.
- Tests MUST NOT share mutable state.
- Integration tests MUST use embedded or containerized infrastructure
  (H2, Embedded Kafka) and MUST NOT require external services.

## Governance

- This constitution supersedes all other development practices and
  guidelines within this project.
- All pull requests MUST be verified for compliance with these
  principles before merge.
- Amendments to this constitution require:
  1. A written proposal documenting the change and rationale.
  2. Review and approval by at least one project maintainer.
  3. A migration plan for existing code if the change affects current
     implementations.
  4. Version bump following semantic versioning (see below).
- **Versioning Policy**:
  - MAJOR: Backward-incompatible principle removals or redefinitions.
  - MINOR: New principle/section added or materially expanded guidance.
  - PATCH: Clarifications, wording, or non-semantic refinements.
- **Compliance Review**: Architecture tests (e.g., ArchUnit) SHOULD be
  used to automate enforcement of layering constraints and dependency
  rules. Manual review is required for principles not automatable.
- Refer to `CLAUDE.md` or the agent-file-template for runtime
  development guidance.

**Version**: 1.0.0 | **Ratified**: 2026-02-07 | **Last Amended**: 2026-02-07
