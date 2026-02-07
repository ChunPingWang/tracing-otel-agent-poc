# TECH：電子商務分散式追蹤 PoC — OpenTelemetry Java Agent 技術規格

## 文件資訊

| 項目 | 內容 |
|------|------|
| 專案名稱 | E-Commerce Distributed Tracing PoC (OTel Agent) |
| 版本 | v1.2 |
| 日期 | 2026-02-07 |
| 對應 PRD | PRD v1.3 |

---

## 1. 技術架構總覽

```
┌──────────────────────────────────────────────────────────────────────────┐
│                            Docker Network                                │
│                                                                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌────────────────┐ │
│  │   Order      │  │  Product    │  │  Inventory   │  │  Payment       │ │
│  │   Service    │→ │  Service    │  │  Service     │  │  Service       │ │
│  │  :8081       │→ │  :8082      │  │  :8083       │  │  :8084         │ │
│  │ +OTel Agent  │→ │ +OTel Agent │  │ +OTel Agent  │  │ +OTel Agent    │ │
│  └──────┬───────┘  └──────┬──────┘  └──────┬───────┘  └───────┬────────┘ │
│         │                 │                │                  │          │
│         │   Kafka Produce │                │                  │          │
│         ├─────────────────┼────────────────┼──────────────────┘          │
│         │                 │                │                             │
│         ▼                 │                │                             │
│  ┌─────────────────┐      │                │                             │
│  │  Kafka Broker    │      │                │                             │
│  │  :9092           │      │                │                             │
│  │ ┌─────────────┐  │      │                │                             │
│  │ │order-       │  │      │                │                             │
│  │ │confirmed    │  │      │                │                             │
│  │ │             │  │      │                │                             │
│  │ │order-       │  │      │                │                             │
│  │ │confirmed.DLT│  │      │                │                             │
│  │ └─────────────┘  │      │                │                             │
│  └────────┬─────────┘      │                │                             │
│           │ Kafka Consume  │                │                             │
│           ▼                │                │                             │
│  ┌────────────────┐        │                │                             │
│  │ Notification   │        │                │                             │
│  │ Service :8085  │        │                │                             │
│  │ +OTel Agent    │        │                │                             │
│  └────────┬───────┘        │                │                             │
│           │                │                │                             │
│           └────────┬───────┴────────┬───────┘                             │
│                    │  OTLP/gRPC     │                                     │
│                    ▼  (:4317)       ▼                                     │
│           ┌──────────────────────────────────┐                           │
│           │       Jaeger All-in-One          │                           │
│           │       UI: :16686                 │                           │
│           │       OTLP gRPC: :4317           │                           │
│           └──────────────────────────────────┘                           │
└──────────────────────────────────────────────────────────────────────────┘
```

---

## 2. 技術棧

| 元件 | 技術 | 版本 | 說明 |
|------|------|------|------|
| Runtime | JDK | 8 (OpenJDK) | 既有環境限制 |
| Framework | Spring Boot | 2.7.18 | 最後的 2.x 穩定版 |
| HTTP Client | RestTemplate | Spring Boot 內建 | 服務間同步呼叫 |
| Messaging | Spring Kafka | 2.9.x | 非同步事件驅動通訊 |
| Message Broker | Apache Kafka | 3.6.x (KRaft) | 無 ZooKeeper 模式，簡化 PoC 部署 |
| Database | H2 | 內嵌 | PoC 用途，簡化部署 |
| ORM | Spring Data JPA | 2.7.x | 資料存取 |
| Tracing Agent | OpenTelemetry Java Agent | 1.32.1 | 最後支援 JDK 8 的版本 |
| Tracing Backend | Jaeger | latest (all-in-one) | 接收 OTLP 並提供 UI |
| Container | Docker / Docker Compose | 最新穩定版 | PoC 環境編排 |
| Local K8s | Kind (Kubernetes in Docker) | v0.20+ | 本地 K8s 叢集部署驗證 |
| K8s CLI | kubectl | v1.28+ | Kubernetes 資源管理 |
| Ingress Controller | NGINX Ingress Controller | Kind 相容版本 | K8s 外部流量路由（HTTP） |
| API Gateway | Apache APISIX | 3.9.x | 藍綠部署流量管理、加權路由、Header 路由 |
| Gateway Config Store | etcd | 3.5.x | APISIX 動態配置儲存 |
| Package Manager | Helm | 3.14+ | APISIX Helm Chart 安裝 |

---

## 3. 服務設計

### 3.1 服務清單

| 服務名稱 | Port | 職責 | 資料庫表 |
|----------|------|------|----------|
| order-service | 8081 | 訂單編排（Orchestrator）+ Kafka Producer | orders |
| product-service | 8082 | 商品目錄與價格查詢 | products |
| inventory-service | 8083 | 庫存管理與預扣 | inventory |
| payment-service | 8084 | 支付處理（模擬） | payments |
| notification-service | 8085 | 訂單通知（Kafka Consumer） | notifications |

### 3.2 API 設計

#### Order Service（Orchestrator）

```
POST /api/orders
Request:
{
  "customerId": "C001",
  "items": [
    { "productId": "P001", "quantity": 2 }
  ]
}
Response:
{
  "orderId": "ORD-20260207-001",
  "status": "CONFIRMED",
  "totalAmount": 1990.00,
  "traceId": "abc123..."     ← 從 OTel Context 取得，便於追蹤
}
```

#### Product Service

```
GET /api/products/{productId}
Response:
{
  "productId": "P001",
  "name": "無線藍牙耳機",
  "price": 995.00,
  "available": true
}
```

#### Inventory Service

```
POST /api/inventory/reserve
Request:  { "productId": "P001", "quantity": 2 }
Response: { "reserved": true, "remainingStock": 48 }

POST /api/inventory/release
Request:  { "productId": "P001", "quantity": 2 }
Response: { "released": true }
```

#### Payment Service

```
POST /api/payments
Request:  { "orderId": "ORD-...", "amount": 1990.00 }
Response: { "paymentId": "PAY-001", "status": "SUCCESS" }
```

#### Notification Service（Kafka Consumer，無對外 REST API）

```
Kafka Consumer: Topic = order-confirmed
Event Payload:
{
  "orderId": "ORD-20260207-001",
  "customerId": "C001",
  "totalAmount": 1990.00,
  "timestamp": "2026-02-07T10:30:45Z"
}

Internal:
  → 查詢 DB 取得客戶聯絡資訊
  → 模擬發送通知
  → 將通知記錄寫入 DB (notifications 表)
```

#### 管理端點（所有服務共用）

```
POST /api/admin/simulate-delay?ms=5000      ← Payment Service: 延遲模擬
POST /api/admin/simulate-failure?enabled=true ← Notification Service: 失敗模擬
```

### 3.3 呼叫鏈路

```
Client
  └→ POST /api/orders (order-service)
       ├→ GET /api/products/{id} (product-service)
       │    └→ [JDBC] SELECT * FROM products (H2)
       ├→ POST /api/inventory/reserve (inventory-service)
       │    └→ [JDBC] UPDATE inventory SET ... (H2)
       ├→ POST /api/payments (payment-service)
       │    └→ [JDBC] INSERT INTO payments ... (H2)
       ├→ [JDBC] UPDATE orders SET status='CONFIRMED' (H2)
       │
       └→ [Kafka Produce] → Topic: order-confirmed
                               │
                               └→ [Kafka Consume] notification-service
                                    ├→ [JDBC] SELECT * FROM customers (H2)
                                    ├→ (模擬通知發送)
                                    └→ [JDBC] INSERT INTO notifications ... (H2)
```

**Trace Span 結構預覽（Jaeger UI）：**

```
order-service: POST /api/orders                          [───────────────────────]
  ├─ product-service: GET /api/products/P001               [────]
  │    └─ H2: SELECT                                         [─]
  ├─ inventory-service: POST /api/inventory/reserve            [─────]
  │    └─ H2: UPDATE                                             [──]
  ├─ payment-service: POST /api/payments                           [───────]
  │    └─ H2: INSERT                                                  [──]
  ├─ H2: UPDATE orders                                                     [─]
  └─ kafka.produce: order-confirmed                                        [─]
       └─ notification-service: kafka.consume: order-confirmed               [────────]
            ├─ H2: SELECT customers                                            [──]
            └─ H2: INSERT notifications                                             [──]
```

---

## 4. OpenTelemetry Java Agent 整合

### 4.1 Agent 下載與版本鎖定

```bash
# 下載 OTel Java Agent 1.32.1（最後支援 JDK 8）
curl -L -o opentelemetry-javaagent.jar \
  https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v1.32.1/opentelemetry-javaagent.jar
```

### 4.2 Agent 啟動參數

```bash
java -javaagent:/opt/otel/opentelemetry-javaagent.jar \
     -Dotel.service.name=order-service \
     -Dotel.exporter.otlp.endpoint=http://jaeger:4317 \
     -Dotel.exporter.otlp.protocol=grpc \
     -Dotel.traces.exporter=otlp \
     -Dotel.metrics.exporter=none \
     -Dotel.logs.exporter=none \
     -Dotel.resource.attributes=service.namespace=ecommerce-poc,deployment.environment=poc \
     -jar order-service.jar
```

### 4.3 自動攔截範圍

OTel Java Agent 1.32.1 自動攔截以下框架（無需程式碼變更）：

| 框架 | 攔截內容 | 產生的 Span 屬性 |
|------|----------|------------------|
| Spring MVC | Inbound HTTP 請求 | http.method, http.route, http.status_code |
| RestTemplate | Outbound HTTP 呼叫 | http.method, http.url, http.status_code |
| JDBC | 資料庫查詢 | db.system, db.statement, db.name |
| Spring Data | Repository 操作 | 自動關聯到 JDBC Span |
| Spring Kafka (Producer) | KafkaTemplate.send() | messaging.system=kafka, messaging.destination, messaging.kafka.partition |
| Spring Kafka (Consumer) | @KafkaListener | messaging.system=kafka, messaging.destination, messaging.kafka.consumer_group, messaging.kafka.partition |
| Kafka Client (原生) | Producer/Consumer API | 同上，Agent 直接攔截 Kafka Client 層 |

### 4.4 Context Propagation

Agent 預設使用 W3C Trace Context（`traceparent` header）進行跨服務的 Context 傳播：

```
traceparent: 00-<trace-id>-<span-id>-01
```

RestTemplate 呼叫時，Agent 自動注入此 Header；接收端 Agent 自動解析並建立 parent-child Span 關係。

**Kafka Context Propagation：** Agent 同樣自動在 Kafka Message Header 中注入 `traceparent`。Consumer 端 Agent 解析 Header 後，Consumer Span 自動成為 Producer Span 的子 Span，實現跨非同步邊界的 Trace 串聯。

```
Producer (order-service)                     Consumer (notification-service)
  │                                             │
  │ KafkaTemplate.send()                        │ @KafkaListener
  │   → Agent 注入 Kafka Header:                │   → Agent 解析 Kafka Header:
  │     traceparent: 00-<traceId>-<spanId>-01   │     traceparent: 00-<traceId>-<spanId>-01
  │                                             │   → 建立 child Span
  │   ──── Kafka Message ────────────────────→  │
```

---

## 5. Docker Compose 部署

### 5.1 docker-compose.yml 結構

```yaml
version: '3.8'

services:
  jaeger:
    image: jaegertracing/all-in-one:latest
    ports:
      - "16686:16686"   # Jaeger UI
      - "4317:4317"     # OTLP gRPC
    environment:
      - COLLECTOR_OTLP_ENABLED=true

  kafka:
    image: apache/kafka:3.6.2
    ports:
      - "9092:9092"
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"

  order-service:
    build: ./order-service
    ports:
      - "8081:8081"
    environment:
      - JAVA_TOOL_OPTIONS=-javaagent:/opt/otel/opentelemetry-javaagent.jar
      - OTEL_SERVICE_NAME=order-service
      - OTEL_EXPORTER_OTLP_ENDPOINT=http://jaeger:4317
      - OTEL_EXPORTER_OTLP_PROTOCOL=grpc
      - OTEL_TRACES_EXPORTER=otlp
      - OTEL_METRICS_EXPORTER=none
      - OTEL_LOGS_EXPORTER=none
      - PRODUCT_SERVICE_URL=http://product-service:8082
      - INVENTORY_SERVICE_URL=http://inventory-service:8083
      - PAYMENT_SERVICE_URL=http://payment-service:8084
      - SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
    depends_on:
      - jaeger
      - kafka
      - product-service
      - inventory-service
      - payment-service

  product-service:
    build: ./product-service
    ports:
      - "8082:8082"
    environment:
      - JAVA_TOOL_OPTIONS=-javaagent:/opt/otel/opentelemetry-javaagent.jar
      - OTEL_SERVICE_NAME=product-service
      - OTEL_EXPORTER_OTLP_ENDPOINT=http://jaeger:4317
      - OTEL_TRACES_EXPORTER=otlp
      - OTEL_METRICS_EXPORTER=none
      - OTEL_LOGS_EXPORTER=none
    depends_on:
      - jaeger

  inventory-service:
    build: ./inventory-service
    ports:
      - "8083:8083"
    environment:
      - JAVA_TOOL_OPTIONS=-javaagent:/opt/otel/opentelemetry-javaagent.jar
      - OTEL_SERVICE_NAME=inventory-service
      - OTEL_EXPORTER_OTLP_ENDPOINT=http://jaeger:4317
      - OTEL_TRACES_EXPORTER=otlp
      - OTEL_METRICS_EXPORTER=none
      - OTEL_LOGS_EXPORTER=none
    depends_on:
      - jaeger

  payment-service:
    build: ./payment-service
    ports:
      - "8084:8084"
    environment:
      - JAVA_TOOL_OPTIONS=-javaagent:/opt/otel/opentelemetry-javaagent.jar
      - OTEL_SERVICE_NAME=payment-service
      - OTEL_EXPORTER_OTLP_ENDPOINT=http://jaeger:4317
      - OTEL_TRACES_EXPORTER=otlp
      - OTEL_METRICS_EXPORTER=none
      - OTEL_LOGS_EXPORTER=none
    depends_on:
      - jaeger

  notification-service:
    build: ./notification-service
    ports:
      - "8085:8085"
    environment:
      - JAVA_TOOL_OPTIONS=-javaagent:/opt/otel/opentelemetry-javaagent.jar
      - OTEL_SERVICE_NAME=notification-service
      - OTEL_EXPORTER_OTLP_ENDPOINT=http://jaeger:4317
      - OTEL_TRACES_EXPORTER=otlp
      - OTEL_METRICS_EXPORTER=none
      - OTEL_LOGS_EXPORTER=none
      - SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
    depends_on:
      - jaeger
      - kafka
```

### 5.2 Dockerfile 範本

```dockerfile
FROM openjdk:8-jre-slim

WORKDIR /app

# 下載 OTel Agent
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v1.32.1/opentelemetry-javaagent.jar /opt/otel/opentelemetry-javaagent.jar

COPY target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 5.3 Kind (Kubernetes) 部署

除 Docker Compose 外，本 PoC 亦支援部署至 Kind 本地 Kubernetes 叢集，以驗證 K8s 環境下的分散式追蹤行為。

**部署架構：**

- 單節點 Kind 叢集
- NGINX Ingress Controller 處理外部 HTTP 流量
- 專用 Kubernetes Namespace 隔離 PoC 資源
- 每個微服務為獨立的 Deployment + Service (ClusterIP)
- Kafka 與 Jaeger 同樣部署於叢集內
- 透過 Ingress 路由規則將 Jaeger UI、Order Service API 及管理端點暴露至主機

**Ingress 路由設計：**

| 主機名稱/路徑 | 後端 Service | 說明 |
|----------------|-------------|------|
| 依 Ingress 規則配置 | order-service:8081 | 訂單 API |
| 依 Ingress 規則配置 | payment-service:8084 | 支付管理端點 |
| 依 Ingress 規則配置 | notification-service:8085 | 通知管理端點 |
| 依 Ingress 規則配置 | jaeger-query:16686 | Jaeger UI |

> 具體的 host-based 或 path-based 路由方案將在實作規劃階段決定。

**與 Docker Compose 的差異：**

| 面向 | Docker Compose | Kind K8s |
|------|----------------|----------|
| 服務發現 | Docker DNS (容器名稱) | Kubernetes DNS (Service 名稱) |
| 網路模型 | Docker bridge network | Kubernetes Pod network |
| 外部存取 | Port mapping | Ingress Controller (HTTP 80/443) |
| 流量路由 | 直接 port 對應 | Ingress 規則 (host/path-based routing) |
| 組態管理 | environment 區塊 | Deployment env / ConfigMap |
| 生命週期 | docker-compose up/down | 部署腳本 (create/delete Kind cluster) |

**K8s Manifest 目錄結構：**

```
k8s/
├── kind-config.yaml          # Kind 叢集設定（extraPortMappings: 80, 443）
├── namespace.yaml             # Namespace 定義
├── ingress.yaml               # Ingress 路由規則
├── jaeger/                    # Jaeger Deployment + Service
├── kafka/                     # Kafka Deployment + Service
├── order-service/             # Deployment + Service
├── product-service/           # Deployment + Service
├── inventory-service/         # Deployment + Service
├── payment-service/           # Deployment + Service
└── notification-service/      # Deployment + Service
```

**部署與銷毀腳本：**

```bash
# 一鍵部署（含 Kind 叢集建立 + Ingress Controller 安裝 + 映像建構載入 + K8s 資源部署）
scripts/kind-deploy.sh

# 一鍵銷毀
scripts/kind-teardown.sh
```

### 5.4 Apache APISIX 藍綠部署

本 PoC 額外提供以 Apache APISIX 作為 API Gateway 的藍綠部署方案，驗證在 Gateway 層實現流量切換的可行性。

**部署架構：**

```
Client
  └→ APISIX Gateway (:9080)
       ├→ [weight: 90] Blue (v1) order-service → product/inventory/payment/notification
       └→ [weight: 10] Green (v2) order-service → product/inventory/payment/notification
                                                      ↓
                                                  Jaeger (OTLP)
```

**核心元件：**

| 元件 | 說明 |
|------|------|
| APISIX Gateway | Data Plane，處理 HTTP 請求路由與流量分配 |
| etcd | APISIX 配置儲存（單節點，無持久化） |
| APISIX Admin API | Control Plane，透過 REST API 動態更新路由/上游設定 |
| Blue (v1) order-service | 當前穩定版本 |
| Green (v2) order-service | 待驗證新版本 |

**流量分配場景：**

| 場景 | Blue 權重 | Green 權重 | 用途 |
|------|-----------|------------|------|
| 全量 Blue | 100 | 0 | 初始狀態 / 回滾 |
| 金絲雀 | 90 | 10 | 小流量驗證 |
| 均分 | 50 | 50 | 對比測試 |
| 全量 Green | 0 | 100 | 完成切換 |
| Header 路由 | 預設 Blue | X-Canary: true → Green | QA 定向測試 |

**APISIX 插件使用：**

- `traffic-split`：加權流量分配與 Header 條件路由
- `proxy-rewrite`：請求路徑改寫
- `opentelemetry`：Gateway 層 Trace Context 透傳與 Span 產生

**部署腳本：**

```bash
# 一鍵部署（Kind + APISIX + Blue/Green + Jaeger + 支援服務）
scripts/apisix-deploy.sh

# 流量切換便捷指令
scripts/apisix-canary.sh          # 90/10 金絲雀
scripts/apisix-cutover.sh         # 0/100 全量切換
scripts/apisix-rollback.sh        # 100/0 回滾

# 一鍵銷毀
scripts/apisix-teardown.sh
```

---

## 6. 測試場景對應

### 6.1 場景一：正常下單

```bash
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"C001","items":[{"productId":"P001","quantity":2}]}'
```

**Jaeger 預期結果：** 1 個 Trace，包含 7+ 個 Span（order 入口、product 呼叫、inventory 呼叫、payment 呼叫、各服務的 DB 操作）。

### 6.2 場景二：庫存不足

```bash
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"C001","items":[{"productId":"P999","quantity":999}]}'
```

**Jaeger 預期結果：** Trace 中 inventory-service 的 Span 帶有 `error=true` 和 `otel.status_code=ERROR`。

### 6.3 場景三：支付超時

Payment Service 內建延遲模擬端點：

```bash
# 啟用延遲模擬（5 秒）
curl -X POST http://localhost:8084/api/admin/simulate-delay?ms=5000

# 觸發下單
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"C001","items":[{"productId":"P001","quantity":1}]}'
```

**Jaeger 預期結果：** payment-service Span 的 duration > 3s，整個 Trace duration 明顯偏長。

### 6.4 場景四：訂單確認後 Kafka 非同步通知

```bash
# 正常下單（觸發完整同步 + 非同步鏈路）
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"C001","items":[{"productId":"P001","quantity":2}]}'
```

**Jaeger 預期結果：**

- 1 條 Trace 包含 5 個服務的 Span
- 同步段：order → product → inventory → payment（HTTP Span）
- 非同步段：order kafka.produce → notification kafka.consume（Kafka Span）
- notification-service 內包含 2 個 JDBC Span（SELECT customers + INSERT notifications）
- Kafka Span 屬性：`messaging.system=kafka`, `messaging.destination=order-confirmed`

### 6.5 場景五：Kafka 消費失敗與 DLT

```bash
# 啟用 Notification Service 失敗模擬
curl -X POST http://localhost:8085/api/admin/simulate-failure?enabled=true

# 觸發下單
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"C001","items":[{"productId":"P001","quantity":1}]}'

# 等待重試完成（約 10-15 秒），然後關閉失敗模擬
curl -X POST http://localhost:8085/api/admin/simulate-failure?enabled=false
```

**Jaeger 預期結果：**

- Trace 中 notification-service 有多個 Consumer Span（原始 + 重試）
- 每個 Consumer Span 帶有 `error=true` 和 `otel.status_code=ERROR`
- 最後一個 Span 為 DLT Producer：`messaging.destination=order-confirmed.DLT`

---

## 7. 效能基準測試

### 7.1 測試方法

使用 Apache Bench 或 wrk 對 Order Service 進行壓力測試，分別在有 Agent 和無 Agent 的情境下量測。

```bash
# 無 Agent 基線
wrk -t4 -c50 -d60s http://localhost:8081/api/orders

# 有 Agent
wrk -t4 -c50 -d60s http://localhost:8081/api/orders
```

### 7.2 量測指標

| 指標 | 無 Agent | 有 Agent | 差異 |
|------|----------|----------|------|
| Avg Latency (ms) | — | — | < 5% |
| P99 Latency (ms) | — | — | < 10% |
| Throughput (req/s) | — | — | < 5% 下降 |
| JVM Startup Time (s) | — | — | < 10s 增加 |
| Heap Usage (MB) | — | — | 記錄 |

---

## 8. 專案結構

```
poc1-otel-agent/
├── docker-compose.yml
├── order-service/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/poc/order/
│       ├── OrderApplication.java
│       ├── controller/OrderController.java
│       ├── service/OrderService.java
│       ├── event/OrderConfirmedEvent.java
│       ├── event/OrderEventPublisher.java     ← KafkaTemplate 發送事件
│       ├── model/Order.java
│       ├── repository/OrderRepository.java
│       └── dto/CreateOrderRequest.java
├── product-service/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/poc/product/
│       ├── ProductApplication.java
│       ├── controller/ProductController.java
│       ├── model/Product.java
│       └── repository/ProductRepository.java
├── inventory-service/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/poc/inventory/
│       ├── InventoryApplication.java
│       ├── controller/InventoryController.java
│       ├── service/InventoryService.java
│       ├── model/Inventory.java
│       └── repository/InventoryRepository.java
├── payment-service/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/poc/payment/
│       ├── PaymentApplication.java
│       ├── controller/PaymentController.java
│       ├── service/PaymentService.java
│       ├── model/Payment.java
│       └── repository/PaymentRepository.java
└── notification-service/
    ├── Dockerfile
    ├── pom.xml
    └── src/main/java/com/poc/notification/
        ├── NotificationApplication.java
        ├── listener/OrderConfirmedListener.java  ← @KafkaListener
        ├── service/NotificationService.java
        ├── model/Notification.java
        ├── repository/NotificationRepository.java
        └── controller/AdminController.java        ← 失敗模擬開關
```

---

## 9. Maven 依賴（各服務共用）

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.7.18</version>
</parent>

<properties>
    <java.version>1.8</java.version>
</properties>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <!-- Order Service 與 Notification Service 額外需要 -->
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>runtime</scope>
    </dependency>
</dependencies>
```

> **注意：無需加入任何 OpenTelemetry 的 Maven 依賴**，Kafka 與 JDBC 的追蹤同樣由 Agent 自動完成，這正是 Java Agent 方案的核心優勢。

---

## 10. 注意事項與限制

1. **OTel Agent 版本鎖定**：必須使用 1.32.1，2.x 版本不支援 JDK 8
2. **Sampling 策略**：PoC 預設 100% 取樣；正式環境需調整為 `parentbased_traceidratio` 並設定合理取樣率
3. **Agent 升級路徑**：未來升級 JDK 17 後可直接切換到 OTel Agent 2.x，無需改程式碼
4. **Jaeger 儲存**：PoC 使用記憶體儲存（all-in-one 預設），正式環境需配置 Elasticsearch 或 Cassandra
5. **TraceID 日誌關聯**：Agent 可自動將 TraceID 注入 MDC（需 Logback/Log4j2 設定），PoC 暫不涵蓋
6. **Kafka Context Propagation**：Agent 自動在 Kafka Record Header 注入 `traceparent`，每筆訊息增加約 55 bytes，高吞吐場景需評估影響
7. **Kafka Consumer Span 類型**：Agent 預設為每筆 record 建立獨立 Span。批次消費場景下，每批 N 筆訊息會產生 N 個 Consumer Span
8. **JDBC Span 敏感資料**：Agent 預設記錄完整 SQL 語句（含參數）。正式環境可透過 `-Dotel.instrumentation.jdbc-datasource.enabled=false` 或設定 `db.statement` 清洗規則
9. **Kafka 零侵入驗證重點**：本 PoC 的核心驗證之一是 Kafka 追蹤同樣不需修改任何程式碼，與 HTTP 追蹤享有完全相同的零侵入特性
10. **Kind K8s 部署**：本 PoC 同時提供 Kind 本地 Kubernetes 部署方案，複用既有 Docker 映像檔，新增 K8s manifests、Ingress 路由規則與部署腳本。Kind 叢集使用單節點配置，含 NGINX Ingress Controller 處理外部 HTTP 流量，不含 PV/PVC、TLS 等正式環境元件
11. **Ingress Controller 與 Trace Context**：NGINX Ingress Controller 預設會轉發 HTTP Header（包含 W3C `traceparent`），不會中斷分散式追蹤的 Context Propagation
12. **APISIX 藍綠部署**：APISIX 的 `traffic-split` 插件支援加權流量分配與條件路由，配置變更透過 Admin API 即時生效，無需重啟 Gateway。APISIX 的 `opentelemetry` 插件可在 Gateway 層產生 Span 並透傳 W3C Trace Context
13. **APISIX 與 Ingress 的關係**：Feature 002 使用 NGINX Ingress Controller 作為 K8s 外部流量入口；Feature 003 使用 APISIX 作為 API Gateway 實現藍綠部署。兩者為獨立的部署方案，各自有專屬的 Kind 叢集與部署腳本
