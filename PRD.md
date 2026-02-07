# PRD：電子商務分散式追蹤 PoC — OpenTelemetry Java Agent 方案

## 文件資訊

| 項目 | 內容 |
|------|------|
| 專案名稱 | E-Commerce Distributed Tracing PoC (OTel Agent) |
| 版本 | v1.1 |
| 日期 | 2026-02-07 |
| 作者 | Enterprise Architecture Team |

---

## 1. 背景與動機

企業電子商務平台採用微服務架構，包含訂單服務、商品服務、庫存服務、支付服務等多個獨立部署的 Spring Boot 2 應用。隨著系統規模成長，跨服務呼叫鏈路的可觀測性（Observability）成為營運痛點：

- 客戶下單後遇到延遲，客服無法快速定位是哪個服務造成瓶頸
- 促銷活動期間系統效能下降，缺乏端到端的延遲分析能力
- 服務間的依賴關係不透明，架構治理缺乏數據支撐

此外，平台部分流程已採用 Kafka 進行非同步事件處理（如訂單確認後的通知與物流排程），非同步鏈路的可觀測性同樣是關鍵痛點。

本 PoC 驗證以 **OpenTelemetry Java Agent（零侵入）** 方式為既有 Spring Boot 2 / JDK 8 應用導入分散式追蹤的可行性，涵蓋同步 HTTP、非同步 Kafka 以及 DB 存取等多種通訊模式。

---

## 2. 業務目標

| 編號 | 目標 | 成功指標 |
|------|------|----------|
| BG-1 | 實現訂單流程端到端可視化 | 在 Jaeger UI 可看到完整的下單→查商品→扣庫存→支付 呼叫鏈 |
| BG-2 | 支援效能瓶頸快速定位 | 透過 Trace 可識別出延遲超過 500ms 的服務節點 |
| BG-3 | 零程式碼修改導入追蹤 | 既有服務程式碼無需任何變更即可產生 Trace 資料 |
| BG-4 | 評估生產環境部署可行性 | 量化 Agent 對應用啟動時間與 Runtime 效能的影響 |
| BG-5 | 驗證非同步 Kafka 鏈路追蹤 | Kafka Producer → Consumer 的呼叫鏈在同一條 Trace 中完整呈現 |
| BG-6 | 驗證 DB 存取自動追蹤 | 所有 JDBC 操作自動產生 Span，包含 SQL 語句與執行時間 |

---

## 3. 業務場景

### 3.1 場景一：客戶下單（Happy Path）

**前置條件：** 客戶已登入、購物車有商品

**主要流程：**

1. 客戶在前端點擊「結帳」
2. **Order Service** 接收下單請求，建立訂單（狀態：CREATED）
3. Order Service 呼叫 **Product Service** 查詢商品價格與可用性
4. Order Service 呼叫 **Inventory Service** 執行庫存預扣
5. Order Service 呼叫 **Payment Service** 發起扣款
6. 扣款成功後，訂單狀態更新為 CONFIRMED
7. 回傳訂單確認結果給客戶

**追蹤期望：** 上述步驟 2-7 應在同一條 Trace 中完整呈現，每個服務間的呼叫為獨立 Span。

### 3.2 場景二：庫存不足（異常路徑）

**前置條件：** 客戶下單的商品庫存為 0

**主要流程：**

1. 客戶點擊「結帳」
2. Order Service 接收請求
3. Order Service 呼叫 Product Service 查詢 → 正常回應
4. Order Service 呼叫 Inventory Service 預扣庫存 → **回傳庫存不足錯誤**
5. Order Service 將訂單狀態設為 FAILED，回傳錯誤訊息

**追蹤期望：** Trace 中應可看到 Inventory Service 的 Span 帶有錯誤標記（Error Tag），且後續的 Payment Service 呼叫不應出現。

### 3.3 場景三：支付超時（效能異常）

**前置條件：** Payment Service 模擬高延遲（> 3 秒）

**主要流程：**

1. 正常流程走到步驟 5（呼叫 Payment Service）
2. Payment Service 處理時間超過設定的 timeout（Order Service HTTP timeout 為 3 秒）
3. Order Service 接收到 Timeout 例外
4. Order Service 呼叫 Inventory Service 執行庫存回滾
5. 訂單狀態設為 PAYMENT_TIMEOUT

**追蹤期望：** Trace 中 Payment Service 的 Span duration 應明顯偏長，且可在 Jaeger 中透過延遲排序快速發現。

### 3.4 場景四：訂單確認後非同步通知（Kafka Happy Path）

**前置條件：** 訂單已成功確認，Kafka 叢集正常運作

**主要流程：**

1. Order Service 完成下單流程（同場景一步驟 1-6）
2. Order Service 將「訂單已確認」事件發送至 Kafka Topic `order-confirmed`（Payload 包含 orderId、customerId、customerEmail、items、totalAmount、status）
3. **Notification Service** 消費 `order-confirmed` 事件
4. Notification Service 查詢 DB 取得客戶聯絡資訊
5. Notification Service 發送訂單確認通知（模擬）
6. Notification Service 將通知結果寫入 DB

**追蹤期望：**

- 同步段（HTTP 呼叫鏈）與非同步段（Kafka Produce → Consume）應在同一條 Trace 中
- Kafka Producer Span 與 Consumer Span 形成 parent-child 關係
- Notification Service 的 DB 操作（查詢 + 寫入）各有獨立的 JDBC Span
- 整條 Trace 涵蓋 5 個服務：Order → Product → Inventory → Payment → (Kafka) → Notification

### 3.5 場景五：Kafka 消費失敗與重試（非同步異常路徑）

**前置條件：** Notification Service 模擬處理失敗（如通知閘道不可用）

**主要流程：**

1. Order Service 發送「訂單已確認」事件至 Kafka
2. Notification Service 消費事件
3. Notification Service 處理失敗，拋出例外
4. Spring Kafka 根據重試策略自動重試（最多 3 次）
5. 3 次重試均失敗後，訊息送至 Dead Letter Topic `order-confirmed.DLT`

**追蹤期望：**

- 原始 Trace 中 Notification Service 的 Consumer Span 帶有 `error=true`
- 每次重試產生新的 Consumer Span，均關聯到同一個 Trace
- DLT 投遞產生獨立的 Producer Span

---

## 4. 使用者故事

| 編號 | 角色 | 故事 | 驗收條件 |
|------|------|------|----------|
| US-1 | SRE 工程師 | 作為 SRE，我希望在 Jaeger UI 搜尋特定 TraceID，看到完整的跨服務呼叫鏈 | 輸入 TraceID 後，顯示包含 4 個服務的完整 Trace 圖 |
| US-2 | SRE 工程師 | 作為 SRE，我希望依服務名稱與延遲篩選 Trace | 可篩選 order-service 且 duration > 1s 的 Trace |
| US-3 | 開發人員 | 作為開發人員，我希望在 Span 中看到 HTTP method、URL、status code | 每個 HTTP Span 包含 http.method, http.url, http.status_code 屬性 |
| US-4 | 開發人員 | 作為開發人員，我希望看到 DB 查詢的 Span | JDBC 呼叫自動產生 Span，包含 db.statement 屬性 |
| US-5 | 架構師 | 作為架構師，我希望了解 Agent 對效能的影響 | 以 100 次請求（排除前 10 次 warmup）取平均，有 Agent vs 無 Agent 的回應時間比較報告，overhead < 5% |
| US-6 | 維運人員 | 作為維運人員，我希望透過啟動參數即可開關追蹤 | 移除 -javaagent 參數即可完全關閉追蹤，無需改程式碼 |
| US-7 | SRE 工程師 | 作為 SRE，我希望在同一條 Trace 中看到 HTTP 同步呼叫與 Kafka 非同步事件 | 下單 Trace 包含 Kafka produce/consume Span，與 HTTP Span 串聯 |
| US-8 | 開發人員 | 作為開發人員，我希望在 Kafka Consumer Span 中看到 Topic 名稱與 Partition 資訊 | Consumer Span 包含 messaging.destination, messaging.kafka.partition 屬性 |
| US-9 | SRE 工程師 | 作為 SRE，我希望追蹤 Kafka 消費失敗與重試的完整過程 | 重試的每次 Consumer Span 均可在 Trace 中查看，含 error 標記 |
| US-10 | 開發人員 | 作為開發人員，我希望在 Trace 中看到每個服務的 SQL 查詢細節 | JDBC Span 包含 db.statement（SQL）、db.system（H2）與執行時間 |

---

## 5. 非功能需求

| 類別 | 需求 | 目標值 |
|------|------|--------|
| 效能 | Agent 引入的延遲 overhead | < 5%（以 100 次請求取平均，排除前 10 次 warmup） |
| 效能 | Agent 對啟動時間的影響 | < 10 秒增加 |
| 可靠性 | Jaeger 不可用時不影響業務服務 | Agent graceful degradation |
| 相容性 | 支援 JDK 版本 | JDK 8 |
| 相容性 | 支援 Spring Boot 版本 | Spring Boot 2.7.x |
| 可維護性 | 追蹤設定外部化 | 所有設定透過環境變數或 JVM 參數控制 |

---

## 6. 範圍界定

### In Scope

- 5 個微服務的 HTTP 呼叫鏈追蹤（含新增 Notification Service）
- Kafka Producer / Consumer 非同步鏈路追蹤
- Kafka 消費失敗重試與 Dead Letter Topic 追蹤
- JDBC 自動追蹤（所有服務的 DB 操作）
- Jaeger UI 查詢與視覺化
- 效能影響基準測試

### Out of Scope

- Metrics 與 Logs 的整合（僅 Traces）
- 告警規則設定
- 正式環境部署規劃
- 前端（Browser）追蹤
- Kafka Streams 追蹤

---

## 7. 風險與假設

| 類型 | 描述 | 緩解措施 |
|------|------|----------|
| 風險 | OTel Agent 1.x 已停止主要功能更新 | 評估後續升級 JDK 17 的路徑 |
| 風險 | Agent 可能與既有的 APM 工具衝突 | PoC 環境獨立部署驗證 |
| 風險 | Kafka Context Propagation 在高吞吐場景下可能增加 Header 大小 | PoC 量測 Kafka Header overhead |
| 假設 | 服務間通訊包含同步 HTTP/REST 與非同步 Kafka 兩種模式 | — |
| 假設 | 開發團隊可接受 Docker Compose 作為 PoC 執行環境 | — |
| 假設 | Kafka 使用 Spring Kafka（非原生 Kafka Client） | — |

---

## 8. 成功標準

PoC 在以下條件全部滿足時視為成功：

1. 五個業務場景（含 Kafka 非同步）的 Trace 均可在 Jaeger UI 完整呈現
2. HTTP 同步呼叫與 Kafka 非同步事件串聯在同一條 Trace 中
3. 所有服務的 JDBC 操作自動產生 Span，包含 SQL 語句
4. 無需修改任何業務程式碼（Kafka 與 DB 追蹤同樣零侵入）
5. Agent overhead 量測結果 < 5%（以 100 次請求取平均，排除前 10 次 warmup）
6. 產出可重現的 Docker Compose 環境與操作文件
7. 產出正式導入的建議報告（含升級路徑）
