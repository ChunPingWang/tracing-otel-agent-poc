# Data Model: E-Commerce Distributed Tracing PoC

**Date**: 2026-02-07
**Branch**: `001-otel-distributed-tracing`

## Entity Overview

```text
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│    Order      │────>│   Product    │     │  Inventory   │
│  (order-svc)  │     │ (product-svc)│     │(inventory-svc)│
└──────┬───────┘     └──────────────┘     └──────────────┘
       │
       ├─────────────────────┐
       │                     │
       ▼                     ▼
┌──────────────┐     ┌──────────────┐
│   Payment    │     │ Notification │
│ (payment-svc)│     │(notif-svc)   │
└──────────────┘     └──────┬───────┘
                            │
                            ▼
                     ┌──────────────┐
                     │   Customer   │
                     │ (notif-svc)  │
                     └──────────────┘
```

## Entities by Bounded Context

### Order Service (order-service)

#### Order (Aggregate Root)

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | Long | PK, auto-generated | 內部 ID |
| orderId | String | Unique, not null | 業務訂單編號 (e.g., ORD-20260207-001) |
| customerId | String | Not null | 客戶識別碼 |
| status | OrderStatus | Not null | 訂單狀態 |
| totalAmount | BigDecimal | Not null, >= 0 | 訂單總金額 |
| createdAt | LocalDateTime | Not null | 建立時間 |
| updatedAt | LocalDateTime | Not null | 最後更新時間 |

**State Transitions (OrderStatus)**:

```text
CREATED ──────────────────→ CONFIRMED
   │                             │
   ├──→ FAILED                   └──→ (Kafka event published)
   │   (庫存不足)
   │
   └──→ PAYMENT_TIMEOUT
       (支付超時 → 庫存回滾)
```

| From | To | Trigger |
|------|----|---------|
| (new) | CREATED | Order 建立 |
| CREATED | CONFIRMED | 支付成功 |
| CREATED | FAILED | 庫存不足 |
| CREATED | PAYMENT_TIMEOUT | 支付超時 (3s) |

#### OrderItem (Entity, embedded in Order)

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| productId | String | Not null | 商品編號 |
| quantity | Integer | Not null, > 0 | 購買數量 |
| unitPrice | BigDecimal | Nullable | 單價（查詢 Product Service 後填入） |

### Product Service (product-service)

#### Product

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | Long | PK, auto-generated | 內部 ID |
| productId | String | Unique, not null | 商品編號 (e.g., P001) |
| name | String | Not null | 商品名稱 |
| price | BigDecimal | Not null, >= 0 | 商品價格 |
| available | Boolean | Not null | 是否可購買 |

**Initial Data (data.sql)**:

| productId | name | price | available |
|-----------|------|-------|-----------|
| P001 | 無線藍牙耳機 | 995.00 | true |
| P002 | USB-C 充電線 | 299.00 | true |
| P003 | 螢幕保護貼 | 199.00 | true |

### Inventory Service (inventory-service)

#### Inventory

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | Long | PK, auto-generated | 內部 ID |
| productId | String | Unique, not null | 商品編號 |
| availableStock | Integer | Not null, >= 0 | 可用庫存數量 |
| reservedStock | Integer | Not null, >= 0 | 已預扣數量 |

**Domain Rules**:
- `reserve(quantity)`: availableStock >= quantity → availableStock -= quantity, reservedStock += quantity
- `release(quantity)`: reservedStock >= quantity → reservedStock -= quantity, availableStock += quantity
- 庫存不足時拋出 domain exception

**Initial Data (data.sql)**:

| productId | availableStock | reservedStock |
|-----------|---------------|---------------|
| P001 | 50 | 0 |
| P002 | 100 | 0 |
| P003 | 200 | 0 |

### Payment Service (payment-service)

#### Payment

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | Long | PK, auto-generated | 內部 ID |
| paymentId | String | Unique, not null | 支付編號 (e.g., PAY-001) |
| orderId | String | Not null | 關聯訂單編號 |
| amount | BigDecimal | Not null, >= 0 | 支付金額 |
| status | PaymentStatus | Not null | 支付狀態 |
| createdAt | LocalDateTime | Not null | 建立時間 |

**State Transitions (PaymentStatus)**:

```text
(new) → SUCCESS
(new) → FAILED
```

### Notification Service (notification-service)

#### Customer (Reference Data)

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | Long | PK, auto-generated | 內部 ID |
| customerId | String | Unique, not null | 客戶識別碼 |
| name | String | Not null | 客戶姓名 |
| email | String | Not null | 電子郵件 |
| phone | String | Nullable | 電話號碼 |

**Initial Data (data.sql)**:

| customerId | name | email | phone |
|------------|------|-------|-------|
| C001 | 王小明 | wang@example.com | 0912-345-678 |
| C002 | 李小華 | lee@example.com | 0923-456-789 |

#### Notification

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | Long | PK, auto-generated | 內部 ID |
| orderId | String | Not null | 關聯訂單編號 |
| customerId | String | Not null | 客戶識別碼 |
| customerEmail | String | Not null | 通知目標 email |
| status | NotificationStatus | Not null | 通知狀態 |
| message | String | Not null | 通知內容 |
| createdAt | LocalDateTime | Not null | 建立時間 |

**State Transitions (NotificationStatus)**:

```text
(new) → SENT
(new) → FAILED
```

## Kafka Event Schema

### OrderConfirmedEvent (Topic: order-confirmed)

| Field | Type | Description |
|-------|------|-------------|
| orderId | String | 訂單編號 |
| customerId | String | 客戶識別碼 |
| customerEmail | String | 客戶電子郵件 |
| items | List\<OrderItemDto\> | 商品清單 |
| totalAmount | BigDecimal | 訂單總金額 |
| status | String | 訂單狀態 ("CONFIRMED") |
| timestamp | String (ISO 8601) | 事件發生時間 |

#### OrderItemDto (embedded)

| Field | Type | Description |
|-------|------|-------------|
| productId | String | 商品編號 |
| quantity | Integer | 購買數量 |
| unitPrice | BigDecimal | 單價 |

## Cross-Service Relationships

| Relationship | Type | Description |
|-------------|------|-------------|
| Order → Product | HTTP GET | 查詢商品價格與可用性 |
| Order → Inventory | HTTP POST | 庫存預扣 / 回滾 |
| Order → Payment | HTTP POST | 發起支付 |
| Order → Notification | Kafka Event | 訂單確認事件 (order-confirmed) |
| Notification → Customer | Local DB | 查詢客戶聯絡資訊 |
