# Research: E-Commerce Distributed Tracing PoC (OTel Agent)

**Date**: 2026-02-07
**Branch**: `001-otel-distributed-tracing`

## R1: OpenTelemetry Java Agent 版本選擇

**Decision**: OpenTelemetry Java Agent 1.32.1

**Rationale**: 1.32.1 是最後支援 JDK 8 的版本。2.x 版本要求 JDK 17+，
與既有 Spring Boot 2.7.x / JDK 8 環境不相容。

**Alternatives considered**:
- OTel Agent 2.x — 不支援 JDK 8，排除
- Zipkin Brave — 需侵入程式碼（加入依賴與設定），違反零侵入需求
- Jaeger Client (deprecated) — 已停止維護，不推薦新專案使用
- Micrometer Tracing — Spring Boot 3.x 內建，不適用於 2.7.x

## R2: Tracing Backend 選擇

**Decision**: Jaeger All-in-One (latest)

**Rationale**: Jaeger 原生支援 OTLP gRPC 協定，提供功能完整的 UI
（Trace 搜尋、服務依賴圖、延遲分析），且 all-in-one 模式適合 PoC
（記憶體儲存，單一 Container）。

**Alternatives considered**:
- Zipkin — UI 功能較弱，缺乏服務依賴圖
- Grafana Tempo + Grafana — 功能強大但部署複雜度高，PoC 階段過重
- SigNoz — 功能完整但需多個 Container，PoC 階段過重
- AWS X-Ray / GCP Cloud Trace — 雲端廠商鎖定，不適合 PoC

## R3: Kafka 部署模式

**Decision**: Apache Kafka 3.6.2 (KRaft mode, single node)

**Rationale**: KRaft 模式移除 ZooKeeper 依賴，僅需單一 Container 即可
運行 Kafka broker + controller。大幅簡化 PoC 環境的部署複雜度。

**Alternatives considered**:
- Kafka + ZooKeeper — 需額外 Container，增加部署複雜度
- Redpanda — Kafka 相容但 OTel Agent 的攔截行為未經完整驗證
- RabbitMQ — 不符合 PRD 指定的 Kafka 需求

## R4: 六角形架構在 PoC 中的適用性

**Decision**: 所有微服務採用完整六角形架構（domain / application / infrastructure）

**Rationale**: 憲法（constitution）明確要求所有服務 MUST 採用六角形架構，
此為不可協商原則。雖然 PoC 可用較簡單的分層方式，但遵守憲法可確保：
1. 驗證六角形架構在 OTel Agent 零侵入場景下的可行性
2. Domain layer 無框架依賴，Agent 僅在 infrastructure layer 攔截
3. 為後續正式導入建立架構範本

**Alternatives considered**:
- 傳統三層架構（Controller-Service-Repository）— 較簡單但違反憲法
- 僅 Order Service 採用六角形，其他簡化 — 不一致，違反憲法

## R5: 測試策略

**Decision**: JUnit 5 + Spring Boot Test + spring-kafka-test + ArchUnit

**Rationale**:
- JUnit 5: Spring Boot 2.7.x 預設測試框架
- Spring Boot Test: 提供 @SpringBootTest、MockMvc、@DataJpaTest
- spring-kafka-test: 提供 EmbeddedKafka 進行 Kafka 整合測試
- ArchUnit: 自動化驗證六角形架構分層約束

**Alternatives considered**:
- TestContainers — 功能更強但需 Docker-in-Docker，PoC 環境增加複雜度
- Mockito alone — 不足以覆蓋整合測試需求
- Cucumber (BDD) — 驗收測試框架，但 PoC 規模下 Given-When-Then 以
  JUnit parameterized tests 即可覆蓋

## R6: HTTP Client 選擇

**Decision**: RestTemplate (Spring Boot 內建)

**Rationale**: Spring Boot 2.7.x 內建 RestTemplate，OTel Agent 1.32.1
自動攔截 RestTemplate 的 outbound 呼叫並注入 W3C Trace Context header。
零設定即可實現跨服務 Context Propagation。

**Alternatives considered**:
- WebClient (WebFlux) — 需引入 spring-boot-starter-webflux，增加依賴；
  且 WebFlux 的 reactive 模型增加 PoC 複雜度
- Apache HttpClient — OTel Agent 同樣支援攔截，但需額外設定
- OpenFeign — 需引入額外依賴，增加複雜度

## R7: Kafka Event Payload 設計

**Decision**: 完整資料模式（orderId, customerId, customerEmail, items,
totalAmount, status）

**Rationale**: 規格書釐清階段決定採用完整資料 Payload，減少
Notification Service 對其他服務的查詢依賴。Notification Service 仍需
查詢 DB 取得客戶詳細資訊（如地址、偏好通知管道），以產生 JDBC Span。

**Alternatives considered**:
- 最小 Payload（僅 orderId + customerId）— 需更多 DB 查詢回補
- 中等 Payload（不含 items）— 通知內容可能不完整

## R8: Payment Timeout 處理策略

**Decision**: RestTemplate 配置 3 秒 connectTimeout + readTimeout，
超時拋出 ResourceAccessException，Order Service 捕獲後執行庫存回滾。

**Rationale**: 規格書釐清階段決定 timeout = 3 秒。Payment Service 模擬
延遲 > 3 秒時，RestTemplate 將拋出超時例外。Order Service 在 application
layer 捕獲例外並透過 InventoryReservePort（release）執行庫存回滾。

**Alternatives considered**:
- Circuit Breaker (Resilience4j) — PoC 階段過複雜，Out of Scope
- Async payment with callback — 變更架構模式，不適合 PoC
