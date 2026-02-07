# Contracts: Grafana Monitoring Dashboard

**Feature**: 004-grafana-dashboard

## Not Applicable

This feature is infrastructure-only. It does not introduce or modify any API contracts between services.

The existing microservice API contracts (REST endpoints, Kafka message schemas) remain unchanged. This feature only:

1. Enables OTel metrics export via environment variable changes
2. Adds Prometheus and Grafana as new infrastructure services
3. Provisions Grafana dashboards via JSON configuration files

No contract tests are required for this feature.
