# tracing-otel-agent-poc Development Guidelines

Auto-generated from all feature plans. Last updated: 2026-02-07

## Active Technologies
- Bash (scripts), YAML (K8s manifests, Helm values), JSON (APISIX Admin API payloads). No Java code changes. + Apache APISIX 3.9.1-debian (Helm chart), etcd (bundled), Kind v0.20+, kubectl v1.28+, Helm 3.14+ (003-apisix-blue-green)
- etcd (APISIX config store, single replica, no persistence). Application services use H2 in-memory (unchanged). (003-apisix-blue-green)

- Java 1.8 (OpenJDK 8) + Spring Boot 2.7.18, Spring Data JPA 2.7.x, Spring Kafka 2.9.x, H2 Database, OpenTelemetry Java Agent 1.32.1 (001-otel-distributed-tracing)

## Project Structure

```text
src/
tests/
```

## Commands

# Add commands for Java 1.8 (OpenJDK 8)

## Code Style

Java 1.8 (OpenJDK 8): Follow standard conventions

## Recent Changes
- 003-apisix-blue-green: Added Bash (scripts), YAML (K8s manifests, Helm values), JSON (APISIX Admin API payloads). No Java code changes. + Apache APISIX 3.9.1-debian (Helm chart), etcd (bundled), Kind v0.20+, kubectl v1.28+, Helm 3.14+

- 001-otel-distributed-tracing: Added Java 1.8 (OpenJDK 8) + Spring Boot 2.7.18, Spring Data JPA 2.7.x, Spring Kafka 2.9.x, H2 Database, OpenTelemetry Java Agent 1.32.1

<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->
