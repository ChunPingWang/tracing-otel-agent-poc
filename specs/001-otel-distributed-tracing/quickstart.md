# Quickstart: E-Commerce Distributed Tracing PoC

## 前置需求

- Docker Desktop (with Docker Compose)
- JDK 8 (OpenJDK)
- Maven 3.6+
- curl (測試用)

## 一鍵啟動

```bash
# 1. 建置所有微服務
cd order-service && mvn clean package -DskipTests && cd ..
cd product-service && mvn clean package -DskipTests && cd ..
cd inventory-service && mvn clean package -DskipTests && cd ..
cd payment-service && mvn clean package -DskipTests && cd ..
cd notification-service && mvn clean package -DskipTests && cd ..

# 2. 啟動 Docker Compose 環境
docker-compose up --build -d

# 3. 等待所有服務就緒（約 30-60 秒）
echo "等待服務啟動..."
sleep 30

# 4. 驗證服務狀態
curl -s http://localhost:8081/actuator/health | jq .
curl -s http://localhost:8082/actuator/health | jq .
curl -s http://localhost:8083/actuator/health | jq .
curl -s http://localhost:8084/actuator/health | jq .
curl -s http://localhost:8085/actuator/health | jq .
```

## 場景驗證

### 場景一：正常下單（Happy Path）

```bash
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"C001","items":[{"productId":"P001","quantity":2}]}'
```

**預期回應**:
```json
{
  "orderId": "ORD-...",
  "status": "CONFIRMED",
  "totalAmount": 1990.00,
  "traceId": "..."
}
```

**Jaeger 驗證**: 開啟 http://localhost:16686 → 搜尋 Service: order-service → 找到包含 4+ 服務的 Trace

### 場景二：庫存不足

```bash
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"C001","items":[{"productId":"P001","quantity":999}]}'
```

**預期回應**: 訂單狀態 FAILED
**Jaeger 驗證**: inventory-service Span 帶有 error=true

### 場景三：支付超時

```bash
# 啟用延遲模擬（5 秒）
curl -X POST "http://localhost:8084/api/admin/simulate-delay?ms=5000"

# 觸發下單
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"C001","items":[{"productId":"P001","quantity":1}]}'

# 關閉延遲模擬
curl -X POST "http://localhost:8084/api/admin/simulate-delay?ms=0"
```

**預期回應**: 訂單狀態 PAYMENT_TIMEOUT
**Jaeger 驗證**: payment-service Span duration > 3s

### 場景四：Kafka 非同步通知

```bash
# 與場景一相同（正常下單自動觸發 Kafka 事件）
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"C001","items":[{"productId":"P002","quantity":1}]}'
```

**Jaeger 驗證**: 同一條 Trace 涵蓋 HTTP 同步段 + Kafka 非同步段，
notification-service 內含 2 個 JDBC Span

### 場景五：Kafka 消費失敗與 DLT

```bash
# 啟用失敗模擬
curl -X POST "http://localhost:8085/api/admin/simulate-failure?enabled=true"

# 觸發下單
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"C001","items":[{"productId":"P003","quantity":1}]}'

# 等待重試完成（約 15 秒）
sleep 15

# 關閉失敗模擬
curl -X POST "http://localhost:8085/api/admin/simulate-failure?enabled=false"
```

**Jaeger 驗證**: notification-service 有多個 Consumer Span（含 error=true），
最後一個 Span 為 DLT Producer

## 效能基準測試

```bash
# 無 Agent 測試（需移除 docker-compose.yml 中的 JAVA_TOOL_OPTIONS）
# 執行 100 次請求（排除前 10 次 warmup）

# 有 Agent 測試（預設 docker-compose.yml 設定）
for i in $(seq 1 110); do
  curl -s -o /dev/null -w "%{time_total}\n" \
    -X POST http://localhost:8081/api/orders \
    -H "Content-Type: application/json" \
    -d '{"customerId":"C001","items":[{"productId":"P001","quantity":1}]}'
done | tail -n 100 | awk '{sum+=$1} END {print "Average:", sum/NR, "seconds"}'
```

## 環境清理

```bash
docker-compose down -v
```

## Jaeger UI

- URL: http://localhost:16686
- 搜尋 Trace: 選擇 Service → 設定條件 → Find Traces
- 延遲分析: 點擊 Trace → 查看 Span timeline
- 服務依賴: System Architecture → DAG 圖
