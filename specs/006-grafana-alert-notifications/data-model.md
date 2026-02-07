# Data Model: Grafana Alert Contact Point & Notification Policy

## Entities

### Contact Point

A named notification destination that Grafana sends alert notifications to.

| Field | Description |
|-------|-------------|
| orgId | Organization ID (default: 1) |
| name | Display name, referenced by notification policies |
| receivers[] | List of notification channels within this contact point |

### Receiver (within Contact Point)

An individual notification channel.

| Field | Description |
|-------|-------------|
| uid | Unique identifier (max 40 chars, alphanumeric + `-` + `_`) |
| type | Notification type: `webhook`, `email`, `slack`, etc. |
| disableResolveMessage | Whether to suppress "resolved" notifications (default: false) |
| settings | Type-specific settings (e.g., URL for webhook) |

### Webhook Settings (type: webhook)

| Field | Required | Description |
|-------|----------|-------------|
| url | Yes | Webhook endpoint URL |
| httpMethod | No | `POST` (default) or `PUT` |
| maxAlerts | No | Max alerts per message (0 = unlimited) |

### Notification Policy

A routing tree that determines which contact point handles each alert.

| Field | Description |
|-------|-------------|
| orgId | Organization ID (default: 1) |
| receiver | Default contact point name (fallback) |
| group_by | Labels to group alerts by |
| group_wait | Wait before first notification for new group |
| group_interval | Wait before sending updated group notification |
| repeat_interval | Wait before re-sending unchanged group |
| routes[] | Child routes with label matchers |

### Route (within Notification Policy)

A child routing rule that matches alerts by labels.

| Field | Description |
|-------|-------------|
| receiver | Contact point name for matched alerts |
| matchers | Label matchers (e.g., `severity = critical`) |
| group_wait | Override parent group_wait |
| group_interval | Override parent group_interval |
| repeat_interval | Override parent repeat_interval |
| continue | If true, continue matching sibling routes |

## Relationships

```
Notification Policy (root)
├── receiver → Contact Point (default)
├── routes[0]
│   ├── matchers: [severity = critical]
│   └── receiver → Contact Point (webhook)
└── routes[1]
    ├── matchers: [severity = warning]
    └── receiver → Contact Point (webhook)

Contact Point
└── receivers[]
    └── Receiver (webhook)
        └── url → Webhook Receiver (Docker service)
```

## State Transitions

Alert notification lifecycle:

```
Alert Fires → group_wait → First Notification Sent
    ↓
group_interval → Group Update Sent (if alerts changed)
    ↓
repeat_interval → Re-notification Sent (if still firing)
    ↓
Alert Resolves → Resolved Notification Sent
```
