# API Contracts: APISIX Admin API Operations

**Feature**: 003-apisix-blue-green
**Date**: 2026-02-07

> These contracts define the APISIX Admin API calls used by deployment and traffic scripts.
> Base URL: `http://localhost:9180/apisix/admin`
> Auth Header: `X-API-KEY: poc-admin-key-2024`

---

## 1. Create Blue Upstream

```
PUT /apisix/admin/upstreams/1
Content-Type: application/json
X-API-KEY: poc-admin-key-2024

{
  "name": "order-blue-v1",
  "desc": "Blue deployment - order-service v1",
  "type": "roundrobin",
  "scheme": "http",
  "nodes": {
    "order-service-blue.ecommerce.svc.cluster.local:8081": 1
  },
  "timeout": {
    "connect": 5,
    "send": 10,
    "read": 10
  },
  "checks": {
    "active": {
      "type": "http",
      "http_path": "/h2-console",
      "healthy": { "interval": 5, "successes": 2 },
      "unhealthy": { "interval": 3, "http_failures": 3 }
    }
  }
}

Response: 201 Created
```

## 2. Create Green Upstream

```
PUT /apisix/admin/upstreams/2
Content-Type: application/json
X-API-KEY: poc-admin-key-2024

{
  "name": "order-green-v2",
  "desc": "Green deployment - order-service v2",
  "type": "roundrobin",
  "scheme": "http",
  "nodes": {
    "order-service-green.ecommerce.svc.cluster.local:8081": 1
  },
  "timeout": {
    "connect": 5,
    "send": 10,
    "read": 10
  },
  "checks": {
    "active": {
      "type": "http",
      "http_path": "/h2-console",
      "healthy": { "interval": 5, "successes": 2 },
      "unhealthy": { "interval": 3, "http_failures": 3 }
    }
  }
}

Response: 201 Created
```

## 3. Create Order API Route (Initial: 100% Blue)

```
PUT /apisix/admin/routes/1
Content-Type: application/json
X-API-KEY: poc-admin-key-2024

{
  "name": "order-api-route",
  "desc": "Order API with blue-green traffic split",
  "uri": "/api/*",
  "methods": ["GET", "POST", "PUT", "DELETE"],
  "upstream_id": "1",
  "plugins": {
    "traffic-split": {
      "rules": [
        {
          "match": [
            { "vars": [["http_X-Canary", "==", "true"]] }
          ],
          "weighted_upstreams": [
            { "upstream_id": "2", "weight": 1 }
          ]
        },
        {
          "weighted_upstreams": [
            { "upstream_id": "2", "weight": 0 },
            { "weight": 100 }
          ]
        }
      ]
    }
  }
}

Response: 201 Created
```

## 4. Switch to Canary (90/10)

```
PATCH /apisix/admin/routes/1
Content-Type: application/json
X-API-KEY: poc-admin-key-2024

{
  "plugins": {
    "traffic-split": {
      "rules": [
        {
          "match": [
            { "vars": [["http_X-Canary", "==", "true"]] }
          ],
          "weighted_upstreams": [
            { "upstream_id": "2", "weight": 1 }
          ]
        },
        {
          "weighted_upstreams": [
            { "upstream_id": "2", "weight": 10 },
            { "weight": 90 }
          ]
        }
      ]
    }
  }
}

Response: 200 OK
```

## 5. Switch to 50/50

```
PATCH /apisix/admin/routes/1
Content-Type: application/json
X-API-KEY: poc-admin-key-2024

{
  "plugins": {
    "traffic-split": {
      "rules": [
        {
          "match": [
            { "vars": [["http_X-Canary", "==", "true"]] }
          ],
          "weighted_upstreams": [
            { "upstream_id": "2", "weight": 1 }
          ]
        },
        {
          "weighted_upstreams": [
            { "upstream_id": "2", "weight": 50 },
            { "weight": 50 }
          ]
        }
      ]
    }
  }
}

Response: 200 OK
```

## 6. Full Cutover to Green (0/100)

```
PATCH /apisix/admin/routes/1
Content-Type: application/json
X-API-KEY: poc-admin-key-2024

{
  "plugins": {
    "traffic-split": {
      "rules": [
        {
          "match": [
            { "vars": [["http_X-Canary", "==", "true"]] }
          ],
          "weighted_upstreams": [
            { "upstream_id": "2", "weight": 1 }
          ]
        },
        {
          "weighted_upstreams": [
            { "upstream_id": "2", "weight": 100 },
            { "weight": 0 }
          ]
        }
      ]
    }
  }
}

Response: 200 OK
```

## 7. Rollback to Blue (100/0)

Same as contract #3 initial state (weight 0 for Green, weight 100 for Blue).

## 8. Create OpenTelemetry Global Rule

```
PUT /apisix/admin/global_rules/1
Content-Type: application/json
X-API-KEY: poc-admin-key-2024

{
  "plugins": {
    "opentelemetry": {
      "sampler": {
        "name": "always_on"
      }
    }
  }
}

Response: 201 Created
```

## 9. Get Current Route Configuration (Status Check)

```
GET /apisix/admin/routes/1
X-API-KEY: poc-admin-key-2024

Response: 200 OK
{
  "value": {
    "name": "order-api-route",
    "plugins": {
      "traffic-split": { ... }
    },
    ...
  }
}
```

## 10. Additional Routes for Admin Endpoints

### Payment Service Admin (simulate-delay)

```
PUT /apisix/admin/upstreams/3
Content-Type: application/json
X-API-KEY: poc-admin-key-2024

{
  "name": "payment-service",
  "type": "roundrobin",
  "nodes": {
    "payment-service.ecommerce.svc.cluster.local:8084": 1
  }
}

PUT /apisix/admin/routes/2
Content-Type: application/json
X-API-KEY: poc-admin-key-2024

{
  "name": "payment-admin-route",
  "uri": "/payment/*",
  "upstream_id": "3",
  "plugins": {
    "proxy-rewrite": {
      "regex_uri": ["^/payment/(.*)", "/api/$1"]
    }
  }
}
```

### Notification Service Admin (simulate-failure)

```
PUT /apisix/admin/upstreams/4
Content-Type: application/json
X-API-KEY: poc-admin-key-2024

{
  "name": "notification-service",
  "type": "roundrobin",
  "nodes": {
    "notification-service.ecommerce.svc.cluster.local:8085": 1
  }
}

PUT /apisix/admin/routes/3
Content-Type: application/json
X-API-KEY: poc-admin-key-2024

{
  "name": "notification-admin-route",
  "uri": "/notification/*",
  "upstream_id": "4",
  "plugins": {
    "proxy-rewrite": {
      "regex_uri": ["^/notification/(.*)", "/api/$1"]
    }
  }
}
```
