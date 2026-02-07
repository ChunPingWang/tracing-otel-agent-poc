# Research: Grafana Alert Contact Point & Notification Policy

## R1: Grafana Provisioning Format for Contact Points

**Decision**: Use `contactPoints:` top-level key in a new YAML file (`contact-points.yaml`) under `grafana/provisioning/alerting/`.

**Rationale**: Grafana 11.x reads all files in the `provisioning/alerting/` directory. While contact points CAN coexist in the same file as alert rules (`groups:`), splitting into separate files improves clarity and follows separation of concerns. Each file must start with `apiVersion: 1`.

**Format**:
```yaml
apiVersion: 1
contactPoints:
  - orgId: 1
    name: <contact-point-name>
    receivers:
      - uid: <unique-id>
        type: webhook
        disableResolveMessage: false
        settings:
          url: http://<host>:<port>/<path>
          httpMethod: POST
          maxAlerts: '0'
```

**Alternatives considered**:
- Single combined file with alert rules → rejected for maintainability
- Separate files per contact point → overkill for a single webhook

## R2: Grafana Provisioning Format for Notification Policies

**Decision**: Use `policies:` top-level key in a new YAML file (`notification-policies.yaml`) under `grafana/provisioning/alerting/`.

**Rationale**: The notification policy tree is a **single resource per org** — the entire tree must be defined in one `policies` entry. It cannot be incrementally assembled from multiple files. Keeping it in its own file is clearer than mixing with contact points.

**Format**:
```yaml
apiVersion: 1
policies:
  - orgId: 1
    receiver: <default-contact-point-name>
    group_by: [grafana_folder, alertname]
    group_wait: 10s
    group_interval: 30s
    repeat_interval: 5m
    routes:
      - receiver: <contact-point-name>
        matchers:
          - severity = critical
        group_wait: 5s
        repeat_interval: 1m
```

**Key caveat**: The `receiver` field in policies must exactly match a contact point `name`. Provisioned policies are read-only in the UI.

**Alternatives considered**:
- Using `object_matchers` array format → `matchers` string format is simpler and used in Grafana's official sample.yaml

## R3: Webhook Receiver Docker Image

**Decision**: Use `mendhak/http-https-echo:39` as the webhook receiver.

**Rationale**: It meets all requirements — pre-built Docker image, logs full POST body JSON to stdout (visible via `docker compose logs`), accepts any HTTP method at any path, tiny (~25MB), zero configuration.

**Docker Compose service**:
```yaml
webhook-sink:
  image: mendhak/http-https-echo:39
  ports:
    - "8090:8080"
  environment:
    HTTP_PORT: 8080
  networks:
    - ecommerce-net
```

**Grafana contact point URL**: `http://webhook-sink:8080/grafana-alerts`

**Alternatives considered**:
- `tarampampam/webhook-tester:2` → Go/scratch, ~6.5MB, has web UI, but does NOT log POST bodies to stdout (only viewable in UI). Not suitable for `docker compose logs` verification.
- `grafana/alertmanager-webhook-logger:0.3` → archived since Jan 2025, expects Alertmanager-format JSON (not Grafana Alerting format), only listens at `/`.
- `httpbin` → does NOT log POST bodies to stdout (confirmed issue #597).
- Custom Node.js/Python → requires custom Dockerfile, violates pre-built image requirement.

## R4: Notification Timing for PoC

**Decision**: Use aggressive timings for quick feedback in PoC.

| Parameter | PoC Value | Grafana Default | Rationale |
|-----------|-----------|-----------------|-----------|
| `group_wait` | `10s` | `30s` | Faster first notification for demo |
| `group_interval` | `30s` | `5m` | Show group updates quickly |
| `repeat_interval` | `5m` | `4h` | Re-send often for observability |
| Critical `group_wait` | `5s` | - | Even faster for critical alerts |
| Critical `repeat_interval` | `1m` | - | High urgency, frequent reminders |

**Alternatives considered**:
- Using Grafana defaults → too slow for PoC demonstration (4h repeat means you'd wait 4 hours to see a re-notification)
- Even more aggressive (1s group_wait) → risks notification storms during testing

## R5: File Organization Strategy

**Decision**: Create 2 new files in `grafana/provisioning/alerting/`:
1. `contact-points.yaml` — contact point definitions
2. `notification-policies.yaml` — notification policy tree

**Rationale**: Grafana reads ALL files in the alerting directory. Splitting by resource type (alert rules, contact points, policies) follows the single-responsibility principle and matches the natural Grafana UI navigation (Alert Rules page vs Contact Points page vs Notification Policies page).

**Alternatives considered**:
- Single combined file → harder to maintain, mixes different concerns
- Adding to existing `alert-rules.yaml` → works but violates separation of concerns

## R6: Docker Compose Volume Strategy

**Decision**: No volume changes needed for Docker Compose. The existing volume mount `./grafana/provisioning/alerting:/etc/grafana/provisioning/alerting` already mounts the entire `alerting/` directory, so any new files added there are automatically available in the container.

**Rationale**: Unlike K8s ConfigMaps (which use `subPath` for individual files), the Docker Compose mount covers the entire directory.

**For K8s**: New ConfigMaps are needed for each new file, with corresponding volumeMount entries using `subPath`.

## R7: K8s ConfigMap Strategy

**Decision**: Create 2 new K8s ConfigMaps:
1. `grafana-contact-points` — embeds `contact-points.yaml`
2. `grafana-notification-policies` — embeds `notification-policies.yaml`

Add corresponding volumeMount entries with `subPath` in the Grafana deployment.

**Rationale**: Follows the existing pattern (separate ConfigMap per provisioning file) established in Features 004 and 005. K8s ConfigMaps with subPath require individual file mapping.
