# E-Commerce Distributed Tracing PoC -- OpenTelemetry Java Agent

> 使用 OpenTelemetry Java Agent **零侵入**方式，為 Spring Boot 2 / JDK 8 微服務導入分散式追蹤（Distributed Tracing），涵蓋同步 HTTP、非同步 Kafka、JDBC 三種通訊模式的端到端可觀測性。

---

## 目錄

- [專案簡介](#專案簡介)
- [系統架構](#系統架構)
- [技術棧](#技術棧)
- [服務清單與連接埠](#服務清單與連接埠)
- [快速開始](#快速開始)
- [測試場景](#測試場景)
- [Jaeger UI 操作指南](#jaeger-ui-操作指南)
- [專案結構（六角形架構）](#專案結構六角形架構)
- [執行測試](#執行測試)
- [效能基準測試腳本](#效能基準測試腳本)
- [環境清理](#環境清理)
- [常見問題（FAQ）](#常見問題faq)
- [參考資料](#參考資料)

---

## 專案簡介

本專案是一個**概念驗證（Proof of Concept）**，目標是驗證在不修改任何業務程式碼的前提下，透過 OpenTelemetry Java Agent 為既有的 Spring Boot 2 微服務系統導入完整的分散式追蹤能力。

**核心驗證目標：**

| 編號 | 目標 | 說明 |
|------|------|------|
| BG-1 | 端到端可視化 | 在 Jaeger UI 看到完整的下單呼叫鏈 |
| BG-2 | 效能瓶頸定位 | 透過 Trace 識別延遲超過 500ms 的服務節點 |
| BG-3 | 零程式碼修改 | 既有服務程式碼無需任何變更 |
| BG-4 | 效能影響評估 | 量化 Agent 對啟動時間與回應時間的影響 |
| BG-5 | Kafka 非同步追蹤 | Kafka Producer -> Consumer 串聯在同一條 Trace |
| BG-6 | DB 存取追蹤 | JDBC 操作自動產生 Span，含 SQL 語句與執行時間 |

---

## 系統架構

```
                          +---------------------+
                          |      Client          |
                          |   (curl / 前端)      |
                          +----------+----------+
                                     |
                                     | POST /api/orders
                                     v
+------------------------------------------------------------------------------------+
|  Docker Network                                                                    |
|                                                                                    |
|  +------------------+    +------------------+    +------------------+               |
|  | Order Service    |    | Product Service  |    | Inventory Service|               |
|  | :8081            |--->| :8082            |    | :8083            |               |
|  | +OTel Agent      |    | +OTel Agent      |    | +OTel Agent      |               |
|  +--------+---------+    +------------------+    +------------------+               |
|           |                                             ^                          |
|           |  GET /api/products/{id}  (同步 HTTP)        |                          |
|           +---------------------------------------------+                          |
|           |  POST /api/inventory/reserve (同步 HTTP)                               |
|           |                                                                        |
|           |              +------------------+                                      |
|           +------------->| Payment Service  |                                      |
|           |              | :8084            |                                      |
|           |              | +OTel Agent      |                                      |
|           |              +------------------+                                      |
|           |  POST /api/payments (同步 HTTP, timeout=3s)                            |
|           |                                                                        |
|           v                                                                        |
|  +------------------+         +----------------------+                             |
|  | Apache Kafka     |-------->| Notification Service |                             |
|  | :9092            |         | :8085                |                             |
|  | (KRaft Mode)     |         | +OTel Agent          |                             |
|  +------------------+         +----------------------+                             |
|     order-confirmed                                                                |
|     (非同步事件)                                                                    |
|                                                                                    |
|  +----------------------------+                                                    |
|  | Jaeger All-in-One          |  <--- 所有服務透過 OTLP/gRPC 回報 Trace            |
|  | UI:        :16686          |                                                    |
|  | OTLP gRPC: :4317           |                                                    |
|  +----------------------------+                                                    |
+------------------------------------------------------------------------------------+
```

**服務呼叫鏈路摘要：**

```
Client
  |
  +---> Order Service (8081)
          |
          +---> Product Service (8082)       -- 查詢商品價格
          +---> Inventory Service (8083)      -- 預扣庫存
          +---> Payment Service (8084)        -- 發起支付（同步，3s timeout）
          +---> Kafka (order-confirmed topic) -- 發布訂單確認事件
                  |
                  +---> Notification Service (8085) -- 消費事件，發送通知
```

**Trace Span 結構（Jaeger UI 中的呈現）：**

```
order-service: POST /api/orders                          [---------------------------]
  +-- product-service: GET /api/products/P001               [----]
  |    +-- H2: SELECT                                         [-]
  +-- inventory-service: POST /api/inventory/reserve            [-----]
  |    +-- H2: UPDATE                                             [--]
  +-- payment-service: POST /api/payments                           [-------]
  |    +-- H2: INSERT                                                  [--]
  +-- H2: UPDATE orders                                                     [-]
  +-- kafka.produce: order-confirmed                                        [-]
       +-- notification-service: kafka.consume                               [--------]
            +-- H2: SELECT customers                                            [--]
            +-- H2: INSERT notifications                                             [--]
```

---

## 技術棧

| 元件 | 技術 | 版本 | 說明 |
|------|------|------|------|
| Runtime | OpenJDK | 8 | 既有環境限制 |
| Framework | Spring Boot | 2.7.18 | 最後的 2.x 穩定版 |
| HTTP Client | RestTemplate | Spring Boot 内建 | 服務間同步呼叫 |
| Messaging | Spring Kafka | 2.9.x | 非同步事件驅動通訊 |
| Message Broker | Apache Kafka | 3.6.x (KRaft) | 無 ZooKeeper 模式 |
| Database | H2 | 内嵌模式 | PoC 用途，簡化部署 |
| ORM | Spring Data JPA | 2.7.x | 資料存取 |
| Tracing Agent | OpenTelemetry Java Agent | 1.32.1 | 最後支援 JDK 8 的版本 |
| Tracing Backend | Jaeger | latest (all-in-one) | OTLP 接收 + 追蹤 UI |
| Container | Docker Compose | 最新穩定版 | 環境編排 |
| Architecture Test | ArchUnit | 最新版 | 六角形架構合規驗證 |
| Unit Test | JUnit 5 | 最新版 | 測試框架 |

---

## 服務清單與連接埠

| 服務 | 連接埠 | 說明 |
|------|--------|------|
| Order Service | 8081 | 訂單編排服務（Orchestrator），負責協調下單流程 |
| Product Service | 8082 | 商品服務，提供商品查詢（唯讀） |
| Inventory Service | 8083 | 庫存服務，處理庫存預扣與回滾 |
| Payment Service | 8084 | 支付服務（模擬），支援延遲模擬 |
| Notification Service | 8085 | 通知服務，消費 Kafka 事件並發送通知 |
| Kafka | 9092 | 訊息佇列（KRaft 模式，無 ZooKeeper） |
| Jaeger UI | 16686 | 分散式追蹤視覺化介面 |
| Jaeger OTLP gRPC | 4317 | OpenTelemetry 資料接收端點 |

---

## 快速開始

### 前置需求

- **Docker Desktop**（包含 Docker Compose）
- **JDK 8**（用於本地建置）
- **Maven 3.6+**（用於本地建置）
- **curl**（用於測試）

### 步驟一：取得原始碼

```bash
git clone <repository-url>
cd tracing-otel-agent-poc
```

### 步驟二：建置所有微服務

```bash
cd order-service && mvn clean package -DskipTests && cd ..
cd product-service && mvn clean package -DskipTests && cd ..
cd inventory-service && mvn clean package -DskipTests && cd ..
cd payment-service && mvn clean package -DskipTests && cd ..
cd notification-service && mvn clean package -DskipTests && cd ..
```

### 步驟三：啟動所有服務

```bash
docker-compose up --build -d
```

等待約 30-60 秒讓所有服務完成啟動。

### 步驟四：驗證服務狀態

```bash
curl -s http://localhost:8081/actuator/health | jq .
curl -s http://localhost:8082/actuator/health | jq .
curl -s http://localhost:8083/actuator/health | jq .
curl -s http://localhost:8084/actuator/health | jq .
curl -s http://localhost:8085/actuator/health | jq .
```

每個服務都應回傳 `{"status":"UP"}`。

### 步驟五：開啟 Jaeger UI

瀏覽器開啟 http://localhost:16686 即可查看分散式追蹤資料。

---

## 測試場景

本專案包含 5 個使用者故事（User Story）驗證場景：

### 場景一：正常下單（Happy Path）

客戶下單，商品有庫存，支付成功，觸發 Kafka 非同步通知。

```bash
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"C001","items":[{"productId":"P001","quantity":2}]}'
```

預期結果：訂單狀態為 `CONFIRMED`，回應中包含 `traceId`。在 Jaeger UI 中可看到涵蓋 5 個服務的完整 Trace。

### 場景二：庫存不足

客戶下單數量超過可用庫存。

```bash
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"C001","items":[{"productId":"P001","quantity":999}]}'
```

預期結果：訂單狀態為 `FAILED`。Jaeger 中 Inventory Service 的 Span 帶有 `error=true`，且不會出現 Payment Service 的呼叫。

### 場景三：支付超時

模擬 Payment Service 高延遲，觸發 Order Service 的 3 秒 timeout。

```bash
# 步驟 1：啟用延遲模擬（5 秒 > 3 秒 timeout）
curl -X POST "http://localhost:8084/api/admin/simulate-delay?ms=5000"

# 步驟 2：觸發下單
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"C001","items":[{"productId":"P001","quantity":1}]}'

# 步驟 3：關閉延遲模擬
curl -X POST "http://localhost:8084/api/admin/simulate-delay?ms=0"
```

預期結果：訂單狀態為 `PAYMENT_TIMEOUT`，Jaeger 中可看到庫存回滾（release）呼叫。

### 場景四：Kafka 非同步通知

正常下單會自動觸發 Kafka 事件，驗證同步與非同步鏈路在同一條 Trace 中串聯。

```bash
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"C001","items":[{"productId":"P002","quantity":1}]}'
```

預期結果：Jaeger 中同一條 Trace 涵蓋 HTTP 同步段 + Kafka 非同步段，共 5 個服務。

### 場景五：Kafka 消費失敗與 DLT（Dead Letter Topic）

模擬 Notification Service 處理失敗，觸發重試與 Dead Letter Topic。

```bash
# 步驟 1：啟用失敗模擬
curl -X POST "http://localhost:8085/api/admin/simulate-failure?enabled=true"

# 步驟 2：觸發下單
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"C001","items":[{"productId":"P003","quantity":1}]}'

# 步驟 3：等待重試完成（約 15 秒）
sleep 15

# 步驟 4：關閉失敗模擬
curl -X POST "http://localhost:8085/api/admin/simulate-failure?enabled=false"
```

預期結果：Jaeger 中 Notification Service 有多個 Consumer Span（原始 + 3 次重試），每個帶有 `error=true`，最後產生 DLT Producer Span。

---

## Jaeger UI 操作指南

開啟 http://localhost:16686 使用 Jaeger 追蹤視覺化介面。

| 操作 | 步驟 |
|------|------|
| 搜尋 Trace | 選擇 Service -> 設定條件（時間範圍、延遲等）-> Find Traces |
| 以 TraceID 搜尋 | 直接在搜尋欄貼上 TraceID |
| 延遲分析 | 點擊任一 Trace -> 查看 Span 時間軸 -> 識別耗時最長的 Span |
| 服務依賴 | 點擊上方的 System Architecture -> 查看 DAG 依賴圖 |
| 錯誤篩選 | 搜尋條件中設定 Tags: `error=true` |

---

## 專案結構（六角形架構）

每個微服務都遵循六角形架構（Hexagonal Architecture / Ports & Adapters），確保業務邏輯與基礎設施解耦。

```
tracing-otel-agent-poc/
|-- docker-compose.yml              # Docker Compose 環境編排
|-- docker-compose.no-agent.yml     # 無 Agent 版本（用於效能對比）
|-- README.md                       # 本文件
|-- PRD.md                          # 產品需求文件
|-- TECH.md                         # 技術規格文件
|-- CLAUDE.md                       # AI Agent 上下文
|-- scripts/                        # 效能基準測試腳本
|   |-- benchmark.sh                # 回應時間基準測試
|   |-- startup-benchmark.sh        # 啟動時間基準測試
|   +-- graceful-degradation-test.sh # Jaeger 不可用時的降級測試
|
|-- order-service/                  # 訂單編排服務 (Port 8081)
|   |-- Dockerfile
|   |-- pom.xml
|   +-- src/main/java/com/ecommerce/order/
|       |-- domain/                 # 領域層：Order, OrderItem, OrderStatus
|       |   |-- model/              #   領域模型與值物件
|       |   |-- event/              #   領域事件 (OrderConfirmedEvent)
|       |   +-- port/               #   領域 Port (OrderRepository)
|       |-- application/            # 應用層：CreateOrderUseCase
|       |   |-- port/in/            #   Inbound Port (CreateOrderPort)
|       |   |-- port/out/           #   Outbound Port (ProductQueryPort, InventoryReservePort, ...)
|       |   |-- service/            #   Use Case 實作
|       |   +-- dto/                #   應用層 DTO
|       +-- infrastructure/         # 基礎設施層
|           |-- adapter/in/rest/    #   REST Controller (OrderController)
|           |-- adapter/out/rest/   #   REST Client Adapter
|           |-- adapter/out/kafka/  #   Kafka Publisher Adapter
|           +-- adapter/out/jpa/    #   JPA Repository Adapter
|
|-- product-service/                # 商品服務 (Port 8082)
|   +-- src/main/java/com/ecommerce/product/
|       |-- domain/model/           # Product
|       |-- application/            # QueryProductUseCase, QueryProductPort
|       +-- infrastructure/         # ProductController, JPA Adapter
|
|-- inventory-service/              # 庫存服務 (Port 8083)
|   +-- src/main/java/com/ecommerce/inventory/
|       |-- domain/model/           # Inventory, InsufficientStockException
|       |-- application/            # ReserveInventoryUseCase, ReleaseInventoryUseCase
|       +-- infrastructure/         # InventoryController, JPA Adapter
|
|-- payment-service/                # 支付服務 (Port 8084)
|   +-- src/main/java/com/ecommerce/payment/
|       |-- domain/model/           # Payment, PaymentStatus
|       |-- application/            # ProcessPaymentUseCase, ProcessPaymentPort
|       +-- infrastructure/         # PaymentController, AdminController, JPA Adapter
|
|-- notification-service/           # 通知服務 (Port 8085)
|   +-- src/main/java/com/ecommerce/notification/
|       |-- domain/model/           # Notification, Customer, NotificationStatus
|       |-- application/            # ProcessOrderNotificationUseCase
|       +-- infrastructure/         # KafkaListener, AdminController, JPA Adapter
|
+-- specs/                          # 規格文件
    +-- 001-otel-distributed-tracing/
        |-- spec.md                 # 功能規格書
        |-- plan.md                 # 實作規劃
        |-- tasks.md                # 任務清單
        |-- data-model.md           # 資料模型
        |-- research.md             # 技術研究
        |-- quickstart.md           # 快速入門
        |-- contracts/              # API 合約 (OpenAPI YAML)
        +-- checklists/             # 品質檢查清單
```

**層級依賴規則（透過 ArchUnit 測試驗證）：**

| 來源層 | 可存取 | 不可存取 |
|--------|--------|----------|
| Infrastructure | Application, Domain | -- |
| Application | Domain | Infrastructure（須透過 Port） |
| Domain | -- | Application, Infrastructure |

---

## 執行測試

各服務可獨立執行單元測試：

```bash
# Order Service
cd order-service && mvn test

# Product Service
cd product-service && mvn test

# Inventory Service
cd inventory-service && mvn test

# Payment Service
cd payment-service && mvn test

# Notification Service
cd notification-service && mvn test
```

測試涵蓋：
- 領域模型單元測試（Domain Model Unit Tests）
- 應用層 Use Case 測試
- 六角形架構合規測試（ArchUnit）

---

## 效能基準測試腳本

`scripts/` 目錄下提供三支效能測試腳本：

### 回應時間基準測試

測量 100 次請求的平均回應時間（排除前 10 次 warmup）。

```bash
# 有 Agent（預設 docker-compose.yml）
docker-compose up -d
./scripts/benchmark.sh

# 無 Agent（對比用）
docker-compose -f docker-compose.no-agent.yml up -d
./scripts/benchmark.sh
```

### 啟動時間基準測試

測量各服務從容器啟動到 health check 通過的時間。

```bash
docker-compose up -d
./scripts/startup-benchmark.sh
```

### Graceful Degradation 測試

驗證 Jaeger 不可用時，業務服務是否正常運作。

```bash
docker-compose up -d
./scripts/graceful-degradation-test.sh
```

**效能目標：**

| 指標 | 目標 |
|------|------|
| 平均回應時間 overhead | < 5% |
| 啟動時間增加 | < 10 秒 |

---

## 環境清理

```bash
# 停止所有容器並移除 volumes
docker-compose down -v
```

---

## 常見問題（FAQ）

**Q: 為什麼使用 JDK 8 而不是更新的版本？**
A: 本 PoC 模擬企業既有環境的限制。OTel Java Agent 1.32.1 是最後一個支援 JDK 8 的版本。

**Q: 為什麼 Kafka 使用 KRaft 模式？**
A: KRaft 模式不需要額外部署 ZooKeeper，簡化 Docker Compose 配置。

**Q: 如何完全關閉追蹤？**
A: 在 `docker-compose.yml` 中移除各服務的 `JAVA_TOOL_OPTIONS` 環境變數即可。不需要修改任何程式碼。

**Q: Jaeger 不可用時會影響業務服務嗎？**
A: 不會。Agent 内建 graceful degradation 機制，當追蹤後端不可用時，業務服務正常運作不受影響。

---

## 參考資料

- [OpenTelemetry Java Agent 文件](https://opentelemetry.io/docs/instrumentation/java/automatic/)
- [OpenTelemetry Java Agent 1.32.1 Release](https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/tag/v1.32.1)
- [Jaeger 官方文件](https://www.jaegertracing.io/docs/)
- [Spring Boot 2.7.18 文件](https://docs.spring.io/spring-boot/docs/2.7.18/reference/htmlsingle/)
- [Spring Kafka 文件](https://docs.spring.io/spring-kafka/reference/)
- [W3C Trace Context 規範](https://www.w3.org/TR/trace-context/)
- [Apache Kafka KRaft 模式](https://kafka.apache.org/documentation/#kraft)
