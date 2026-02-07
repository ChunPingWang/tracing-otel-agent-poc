# Feature Specification: E-Commerce Distributed Tracing PoC (OTel Agent)

**Feature Branch**: `001-otel-distributed-tracing`
**Created**: 2026-02-07
**Status**: Draft
**Input**: PRD — 電子商務分散式追蹤 PoC，使用 OpenTelemetry Java Agent 零侵入方式導入分散式追蹤

## User Scenarios & Testing *(mandatory)*

### User Story 1 - 訂單流程端到端追蹤 (Priority: P1)

作為 SRE 工程師，我希望在追蹤系統中看到完整的客戶下單呼叫鏈，包含
訂單建立、商品查詢、庫存預扣、支付處理等所有步驟，以便快速定位效能瓶頸
與故障服務。

**Why this priority**: 這是整個 PoC 的核心驗證目標。若無法實現同步 HTTP
呼叫鏈的端到端追蹤，後續所有場景（異常路徑、非同步追蹤）均無意義。

**Independent Test**: 發送一筆下單請求，在追蹤 UI 中以 TraceID 搜尋，
應看到包含 4 個服務（Order、Product、Inventory、Payment）的完整呼叫鏈圖，
每個服務間的呼叫為獨立的 Span，且包含 HTTP method、URL、status code 等屬性。

**Acceptance Scenarios**:

1. **Given** 5 個微服務均已啟動且掛載追蹤 Agent，**When** 客戶發送下單請求
   且商品有庫存、支付成功，**Then** 追蹤 UI 顯示包含 Order → Product →
   Inventory → Payment 的完整 Trace，訂單狀態為 CONFIRMED
2. **Given** 追蹤 UI 已開啟，**When** SRE 輸入上述訂單的 TraceID 進行搜尋，
   **Then** 顯示包含至少 4 個服務的 Trace 圖，每個 HTTP Span 包含
   http.method、http.url、http.status_code 屬性
3. **Given** 追蹤 UI 已開啟，**When** SRE 以服務名稱「order-service」加上
   延遲條件（duration > 1s）進行篩選，**Then** 系統回傳符合條件的 Trace 清單

---

### User Story 2 - 異常路徑追蹤（庫存不足與支付超時）(Priority: P2)

作為開發人員，我希望在呼叫鏈路中看到異常狀態的標記，包含庫存不足的錯誤
標記以及支付超時的長延遲 Span，以便快速診斷問題根因。

**Why this priority**: 異常路徑是生產環境最常需要排查的場景。在 Happy Path
驗證成功後，驗證異常路徑的可觀測性是 PoC 的第二重要目標。

**Independent Test**: 分別觸發庫存不足與支付超時兩種場景，在追蹤 UI 中確認
錯誤標記與長延遲 Span 是否正確呈現。

**Acceptance Scenarios**:

1. **Given** 商品庫存為 0，**When** 客戶發送下單請求，**Then** Trace 中
   Inventory Service 的 Span 帶有 error 標記，Payment Service 呼叫不應出現，
   訂單狀態為 FAILED
2. **Given** Payment Service 模擬高延遲（> 3 秒）且 Order Service 的 HTTP
   timeout 設為 3 秒，**When** 客戶發送下單請求，**Then** Trace 中 Payment
   Service 的 Span duration 明顯偏長，Order Service 後續呼叫 Inventory Service
   執行庫存回滾，訂單狀態為 PAYMENT_TIMEOUT
3. **Given** 追蹤 UI 已開啟，**When** SRE 依延遲排序搜尋 Trace，**Then**
   支付超時的 Trace 排在前列且可快速識別

---

### User Story 3 - Kafka 非同步鏈路追蹤 (Priority: P3)

作為 SRE 工程師，我希望在同一條 Trace 中同時看到 HTTP 同步呼叫與 Kafka
非同步事件的完整鏈路，以便端到端追蹤跨越同步與非同步邊界的業務流程。

**Why this priority**: 驗證 Kafka Context Propagation 是本 PoC 的關鍵
差異化目標。同步追蹤已是業界成熟方案，非同步追蹤的可行性驗證才是本
PoC 的核心價值。

**Independent Test**: 完成一筆成功下單，確認追蹤 UI 中同一條 Trace 涵蓋
HTTP 同步段與 Kafka 非同步段（Produce → Consume），且 Notification Service
的 DB 操作產生獨立的 JDBC Span。

**Acceptance Scenarios**:

1. **Given** 訂單已成功確認且 Kafka 正常運作，**When** Order Service 發送
   「訂單已確認」事件至 Kafka，**Then** 同一條 Trace 中包含 Kafka Producer
   Span 與 Consumer Span，形成 parent-child 關係
2. **Given** Notification Service 消費事件成功，**When** 查看該 Trace，
   **Then** 看到 Notification Service 的 DB 操作（查詢客戶資訊 + 寫入通知
   結果）各有獨立的 JDBC Span，包含 db.statement 與 db.system 屬性
3. **Given** 追蹤 UI 已開啟，**When** 以 TraceID 搜尋完整下單流程，**Then**
   整條 Trace 涵蓋 5 個服務：Order → Product → Inventory → Payment →
   (Kafka) → Notification

---

### User Story 4 - Kafka 消費失敗與重試追蹤 (Priority: P4)

作為 SRE 工程師，我希望追蹤 Kafka 消費失敗的完整重試過程，包含每次重試
的 Span 與最終 Dead Letter Topic 的投遞，以便診斷非同步處理的故障。

**Why this priority**: 消費失敗與重試是非同步架構中常見的故障模式，
驗證其可追蹤性是 Kafka 追蹤的進階需求。

**Independent Test**: 模擬 Notification Service 處理失敗，確認追蹤 UI 中
顯示重試 Span 與 DLT 投遞 Span。

**Acceptance Scenarios**:

1. **Given** Notification Service 模擬處理失敗（通知閘道不可用），**When**
   消費 order-confirmed 事件，**Then** Trace 中 Consumer Span 帶有 error=true
   標記
2. **Given** 重試策略為最多 3 次，**When** 3 次重試均失敗，**Then** 每次重試
   產生新的 Consumer Span，均關聯到同一個 Trace
3. **Given** 3 次重試均失敗，**When** 訊息送至 Dead Letter Topic
   `order-confirmed.DLT`，**Then** DLT 投遞產生獨立的 Producer Span

---

### User Story 5 - 零侵入驗證與效能影響評估 (Priority: P5)

作為架構師，我希望驗證追蹤 Agent 的導入完全不需修改業務程式碼，並量化
Agent 對應用效能的影響，以便評估正式環境導入的可行性。

**Why this priority**: 零侵入與效能影響是正式導入決策的關鍵依據，但需要
在前述功能驗證完成後才能進行有意義的評估。

**Independent Test**: 對比有 Agent 與無 Agent 的服務回應時間與啟動時間，
確認業務程式碼無任何追蹤相關修改。

**Acceptance Scenarios**:

1. **Given** 服務已掛載追蹤 Agent，**When** 檢視所有微服務的業務程式碼，
   **Then** 無任何追蹤相關的 import、annotation 或 API 呼叫
2. **Given** 相同的測試請求，**When** 分別在有 Agent 與無 Agent 環境下各執行
   100 次請求（排除前 10 次 warmup），**Then** 有 Agent 環境的平均回應時間
   overhead 低於 5%
3. **Given** 服務需要啟動，**When** 掛載 Agent 後啟動，**Then** 啟動時間
   增加不超過 10 秒
4. **Given** 追蹤後端服務不可用，**When** 業務請求持續進入，**Then** 業務
   服務正常運作不受影響（graceful degradation）
5. **Given** 維運人員希望關閉追蹤，**When** 移除 -javaagent 啟動參數，
   **Then** 追蹤完全關閉，無需修改任何程式碼

---

### Edge Cases

- 追蹤後端（收集器）不可用時，Agent MUST graceful degradation，不影響業務服務正常運作
- 同一筆訂單的同步段（HTTP）與非同步段（Kafka）跨越不同時間窗口時，仍 MUST 關聯在同一條 Trace 中
- Kafka Consumer 發生 rebalance 時，重試的 Span MUST 仍能正確關聯到原始 Trace
- 多筆並發訂單同時進入時，每筆訂單 MUST 各自產生獨立的 Trace，不可混淆
- 服務啟動順序不同時，Agent MUST 在服務就緒後自動開始產生 Trace 資料

## Clarifications

### Session 2026-02-07

- Q: Order Service 呼叫 Payment Service 的 HTTP timeout 閾值為何？ → A: 3 秒（與模擬延遲一致，清晰分界）
- Q: Kafka 訂單確認事件的 Payload 包含哪些欄位？ → A: 完整資料（orderId, customerId, customerEmail, items, totalAmount, status）
- Q: 效能基準測試的請求量與測量方式為何？ → A: 100 次請求取平均回應時間比較，排除前 10 次 warmup

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: 系統 MUST 提供 5 個微服務（Order、Product、Inventory、Payment、Notification），每個服務為獨立部署的應用程式
- **FR-002**: Order Service MUST 協調完整的下單流程：建立訂單 → 查詢商品 → 預扣庫存 → 發起支付 → 確認訂單
- **FR-003**: 所有服務間的同步呼叫 MUST 自動產生 Trace 與 Span，包含 HTTP method、URL、status code 屬性
- **FR-004**: 所有服務的資料庫操作 MUST 自動產生 JDBC Span，包含 SQL 語句（db.statement）、資料庫系統（db.system）與執行時間
- **FR-005**: Order Service MUST 在訂單確認後，透過 Kafka 發送「訂單已確認」事件至 `order-confirmed` Topic，Payload MUST 包含 orderId、customerId、customerEmail、items、totalAmount、status 欄位
- **FR-006**: Notification Service MUST 消費 `order-confirmed` 事件，查詢客戶資訊並發送通知（模擬）
- **FR-007**: Kafka Producer 與 Consumer 之間的呼叫鏈 MUST 自動串聯在同一條 Trace 中，包含 Topic 名稱與 Partition 資訊
- **FR-008**: Notification Service 消費失敗時 MUST 自動重試（最多 3 次），重試均失敗後 MUST 將訊息送至 Dead Letter Topic `order-confirmed.DLT`
- **FR-009**: 每次重試的 Consumer Span MUST 關聯到原始 Trace，且帶有 error 標記
- **FR-010**: 庫存不足時，Order Service MUST 將訂單狀態設為 FAILED，Trace 中 Inventory Service 的 Span MUST 帶有 error 標記
- **FR-011**: Order Service 呼叫 Payment Service 的 HTTP timeout MUST 設為 3 秒；超時時 Order Service MUST 呼叫 Inventory Service 執行庫存回滾，訂單狀態設為 PAYMENT_TIMEOUT
- **FR-012**: 追蹤功能 MUST 完全透過啟動參數（-javaagent）控制，業務程式碼零修改
- **FR-013**: 追蹤後端不可用時，業務服務 MUST 正常運作不受影響
- **FR-014**: 所有追蹤設定 MUST 透過環境變數或 JVM 參數外部化控制
- **FR-015**: 系統 MUST 提供可重現的容器化部署環境，包含所有微服務與基礎設施

### Key Entities

- **Order（訂單）**: 包含訂單編號、客戶資訊、商品清單、訂單狀態（CREATED / CONFIRMED / FAILED / PAYMENT_TIMEOUT）、建立時間
- **Product（商品）**: 包含商品編號、名稱、價格、可用性狀態
- **Inventory（庫存）**: 包含商品編號、可用數量、預扣紀錄
- **Payment（支付）**: 包含支付編號、訂單編號、金額、支付狀態
- **Notification（通知）**: 包含通知編號、訂單編號、客戶聯絡資訊、通知狀態、發送時間
- **Trace（追蹤鏈路）**: 包含 TraceID、涵蓋的服務清單、總延遲時間、Span 數量
- **Span（追蹤單元）**: 包含 SpanID、父 SpanID、服務名稱、操作名稱、開始時間、持續時間、屬性標籤、錯誤標記

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: SRE 工程師在追蹤 UI 中輸入 TraceID，3 秒內即可看到包含所有參與服務的完整呼叫鏈圖
- **SC-002**: 五個業務場景（正常下單、庫存不足、支付超時、Kafka 非同步通知、Kafka 消費失敗重試）的 Trace 均可在追蹤 UI 中完整呈現
- **SC-003**: HTTP 同步呼叫與 Kafka 非同步事件成功串聯在同一條 Trace 中
- **SC-004**: 所有服務的資料庫操作自動產生追蹤資料，包含查詢語句與執行時間
- **SC-005**: 以 100 次請求（排除前 10 次 warmup）取平均，導入追蹤後服務回應時間 overhead 低於 5%
- **SC-006**: 導入追蹤後，服務啟動時間增加不超過 10 秒
- **SC-007**: 所有微服務的業務程式碼零修改即可產生追蹤資料
- **SC-008**: 追蹤後端不可用時，業務服務 100% 正常運作
- **SC-009**: 提供可一鍵啟動的容器化環境，任何人均可在 5 分鐘內完成環境建置並重現所有場景
- **SC-010**: 產出正式導入的評估報告，包含效能數據與升級路徑建議
